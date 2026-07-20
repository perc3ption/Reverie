package com.perceptiveus.reverie.core.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Wraps [content] and adds a thin leading-edge hit strip for swipe-back.
 * The strip is the only gesture target, so taps elsewhere (play, lists, etc.)
 * are never intercepted.
 */
@Composable
fun EdgeSwipeBackHost(
    modifier: Modifier = Modifier,
    edgeWidth: Dp = 28.dp,
    triggerDistance: Dp = 64.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val triggerPx = with(density) { triggerDistance.toPx() }
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val edgeAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .align(edgeAlignment)
                .fillMaxHeight()
                .width(edgeWidth)
                .pointerInput(triggerPx, isRtl, dispatcher) {
                    if (dispatcher == null) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture

                        var totalDrag = 0f
                        val finished = horizontalDrag(drag.id) { change ->
                            val delta = change.positionChange().x
                            totalDrag += if (isRtl) -delta else delta
                            change.consume()
                        }
                        if (finished && totalDrag >= triggerPx) {
                            dispatcher.onBackPressed()
                        }
                    }
                },
        )
    }
}
