package com.perceptiveus.reverie.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Taps PCM from ExoPlayer via [TeeAudioProcessor] and publishes spectrum / waveform
 * frames for the player visualizer — no microphone / RECORD_AUDIO required.
 *
 * Create a **new** tee per [PlaybackService] session via [createTeeProcessor]
 * (Media3 sinks must not reuse processor instances).
 */
@UnstableApi
object PlaybackAudioAnalyzer {

    data class Frame(
        val spectrum: FloatArray = FloatArray(BAR_COUNT),
        val peaks: FloatArray = FloatArray(BAR_COUNT),
        val waveform: FloatArray = FloatArray(WAVEFORM_POINTS),
    )

    private val _frame = MutableStateFlow(Frame())
    val frame: StateFlow<Frame> = _frame.asStateFlow()

    private const val FFT_SIZE = 1024
    private const val BAR_COUNT = 48
    private const val WAVEFORM_POINTS = 96
    /** ~30 FPS — keeps audio-thread work bounded. */
    private const val MIN_EMIT_INTERVAL_MS = 33L
    /**
     * Slow AGC reference so one bass spike does not crush every other bar.
     * Fast attack / slow decay keeps the display “hot” across quiet passages.
     */
    private const val ENVELOPE_ATTACK = 0.55f
    private const val ENVELOPE_DECAY = 0.985f
    /** Extra punch after log mapping; soft-clamped to 0..1. */
    private const val DISPLAY_GAIN = 1.75f
    /** Compresses mid values upward so bars use more of the canvas. */
    private const val DISPLAY_GAMMA = 0.58f
    /** Soft-clip gain for the oscilloscope / waveform skins. */
    private const val WAVEFORM_GAIN = 2.4f

    private val sampleRing = FloatArray(FFT_SIZE)
    private var sampleWriteIndex = 0
    private var samplesFilled = 0

