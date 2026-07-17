package com.perceptiveus.reverie.feature.player.visualizer

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Captures FFT (+ waveform) from the player's audio session.
 * Falls back to a musically-timed procedural spectrum when the system Visualizer
 * can't attach (common on some emulators / OEM builds).
 */
class AudioSpectrumCollector(
    private val barCount: Int = DEFAULT_BAR_COUNT,
    private val onFrame: (spectrum: FloatArray, peaks: FloatArray, waveform: FloatArray) -> Unit,
    private val onModeChanged: (mode: CaptureMode) -> Unit = {},
) {
    enum class CaptureMode {
        /** Real FFT/waveform from [Visualizer] attached to the player session. */
        LIVE,
        /** Procedural animation when live capture is unavailable. */
        FALLBACK,
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    private var isPlaying: Boolean = false
    private var positionMs: Long = 0L
    private var fallbackRunnable: Runnable? = null
    private var usingFallback: Boolean = false

    private val spectrum = FloatArray(barCount)
    private val peaks = FloatArray(barCount)
    private val waveform = FloatArray(WAVEFORM_POINTS)

    fun updatePlayback(audioSessionId: Int, isPlaying: Boolean, positionMs: Long) {
        this.isPlaying = isPlaying
        this.positionMs = positionMs
        if (audioSessionId != this.audioSessionId && audioSessionId > 0) {
            this.audioSessionId = audioSessionId
            restartVisualizer()
        }
        if (!isPlaying) {
            decayToIdle()
        } else if (usingFallback) {
            ensureFallbackLoop()
        }
    }

    fun start() {
        restartVisualizer()
    }

    fun stop() {
        stopFallback()
        releaseVisualizer()
    }

    private fun setMode(fallback: Boolean) {
        usingFallback = fallback
        val mode = if (fallback) CaptureMode.FALLBACK else CaptureMode.LIVE
        mainHandler.post { onModeChanged(mode) }
    }

    private fun restartVisualizer() {
        releaseVisualizer()
        if (audioSessionId <= 0) {
            setMode(fallback = true)
            ensureFallbackLoop()
            return
        }
        try {
            val viz = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            viz.captureSize = range[1].coerceAtMost(1024).coerceAtLeast(range[0])
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        bytes: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (bytes == null) return
                        downsampleWaveform(bytes)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        bytes: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (bytes == null) return
                        fftToSpectrum(bytes)
                        emit()
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                true,
            )
            viz.enabled = true
            visualizer = viz
            stopFallback()
            setMode(fallback = false)
        } catch (_: Throwable) {
            setMode(fallback = true)
            ensureFallbackLoop()
        }
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Throwable) {
            // Ignore teardown races.
        }
        visualizer = null
    }

    private fun fftToSpectrum(fft: ByteArray) {
        // Visualizer FFT: byte0 = DC, then n/2-1 complex pairs (re, im).
        val n = fft.size
        val usable = (n / 2 - 1).coerceAtLeast(1)
        for (i in 0 until barCount) {
            val start = 1 + (i * usable) / barCount
            val end = 1 + ((i + 1) * usable) / barCount
            var maxMag = 0f
            var idx = start
            while (idx < end && idx < usable) {
                val re = fft[idx * 2].toInt()
                val im = fft[idx * 2 + 1].toInt()
                val mag = sqrt((re * re + im * im).toFloat())
                if (mag > maxMag) maxMag = mag
                idx++
            }
            // Soft log-ish compress into 0..1
            val normalized = (maxMag / 90f).coerceIn(0f, 1f)
            // Smooth rise, slower fall for that classic LED feel
            spectrum[i] = if (normalized > spectrum[i]) {
                spectrum[i] * 0.35f + normalized * 0.65f
            } else {
                spectrum[i] * 0.82f + normalized * 0.18f
            }
            if (spectrum[i] > peaks[i]) {
                peaks[i] = spectrum[i]
            } else {
                peaks[i] = (peaks[i] - 0.012f).coerceAtLeast(spectrum[i])
            }
        }
    }

    private fun downsampleWaveform(bytes: ByteArray) {
        val step = bytes.size.toFloat() / WAVEFORM_POINTS
        for (i in 0 until WAVEFORM_POINTS) {
            val sample = bytes[(i * step).toInt().coerceIn(0, bytes.lastIndex)].toInt() and 0xFF
            // Center around 0 in -1..1
            waveform[i] = (sample - 128) / 128f
        }
    }

    private fun ensureFallbackLoop() {
        if (fallbackRunnable != null) return
        val runnable = object : Runnable {
            override fun run() {
                if (!usingFallback) return
                if (isPlaying) {
                    proceduralFrame(positionMs)
                    emit()
                } else {
                    decayToIdle()
                }
                fallbackRunnable = this
                mainHandler.postDelayed(this, 33L)
            }
        }
        fallbackRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopFallback() {
        fallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackRunnable = null
    }

    private fun proceduralFrame(timeMs: Long) {
        val t = timeMs / 1000f
        for (i in 0 until barCount) {
            val freq = 0.55f + i * 0.11f
            val wave = (
                0.55f +
                    0.28f * sin(t * freq + i * 0.4f) +
                    0.18f * sin(t * (freq * 1.7f) + i) +
                    0.12f * cos(t * 2.3f + i * 0.2f)
                ).coerceIn(0.08f, 1f)
            // Emphasize bass-ish left and sparkle on right
            val bias = when {
                i < barCount / 5 -> 1.15f
                i > barCount * 4 / 5 -> 0.85f + 0.2f * sin(t * 8f + i)
                else -> 1f
            }
            val target = (wave * bias).coerceIn(0f, 1f)
            spectrum[i] = spectrum[i] * 0.55f + target * 0.45f
            if (spectrum[i] > peaks[i]) peaks[i] = spectrum[i]
            else peaks[i] = (peaks[i] - 0.015f).coerceAtLeast(spectrum[i])
        }
        for (i in 0 until WAVEFORM_POINTS) {
            val x = i / WAVEFORM_POINTS.toFloat()
            waveform[i] = (
                0.55f * sin(t * 6.2f + x * 10f) +
                    0.25f * sin(t * 13f + x * 22f) +
                    0.12f * sin(t * 27f + x * 40f)
                ).coerceIn(-1f, 1f)
        }
    }

    private fun decayToIdle() {
        var any = false
        for (i in spectrum.indices) {
            if (spectrum[i] > 0.01f || peaks[i] > 0.01f) any = true
            spectrum[i] *= 0.88f
            peaks[i] *= 0.92f
            if (spectrum[i] < 0.01f) spectrum[i] = 0f
            if (peaks[i] < 0.01f) peaks[i] = 0f
        }
        for (i in waveform.indices) {
            waveform[i] *= 0.85f
        }
        if (any) emit()
    }

    private fun emit() {
        val spectrumOut = spectrum.copyOf()
        val peaksOut = peaks.copyOf()
        val waveOut = waveform.copyOf()
        mainHandler.post { onFrame(spectrumOut, peaksOut, waveOut) }
    }

    companion object {
        const val DEFAULT_BAR_COUNT = 48
        const val WAVEFORM_POINTS = 96
    }
}
