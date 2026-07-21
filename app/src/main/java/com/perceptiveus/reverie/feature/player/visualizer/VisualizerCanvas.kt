package com.perceptiveus.reverie.feature.player.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.perceptiveus.reverie.core.design.ReveriePurple
import com.perceptiveus.reverie.core.design.ReveriePurpleGlow
import com.perceptiveus.reverie.domain.model.PlayerProgress
import com.perceptiveus.reverie.playback.PlaybackAudioAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val CrtBlack = Color(0xFF050508)
private val GridLine = Color(0xFF1A1630)
private val PhosphorGreen = Color(0xFF39FF14)
private val PhosphorCyan = Color(0xFF00F5FF)
private val AmberLed = Color(0xFFFFB300)
private val HotRed = Color(0xFFFF1744)
private val SoftViolet = Color(0xFFB388FF)

/**
 * Owns frame buffers and a draw tick so only this Canvas invalidates at ~30fps —
 * not the surrounding GlassSurface / style picker.
 */
@Composable
fun VisualizerCanvas(
    style: VisualizerStyle,
    frameFlow: StateFlow<PlaybackAudioAnalyzer.Frame>,
    playerProgress: StateFlow<PlayerProgress>,
    modifier: Modifier = Modifier,
) {
    val isPlaying by remember(playerProgress) {
        playerProgress.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = playerProgress.value.isPlaying)

    val spectrum = remember {
        FloatArray(PlaybackAudioAnalyzer.DEFAULT_BAR_COUNT)
    }
    val peaks = remember {
        FloatArray(PlaybackAudioAnalyzer.DEFAULT_BAR_COUNT)
    }
    val waveform = remember {
        FloatArray(PlaybackAudioAnalyzer.DEFAULT_WAVEFORM_POINTS)
    }
    var drawTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(frameFlow, isPlaying) {
        if (isPlaying) {
            frameFlow.collect { frame ->
                frame.spectrum.copyInto(spectrum)
                frame.peaks.copyInto(peaks)
                frame.waveform.copyInto(waveform)
                drawTick++
            }
        } else {
            while (isActive) {
                var any = false
                for (i in spectrum.indices) {
                    val v = spectrum[i] * 0.88f
                    spectrum[i] = if (v < 0.01f) 0f else v
                    if (spectrum[i] > 0.01f) any = true
                }
                for (i in peaks.indices) {
                    val v = peaks[i] * 0.92f
                    peaks[i] = if (v < 0.01f) 0f else v
                    if (peaks[i] > 0.01f) any = true
                }
                for (i in waveform.indices) {
                    waveform[i] *= 0.85f
                }
                drawTick++
                if (!any) break
                delay(33)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Establish read dependency so only this Canvas redraws on tick.
        drawTick

        drawRect(CrtBlack)
        // Subtle CRT scanlines
        val lineStep = 3f
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color.White.copy(alpha = 0.025f),
                topLeft = Offset(0f, y),
                size = Size(size.width, 1f),
            )
            y += lineStep
        }

        when (style) {
            VisualizerStyle.SPECTRUM -> drawSpectrum(spectrum, peaks)
            VisualizerStyle.SCOPE -> drawOscilloscope(waveform)
            VisualizerStyle.RADIAL -> drawRadial(spectrum)
            VisualizerStyle.VU -> drawVuMeters(spectrum)
            VisualizerStyle.STARBURST -> drawStarburst(spectrum, waveform)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpectrum(
    spectrum: FloatArray,
    peaks: FloatArray,
) {
    if (spectrum.isEmpty()) return
    val pad = 10f
    val usableW = size.width - pad * 2
    val usableH = size.height - pad * 2
    val gap = 2.5f
    val barW = ((usableW - gap * (spectrum.size - 1)) / spectrum.size).coerceAtLeast(2f)

    // Soft floor glow
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, ReveriePurple.copy(alpha = 0.12f)),
            startY = size.height * 0.55f,
            endY = size.height,
        ),
    )

    for (i in spectrum.indices) {
        val level = spectrum[i].coerceIn(0f, 1f)
        val peak = peaks.getOrElse(i) { level }.coerceIn(0f, 1f)
        val h = usableH * level
        val x = pad + i * (barW + gap)
        val top = pad + usableH - h

        val brush = Brush.verticalGradient(
            colors = listOf(PhosphorCyan, ReveriePurpleGlow, ReveriePurple.copy(alpha = 0.85f)),
            startY = top,
            endY = pad + usableH,
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, top),
            size = Size(barW, h.coerceAtLeast(2f)),
            cornerRadius = CornerRadius(2f, 2f),
        )

        // Peak cap — classic Winamp tick
        val peakY = pad + usableH - usableH * peak
        drawRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(x, peakY),
            size = Size(barW, 2.5f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOscilloscope(waveform: FloatArray) {
    if (waveform.isEmpty()) return
    val pad = 12f
    // Grid
    for (i in 1..3) {
        val gy = size.height * i / 4f
        drawLine(GridLine, Offset(pad, gy), Offset(size.width - pad, gy), strokeWidth = 1f)
    }
    for (i in 1..5) {
        val gx = size.width * i / 6f
        drawLine(GridLine, Offset(gx, pad), Offset(gx, size.height - pad), strokeWidth = 1f)
    }

    val midY = size.height / 2f
    val amp = (size.height / 2f - pad) * 0.92f
    val path = Path()
    waveform.forEachIndexed { i, sample ->
        val x = pad + (size.width - pad * 2) * i / (waveform.size - 1).coerceAtLeast(1)
        val y = midY - sample.coerceIn(-1f, 1f) * amp
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    // Glow pass
    drawPath(
        path = path,
        color = PhosphorGreen.copy(alpha = 0.25f),
        style = Stroke(width = 8f, cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = PhosphorGreen,
        style = Stroke(width = 2.4f, cap = StrokeCap.Round),
    )
    // Center hairline
    drawLine(
        color = PhosphorGreen.copy(alpha = 0.2f),
        start = Offset(pad, midY),
        end = Offset(size.width - pad, midY),
        strokeWidth = 1f,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadial(spectrum: FloatArray) {
    if (spectrum.isEmpty()) return
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxR = min(size.width, size.height) * 0.42f
    val inner = maxR * 0.22f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ReveriePurple.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxR,
        ),
        radius = maxR,
        center = Offset(cx, cy),
    )

    val count = spectrum.size
    for (i in 0 until count) {
        val level = spectrum[i].coerceIn(0f, 1f)
        val angle = (i.toFloat() / count) * PI.toFloat() * 2f - PI.toFloat() / 2f
        val outer = inner + (maxR - inner) * level
        val cosA = cos(angle)
        val sinA = sin(angle)
        val start = Offset(cx + cosA * inner, cy + sinA * inner)
        val end = Offset(cx + cosA * outer, cy + sinA * outer)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(SoftViolet, PhosphorCyan),
                start = start,
                end = end,
            ),
            start = start,
            end = end,
            strokeWidth = 3.2f,
            cap = StrokeCap.Round,
        )
    }

    drawCircle(
        color = ReveriePurpleGlow.copy(alpha = 0.8f),
        radius = inner * 0.35f,
        center = Offset(cx, cy),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVuMeters(spectrum: FloatArray) {
    if (spectrum.isEmpty()) return
    val left = spectrum.take(spectrum.size / 2).average().toFloat().coerceIn(0f, 1f)
    val right = spectrum.drop(spectrum.size / 2).average().toFloat().coerceIn(0f, 1f)
    val pad = 16f
    val meterH = (size.height - pad * 3) / 2f
    drawVuChannel(labelBoost = left, top = pad, height = meterH)
    drawVuChannel(labelBoost = right, top = pad * 2 + meterH, height = meterH)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVuChannel(
    labelBoost: Float,
    top: Float,
    height: Float,
) {
    val padX = 16f
    val segments = 24
    val gap = 3f
    val segW = ((size.width - padX * 2) - gap * (segments - 1)) / segments
    val lit = (labelBoost * segments).toInt().coerceIn(0, segments)

    for (i in 0 until segments) {
        val x = padX + i * (segW + gap)
        val color = when {
            i > segments * 0.85f -> HotRed
            i > segments * 0.65f -> AmberLed
            else -> PhosphorGreen
        }
        val on = i < lit
        drawRoundRect(
            color = if (on) color else color.copy(alpha = 0.12f),
            topLeft = Offset(x, top),
            size = Size(segW, height),
            cornerRadius = CornerRadius(2f, 2f),
        )
        if (on) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(x, top),
                size = Size(segW, height * 0.25f),
                cornerRadius = CornerRadius(2f, 2f),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarburst(
    spectrum: FloatArray,
    waveform: FloatArray,
) {
    if (spectrum.isEmpty()) return
    val cx = size.width / 2f
    val cy = size.height / 2f
    val energy = spectrum.average().toFloat().coerceIn(0f, 1f)
    val rays = spectrum.size.coerceAtMost(36)

    clipRect {
        for (i in 0 until rays) {
            val level = spectrum[i % spectrum.size].coerceIn(0f, 1f)
            val angle = (i.toFloat() / rays) * PI.toFloat() * 2f + energy * 0.8f
            val len = min(size.width, size.height) * (0.18f + 0.38f * level)
            val end = Offset(cx + cos(angle) * len, cy + sin(angle) * len)
            drawLine(
                brush = Brush.linearGradient(
                    listOf(ReveriePurpleGlow.copy(alpha = 0.1f), PhosphorCyan.copy(alpha = 0.9f)),
                    start = Offset(cx, cy),
                    end = end,
                ),
                start = Offset(cx, cy),
                end = end,
                strokeWidth = 2f + level * 3f,
                cap = StrokeCap.Round,
            )
        }

        // Orbital ring from waveform energy
        val ringR = min(size.width, size.height) * (0.2f + 0.08f * energy)
        drawCircle(
            color = SoftViolet.copy(alpha = 0.35f),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 2f),
        )

        if (waveform.isNotEmpty()) {
            val path = Path()
            val amp = ringR * 0.35f
            waveform.forEachIndexed { i, sample ->
                val a = (i.toFloat() / waveform.size) * PI.toFloat() * 2f
                val r = ringR + sample * amp
                val p = Offset(cx + cos(a) * r, cy + sin(a) * r)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(
                path = path,
                color = PhosphorCyan.copy(alpha = 0.55f),
                style = Stroke(width = 1.8f),
            )
        }
    }
}