    private val fftReal = FloatArray(FFT_SIZE)
    private val fftImag = FloatArray(FFT_SIZE)
    private val magnitudes = FloatArray(FFT_SIZE / 2)
    private val spectrum = FloatArray(BAR_COUNT)
    private val peaks = FloatArray(BAR_COUNT)
    private val waveform = FloatArray(WAVEFORM_POINTS)
    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5f * (1.0 - cos(2.0 * Math.PI * i / (FFT_SIZE - 1)))).toFloat()
    }

    /** Decaying peak magnitude used as the spectrum AGC reference. */
    private var magnitudeEnvelope = 1e-3f

    @Volatile
    private var channelCount: Int = 2

    @Volatile
    private var encoding: Int = C.ENCODING_PCM_16BIT

    private var lastEmitAt = 0L

    private val lock = Any()

    /** Fresh tee for each ExoPlayer / AudioSink. */
    fun createTeeProcessor(): AudioProcessor = TeeAudioProcessor(SafeSink())

    fun reset() {
        synchronized(lock) {
            sampleWriteIndex = 0
            samplesFilled = 0
            magnitudeEnvelope = 1e-3f
            spectrum.fill(0f)
            peaks.fill(0f)
            waveform.fill(0f)
            _frame.value = Frame()
        }
    }

    /**
     * Never throws — exceptions on the audio thread become ExoPlayer STATE_ERROR.
     */
    private class SafeSink : TeeAudioProcessor.AudioBufferSink {
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            try {
                PlaybackAudioAnalyzer.channelCount = channelCount.coerceIn(1, 8)
                PlaybackAudioAnalyzer.encoding = encoding
                synchronized(lock) {
                    sampleWriteIndex = 0
                    samplesFilled = 0
                }
            } catch (_: Throwable) {
                // ignore
            }
        }

        override fun handleBuffer(buffer: ByteBuffer) {
            if (!buffer.hasRemaining()) return
            try {
                val channels = channelCount.coerceIn(1, 8)
                val enc = encoding
                val view = buffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
                synchronized(lock) {
                    when (enc) {
                        C.ENCODING_PCM_16BIT -> ingestPcm16(view, channels)
                        C.ENCODING_PCM_FLOAT -> ingestPcmFloat(view, channels)
                        else -> return
                    }
                    maybePublishLocked()
                }
            } catch (_: Throwable) {
                // Drop this buffer; never fail the sink.
            }
        }
    }

    private fun ingestPcm16(buffer: ByteBuffer, channels: Int) {
        val bytesPerFrame = 2 * channels
        if (bytesPerFrame <= 0) return
        val frames = buffer.remaining() / bytesPerFrame
        var i = 0
        while (i < frames) {
            var sum = 0f
            var c = 0
            while (c < channels) {
                sum += buffer.short / 32768f
                c++
            }
            pushSample(sum / channels)
            i++
        }
    }

    private fun ingestPcmFloat(buffer: ByteBuffer, channels: Int) {
        val bytesPerFrame = 4 * channels
        if (bytesPerFrame <= 0) return
        val frames = buffer.remaining() / bytesPerFrame
        var i = 0
        while (i < frames) {
            var sum = 0f
            var c = 0
            while (c < channels) {
                sum += buffer.float
                c++
            }
            pushSample(sum / channels)
            i++
        }
    }

    private fun pushSample(sample: Float) {
        val s = if (sample.isFinite()) sample.coerceIn(-1f, 1f) else 0f
        sampleRing[sampleWriteIndex] = s
        sampleWriteIndex = (sampleWriteIndex + 1) % FFT_SIZE
        if (samplesFilled < FFT_SIZE) samplesFilled++
    }

    private fun maybePublishLocked() {
        if (samplesFilled < FFT_SIZE / 4) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastEmitAt < MIN_EMIT_INTERVAL_MS) return
        lastEmitAt = now

        val start = (sampleWriteIndex - min(samplesFilled, FFT_SIZE) + FFT_SIZE) % FFT_SIZE
        val count = min(samplesFilled, FFT_SIZE)
        fftImag.fill(0f)
        for (i in 0 until FFT_SIZE) {
            val sample = if (i < count) {
                sampleRing[(start + i) % FFT_SIZE]
            } else {
                0f
            }
            fftReal[i] = sample * hannWindow[i]
        }

        fftInPlace(fftReal, fftImag)

        val half = FFT_SIZE / 2
        var maxMag = 1e-6f
        for (i in 0 until half) {
            val re = fftReal[i]
            val im = fftImag[i]
            // Mild high-frequency pre-emphasis so mid/treble stay visible next to bass.
            val tilt = 1f + 1.6f * (i.toFloat() / half)
            val mag = sqrt(re * re + im * im) * tilt
            magnitudes[i] = mag
            if (mag > maxMag) maxMag = mag
        }

        magnitudeEnvelope = if (maxMag > magnitudeEnvelope) {
            magnitudeEnvelope * (1f - ENVELOPE_ATTACK) + maxMag * ENVELOPE_ATTACK
        } else {
            magnitudeEnvelope * ENVELOPE_DECAY + maxMag * (1f - ENVELOPE_DECAY)
        }.coerceAtLeast(1e-4f)

        val usable = (half - 1).coerceAtLeast(1)
        for (bar in 0 until BAR_COUNT) {
            val t0 = bar / BAR_COUNT.toFloat()
            val t1 = (bar + 1) / BAR_COUNT.toFloat()
            val startBin = 1 + (logMap(t0) * usable).toInt()
            val endBin = 1 + (logMap(t1) * usable).toInt().coerceAtLeast(startBin + 1)
            var peak = 0f
            var idx = startBin
            while (idx < endBin && idx < half) {
                if (magnitudes[idx] > peak) peak = magnitudes[idx]
                idx++
            }
            val relative = (peak / magnitudeEnvelope).coerceIn(0f, 1f)
            // Log lift + gamma + gain: punchier bars without hard clipping.
            val lifted = (ln(1.0 + relative * 12.0) / ln(13.0)).toFloat()
            val normalized = (lifted.pow(DISPLAY_GAMMA) * DISPLAY_GAIN).coerceIn(0f, 1f)
            spectrum[bar] = if (normalized > spectrum[bar]) {
                spectrum[bar] * 0.25f + normalized * 0.75f
            } else {
                spectrum[bar] * 0.72f + normalized * 0.28f
            }
            if (spectrum[bar] > peaks[bar]) {
                peaks[bar] = spectrum[bar]
            } else {
                peaks[bar] = (peaks[bar] - 0.014f).coerceAtLeast(spectrum[bar])
            }
        }

        val waveStart = (sampleWriteIndex - WAVEFORM_POINTS + FFT_SIZE) % FFT_SIZE
        for (i in 0 until WAVEFORM_POINTS) {
            val sample = sampleRing[(waveStart + i) % FFT_SIZE]
            waveform[i] = tanh(sample * WAVEFORM_GAIN)
        }

        _frame.value = Frame(
            spectrum = spectrum.copyOf(),
            peaks = peaks.copyOf(),
            waveform = waveform.copyOf(),
        )
    }

    private fun logMap(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return ((ln(1.0 + 9.0 * clamped) / ln(10.0)).toFloat())
    }

    private fun fftInPlace(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]
                real[i] = real[j]
                real[j] = tr
                val ti = imag[i]
                imag[i] = imag[j]
                imag[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * Math.PI / len
            val wLenRe = cos(ang).toFloat()
            val wLenIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wRe = 1f
                var wIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = real[i + k]
                    val uIm = imag[i + k]
                    val vRe = real[i + k + len / 2] * wRe - imag[i + k + len / 2] * wIm
                    val vIm = real[i + k + len / 2] * wIm + imag[i + k + len / 2] * wRe
                    real[i + k] = uRe + vRe
                    imag[i + k] = uIm + vIm
                    real[i + k + len / 2] = uRe - vRe
                    imag[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wLenRe - wIm * wLenIm
                    wIm = wRe * wLenIm + wIm * wLenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    const val DEFAULT_BAR_COUNT = BAR_COUNT
    const val DEFAULT_WAVEFORM_POINTS = WAVEFORM_POINTS
}
