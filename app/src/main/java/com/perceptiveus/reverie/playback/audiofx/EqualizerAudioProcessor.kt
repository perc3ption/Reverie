package com.perceptiveus.reverie.playback.audiofx

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.concurrent.atomic.AtomicReference

/**
 * PCM equalizer + optional soft loudness leveling for ExoPlayer's audio chain.
 */
@OptIn(UnstableApi::class)
class EqualizerAudioProcessor : BaseAudioProcessor() {

    private val settingsRef = AtomicReference(AudioFxSettings.Default)

    private var sampleRateHz: Int = 44_100
    private var channelCount: Int = 2
    private var encoding: Int = C.ENCODING_PCM_16BIT

    private var peakingFilters: Array<Biquad> = emptyArray()
    private var bassShelf: Biquad = Biquad.passthrough()
    private var preampGain: Float = 1f

    // Soft loudness AGC state
    private var loudnessEnvelope: Float = 0.05f
    private var loudnessGain: Float = 1f

    fun applySettings(settings: AudioFxSettings) {
        settingsRef.set(settings)
        rebuildFilters()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRateHz = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        rebuildFilters()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val settings = settingsRef.get()
        if (!settings.eqEnabled && !settings.loudnessEnabled) {
            val out = replaceOutputBuffer(inputBuffer.remaining())
            out.put(inputBuffer)
            out.flip()
            return
        }

        val remaining = inputBuffer.remaining()
        val out = replaceOutputBuffer(remaining)
        val view = inputBuffer.order(ByteOrder.nativeOrder())

        when (encoding) {
            C.ENCODING_PCM_FLOAT -> processFloat(view, out, settings)
            else -> processPcm16(view, out, settings)
        }
        out.flip()
    }

    override fun onFlush() {
        peakingFilters.forEach { it.reset() }
        bassShelf.reset()
        loudnessEnvelope = 0.05f
        loudnessGain = 1f
    }

    override fun onReset() {
        onFlush()
    }

    private fun processPcm16(input: ByteBuffer, output: ByteBuffer, settings: AudioFxSettings) {
        val channels = channelCount
        while (input.remaining() >= 2 * channels) {
            val samples = FloatArray(channels)
            var rmsAcc = 0f
            for (c in 0 until channels) {
                var s = input.short / 32768f
                s = processSample(s, c, settings)
                samples[c] = s
                rmsAcc += s * s
            }
            val gain = loudnessGainFor(settings, sqrt(rmsAcc / channels))
            for (c in 0 until channels) {
                val v = (samples[c] * gain).coerceIn(-1f, 1f)
                output.putShort((v * 32767f).toInt().toShort())
            }
        }
    }

    private fun processFloat(input: ByteBuffer, output: ByteBuffer, settings: AudioFxSettings) {
        val channels = channelCount
        while (input.remaining() >= 4 * channels) {
            val samples = FloatArray(channels)
            var rmsAcc = 0f
            for (c in 0 until channels) {
                var s = input.float
                s = processSample(s, c, settings)
                samples[c] = s
                rmsAcc += s * s
            }
            val gain = loudnessGainFor(settings, sqrt(rmsAcc / channels))
            for (c in 0 until channels) {
                output.putFloat((samples[c] * gain).coerceIn(-1f, 1f))
            }
        }
    }

    private fun processSample(sample: Float, channel: Int, settings: AudioFxSettings): Float {
        var s = sample
        if (settings.eqEnabled) {
            s *= preampGain
            if (settings.bassBoostDb > 0.05f) {
                s = bassShelf.process(s, channel)
            }
            for (filter in peakingFilters) {
                s = filter.process(s, channel)
            }
        }
        return s
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

    private fun rebuildFilters() {
        val settings = settingsRef.get()
        val sr = sampleRateHz.toFloat().coerceAtLeast(8_000f)
        preampGain = dbToGain(settings.preampDb)

        peakingFilters = Array(AudioFxSettings.BAND_COUNT) { i ->
            val freq = AudioFxSettings.BAND_FREQUENCIES_HZ[i].toFloat().coerceAtMost(sr / 2f - 100f)
            val gainDb = settings.band(i)
            if (kotlin.math.abs(gainDb) < 0.05f) {
                Biquad.passthrough()
            } else {
                Biquad.peaking(sr, freq, q = 1.4f, gainDb = gainDb, channels = channelCount)
            }
        }

        bassShelf = if (settings.bassBoostDb > 0.05f) {
            Biquad.lowshelf(
                sampleRate = sr,
                freq = 100f,
                q = 0.707f,
                gainDb = settings.bassBoostDb,
                channels = channelCount,
            )
        } else {
            Biquad.passthrough()
        }
    }

    private fun dbToGain(db: Float): Float = 10f.pow(db / 20f)

    /**
     * Per-channel biquad (Direct Form I).
     */
    private class Biquad(
        private val b0: Float,
        private val b1: Float,
        private val b2: Float,
        private val a1: Float,
        private val a2: Float,
        channels: Int,
    ) {
        private val x1 = FloatArray(channels)
        private val x2 = FloatArray(channels)
        private val y1 = FloatArray(channels)
        private val y2 = FloatArray(channels)

        fun process(input: Float, channel: Int): Float {
            val c = channel.coerceIn(0, x1.lastIndex)
            val y = b0 * input + b1 * x1[c] + b2 * x2[c] - a1 * y1[c] - a2 * y2[c]
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
            fun passthrough(): Biquad = Biquad(1f, 0f, 0f, 0f, 0f, channels = 8)

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
                    channels = channels.coerceAtLeast(1),
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
                    channels = channels.coerceAtLeast(1),
                )
            }
        }
    }
}
