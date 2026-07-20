package com.perceptiveus.reverie.playback.audiofx

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PCM equalizer + optional soft loudness leveling for ExoPlayer's audio chain.
 *
 * Designed to be **allocation-light** and **fail-open**: any processing error
 * falls back to a straight buffer copy so playback never enters STATE_ERROR.
 *
 * Do not share one instance across multiple [androidx.media3.exoplayer.audio.AudioSink]s.
 */
@UnstableApi
class EqualizerAudioProcessor : BaseAudioProcessor() {

    private val settingsRef = AtomicReference(AudioFxSettings.Default)

    @Volatile
    private var sampleRateHz: Int = 44_100

    @Volatile
    private var channelCount: Int = 2

    @Volatile
    private var encoding: Int = C.ENCODING_PCM_16BIT

    /** Swapped atomically so the audio thread never sees a half-built filter set. */
    @Volatile
    private var bank: FilterBank = FilterBank.identity(MAX_CHANNELS)

    // Soft loudness AGC state (audio thread only after flush)
    private var loudnessEnvelope: Float = 0.05f
    private var loudnessGain: Float = 1f

    /** Scratch for one interleaved frame — reused to avoid per-callback allocations. */
    private val frameScratch = FloatArray(MAX_CHANNELS)

    fun applySettings(settings: AudioFxSettings) {
        settingsRef.set(settings)
        bank = FilterBank.build(settings, sampleRateHz, channelCount)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            // Skip this processor for unsupported PCM encodings instead of failing the sink.
            return AudioProcessor.AudioFormat.NOT_SET
        }
        val channels = inputAudioFormat.channelCount
        if (channels <= 0 || channels > MAX_CHANNELS) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRateHz = inputAudioFormat.sampleRate.coerceAtLeast(8_000)
        channelCount = channels
        encoding = inputAudioFormat.encoding
        bank = FilterBank.build(settingsRef.get(), sampleRateHz, channelCount)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val startPos = inputBuffer.position()
        try {
            val settings = settingsRef.get()
            if (!settings.eqEnabled && !settings.loudnessEnabled) {
                passthrough(inputBuffer)
                return
            }
            val remaining = inputBuffer.remaining()
            val out = replaceOutputBuffer(remaining)
            val view = inputBuffer.order(ByteOrder.nativeOrder())
            when (encoding) {
                C.ENCODING_PCM_FLOAT -> processFloat(view, out, settings)
                else -> processPcm16(view, out, settings)
            }
            // Copy any leftover partial frame bytes untouched.
            if (view.hasRemaining()) {
                out.put(view)
            }
            out.flip()
        } catch (_: Throwable) {
            // Never take down ExoPlayer — deliver original PCM.
            try {
                inputBuffer.position(startPos)
                passthrough(inputBuffer)
            } catch (_: Throwable) {
                inputBuffer.position(inputBuffer.limit())
                replaceOutputBuffer(0).flip()
            }
        }
    }

    private fun passthrough(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }

    override fun onFlush() {
        bank.reset()
        loudnessEnvelope = 0.05f
        loudnessGain = 1f
    }

    override fun onReset() {
        onFlush()
    }

    private fun processPcm16(input: ByteBuffer, output: ByteBuffer, settings: AudioFxSettings) {
        val channels = channelCount
        val scratch = frameScratch
        val active = bank
        while (input.remaining() >= 2 * channels) {
            var rmsAcc = 0f
            for (c in 0 until channels) {
                var s = input.short / 32768f
                s = active.process(s, c, settings)
                scratch[c] = s
                rmsAcc += s * s
            }
            val gain = loudnessGainFor(settings, sqrt(rmsAcc / channels))
            for (c in 0 until channels) {
                val v = (scratch[c] * gain).coerceIn(-1f, 1f)
                output.putShort((v * 32767f).toInt().toShort())
            }
        }
    }

    private fun processFloat(input: ByteBuffer, output: ByteBuffer, settings: AudioFxSettings) {
        val channels = channelCount
        val scratch = frameScratch
        val active = bank
        while (input.remaining() >= 4 * channels) {
            var rmsAcc = 0f
            for (c in 0 until channels) {
                var s = input.float
                s = active.process(s, c, settings)
                scratch[c] = s
                rmsAcc += s * s
            }
            val gain = loudnessGainFor(settings, sqrt(rmsAcc / channels))
            for (c in 0 until channels) {
                output.putFloat((scratch[c] * gain).coerceIn(-1f, 1f))
            }
        }
    }

    private fun loudnessGainFor(settings: AudioFxSettings, frameRms: Float): Float {
        if (!settings.loudnessEnabled) {
            loudnessGain = 1f
            return 1f
        }
        val attack = 0.15f
        val release = 0.02f
        loudnessEnvelope = if (frameRms > loudnessEnvelope) {
            loudnessEnvelope + (frameRms - loudnessEnvelope) * attack
        } else {
            loudnessEnvelope + (frameRms - loudnessEnvelope) * release
        }
        val target = 0.12f
        val desired = if (loudnessEnvelope > 1e-4f) {
            (target / loudnessEnvelope).coerceIn(0.35f, 3.5f)
        } else {
            1f
        }
        loudnessGain += (desired - loudnessGain) * 0.08f
        return loudnessGain
    }

    /**
     * Immutable filter set for the audio thread.
     */
    private class FilterBank(
        private val preampGain: Float,
        private val peaking: Array<Biquad>,
        private val bassShelf: Biquad,
        private val bassBoostActive: Boolean,
    ) {
        fun process(sample: Float, channel: Int, settings: AudioFxSettings): Float {
            if (!settings.eqEnabled) return sample
            var s = sample * preampGain
            if (bassBoostActive) {
                s = bassShelf.process(s, channel)
            }
            for (filter in peaking) {
                s = filter.process(s, channel)
            }
            return s
        }

        fun reset() {
            peaking.forEach { it.reset() }
            bassShelf.reset()
        }

        companion object {
            fun identity(channels: Int): FilterBank = FilterBank(
                preampGain = 1f,
                peaking = emptyArray(),
                bassShelf = Biquad.passthrough(channels),
                bassBoostActive = false,
            )

            fun build(settings: AudioFxSettings, sampleRateHz: Int, channelCount: Int): FilterBank {
                val channels = channelCount.coerceIn(1, MAX_CHANNELS)
                val sr = sampleRateHz.toFloat().coerceAtLeast(8_000f)
                val nyquistGuard = (sr / 2f - 100f).coerceAtLeast(100f)
                val peaking = Array(AudioFxSettings.BAND_COUNT) { i ->
                    val freq = AudioFxSettings.BAND_FREQUENCIES_HZ[i].toFloat()
                        .coerceIn(20f, nyquistGuard)
                    val gainDb = settings.band(i)
                    if (kotlin.math.abs(gainDb) < 0.05f) {
                        Biquad.passthrough(channels)
                    } else {
                        Biquad.peaking(sr, freq, q = 1.4f, gainDb = gainDb, channels = channels)
                    }
                }
                val bassBoost = settings.bassBoostDb > 0.05f
                val shelf = if (bassBoost) {
                    Biquad.lowshelf(
                        sampleRate = sr,
                        freq = 100f,
                        q = 0.707f,
                        gainDb = settings.bassBoostDb,
                        channels = channels,
                    )
                } else {
                    Biquad.passthrough(channels)
                }
                return FilterBank(
                    preampGain = 10f.pow(settings.preampDb / 20f),
                    peaking = peaking,
                    bassShelf = shelf,
                    bassBoostActive = bassBoost,
                )
            }
        }
    }

    private class Biquad(
        private val b0: Float,
        private val b1: Float,
        private val b2: Float,
        private val a1: Float,
        private val a2: Float,
        channels: Int,
    ) {
        private val x1 = FloatArray(channels.coerceAtLeast(1))
        private val x2 = FloatArray(channels.coerceAtLeast(1))
        private val y1 = FloatArray(channels.coerceAtLeast(1))
        private val y2 = FloatArray(channels.coerceAtLeast(1))

        fun process(input: Float, channel: Int): Float {
            if (x1.isEmpty()) return input
            val c = channel.coerceIn(0, x1.lastIndex)
            val y = b0 * input + b1 * x1[c] + b2 * x2[c] - a1 * y1[c] - a2 * y2[c]
            if (!y.isFinite()) return input
            x2[c] = x1[c]
            x1[c] = input
            y2[c] = y1[c]
            y1[c] = y
            return y
        }

        fun reset() {
            x1.fill(0f)
            x2.fill(0f)
            y1.fill(0f)
            y2.fill(0f)
        }

        companion object {
            fun passthrough(channels: Int): Biquad =
                Biquad(1f, 0f, 0f, 0f, 0f, channels = channels.coerceAtLeast(1))

            fun peaking(
                sampleRate: Float,
                freq: Float,
                q: Float,
                gainDb: Float,
                channels: Int,
            ): Biquad {
                val a = 10.0.pow(gainDb / 40.0)
                val w0 = 2.0 * PI * freq / sampleRate
                val alpha = sin(w0) / (2.0 * q)
                val cosW = cos(w0)
                val b0 = 1 + alpha * a
                val b1 = -2 * cosW
                val b2 = 1 - alpha * a
                val a0 = 1 + alpha / a
                val a1 = -2 * cosW
                val a2 = 1 - alpha / a
                return Biquad(
                    b0 = (b0 / a0).toFloat(),
                    b1 = (b1 / a0).toFloat(),
                    b2 = (b2 / a0).toFloat(),
                    a1 = (a1 / a0).toFloat(),
                    a2 = (a2 / a0).toFloat(),
                    channels = channels,
                )
            }

            fun lowshelf(
                sampleRate: Float,
                freq: Float,
                q: Float,
                gainDb: Float,
                channels: Int,
            ): Biquad {
                val a = 10.0.pow(gainDb / 40.0)
                val w0 = 2.0 * PI * freq / sampleRate
                val alpha = sin(w0) / (2.0 * q)
                val cosW = cos(w0)
                val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
                val b0 = a * ((a + 1) - (a - 1) * cosW + twoSqrtAAlpha)
                val b1 = 2 * a * ((a - 1) - (a + 1) * cosW)
                val b2 = a * ((a + 1) - (a - 1) * cosW - twoSqrtAAlpha)
                val a0 = (a + 1) + (a - 1) * cosW + twoSqrtAAlpha
                val a1 = -2 * ((a - 1) + (a + 1) * cosW)
                val a2 = (a + 1) + (a - 1) * cosW - twoSqrtAAlpha
                return Biquad(
                    b0 = (b0 / a0).toFloat(),
                    b1 = (b1 / a0).toFloat(),
                    b2 = (b2 / a0).toFloat(),
                    a1 = (a1 / a0).toFloat(),
                    a2 = (a2 / a0).toFloat(),
                    channels = channels,
                )
            }
        }
    }

    companion object {
        private const val MAX_CHANNELS = 8
    }
}
