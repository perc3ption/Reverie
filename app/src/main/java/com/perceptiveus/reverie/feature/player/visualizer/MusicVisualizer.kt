package com.perceptiveus.reverie.feature.player.visualizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.ReveriePurple
import com.perceptiveus.reverie.core.design.ReverieTileShape
import com.perceptiveus.reverie.core.design.components.GlassSurface
import com.perceptiveus.reverie.domain.model.PlayerProgress
import com.perceptiveus.reverie.playback.PlaybackAudioAnalyzer
import kotlinx.coroutines.flow.StateFlow

/** Full media-area height when visualizer is the primary pane. */
private val MediaAreaHeight = 220.dp

/** Compact tile height for the spectrum strip under transport. */
private val CompactVisualizerHeight = 112.dp

/**
 * Early-2000s style music visualizer driven by decoded PCM from ExoPlayer.
 *
 * Frame collection and canvas redraw stay inside [VisualizerCanvas] so the glass chrome
 * (style picker, borders) does not recompose at ~30fps.
 *
 * @param compact smaller tile under transport controls (always-visible spectrum).
 */
@Composable
fun MusicVisualizer(
    frameFlow: StateFlow<PlaybackAudioAnalyzer.Frame>,
    playerProgress: StateFlow<PlayerProgress>,
    selectedStyle: VisualizerStyle,
    canAccessPremium: Boolean,
    onStyleSelected: (VisualizerStyle) -> Unit,
    onPremiumStyleLocked: () -> Unit,
    modifier: Modifier = Modifier,
    areaHeight: Dp? = null,
    compact: Boolean = false,
) {
    val tileHeight = areaHeight ?: if (compact) CompactVisualizerHeight else MediaAreaHeight

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight),
        shape = ReverieTileShape,
        emphasized = true,
        glow = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 10.dp,
                    vertical = if (compact) 6.dp else 8.dp,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (compact) {
                    Text(
                        text = selectedStyle.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!canAccessPremium) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable(onClick = onPremiumStyleLocked),
                        ) {
                            Text(
                                text = "Upgrade for more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    } else {
                        StylePickerButton(
                            selected = selectedStyle,
                            canAccessPremium = canAccessPremium,
                            onSelect = onStyleSelected,
                            onPremiumLocked = onPremiumStyleLocked,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    StylePickerButton(
                        selected = selectedStyle,
                        canAccessPremium = canAccessPremium,
                        onSelect = onStyleSelected,
                        onPremiumLocked = onPremiumStyleLocked,
                    )
                }
            }

            VisualizerCanvas(
                style = selectedStyle,
                frameFlow = frameFlow,
                playerProgress = playerProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = if (compact) 4.dp else 8.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun StylePickerButton(
    selected: VisualizerStyle,
    canAccessPremium: Boolean,
    onSelect: (VisualizerStyle) -> Unit,
    onPremiumLocked: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { menuExpanded = true },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Choose visualizer style",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            VisualizerStyle.entries.forEach { style ->
                val locked = style.isPremium && !canAccessPremium
                DropdownMenuItem(
                    text = {
                        Text(
                            text = style.label,
                            color = if (style == selected) {
                                ReveriePurple
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    leadingIcon = if (locked) {
                        {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Premium",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        menuExpanded = false
                        if (locked) onPremiumLocked() else onSelect(style)
                    },
                )
            }
        }
    }
}
