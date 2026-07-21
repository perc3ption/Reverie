package com.perceptiveus.reverie.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ReverieCardShape = RoundedCornerShape(20.dp)
val ReverieTileShape = RoundedCornerShape(16.dp)
val ReverieArtShape = RoundedCornerShape(12.dp)

/**
 * Soft purple outer glow + thin neon border — the mockup "fancy border".
 * Uses layered colored shadows (no blur RenderEffect dependency).
 */
fun Modifier.glowBorder(
    color: Color = ReverieGlowBorder,
    shape: Shape = ReverieCardShape,
    borderWidth: Dp = 1.dp,
    glowRadius: Dp = 10.dp,
    borderAlpha: Float = 0.75f,
    glowAlpha: Float = 0.4f,
): Modifier = this
    .shadow(
        elevation = glowRadius,
        shape = shape,
        clip = false,
        ambientColor = color.copy(alpha = glowAlpha),
        spotColor = color.copy(alpha = glowAlpha * 0.6f),
    )
    .border(
        width = borderWidth,
        color = color.copy(alpha = borderAlpha),
        shape = shape,
    )

/**
 * Subtle glass fill: slightly elevated dark surface with a soft top highlight.
 */
fun Modifier.glassBackground(
    shape: Shape = ReverieCardShape,
    fill: Color = ReverieGlass,
): Modifier = this
    .clip(shape)
    .background(fill, shape)
    .drawBehind {
        val highlight = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = size.height * 0.45f,
        )
        val cornerPx = when (shape) {
            is RoundedCornerShape -> 20.dp.toPx()
            else -> 16.dp.toPx()
        }
        drawRoundRect(
            brush = highlight,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )
    }

/**
 * Soft under-glow wash for selected nav icons (drawn behind the icon).
 */
fun Modifier.navUnderGlow(
    visible: Boolean,
    color: Color = ReveriePurple,
): Modifier = if (!visible) {
    this
} else {
    this.drawBehind {
        val radius = size.minDimension * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.45f),
                    color.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = Offset(size.width / 2f, size.height * 0.65f),
                radius = radius,
            ),
            radius = radius,
            center = Offset(size.width / 2f, size.height * 0.65f),
        )
    }
}

/**
 * Circular play control with a glowing purple ring.
 */
fun Modifier.glowRing(
    color: Color = ReveriePurple,
    ringWidth: Dp = 2.dp,
    glowWidth: Dp = 6.dp,
): Modifier = this.drawBehind {
    val stroke = ringWidth.toPx()
    val glow = glowWidth.toPx()
    val radius = (size.minDimension / 2f) - stroke / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = radius + glow * 0.35f,
        center = center,
        style = Stroke(width = glow),
    )
    drawCircle(
        color = color.copy(alpha = 0.9f),
        radius = radius,
        center = center,
        style = Stroke(width = stroke),
    )
}
