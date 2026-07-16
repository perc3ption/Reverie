package com.perceptiveus.reverie.feature.player.visualizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.ReveriePurple

/**
 * Early-2000s style music visualizer with selectable skins.
 */
@Composable
fun MusicVisualizer(
    audioSessionId: Int,
    isPlaying: Boolean,
    positionMs: Long,
    selectedStyle: VisualizerStyle,
    canAccessPremium: Boolean,
    onStyleSelected: (VisualizerStyle) -> Unit,
    onPremiumStyleLocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var spectrum by remember {
        mutableStateOf(FloatArray(AudioSpectrumCollector.DEFAULT_BAR_COUNT))
    }
    var peaks by remember {
        mutableStateOf(FloatArray(AudioSpectrumCollector.DEFAULT_BAR_COUNT))
    }
    var waveform by remember {
        mutableStateOf(FloatArray(AudioSpectrumCollector.WAVEFORM_POINTS))
    }

    val collector = remember {
        AudioSpectrumCollector { spectrumFrame, peaksFrame, waveformFrame ->
            spectrum = spectrumFrame
            peaks = peaksFrame
            waveform = waveformFrame
        }
    }

    DisposableEffect(Unit) {
        collector.start()
        onDispose { collector.stop() }
    }

    SideEffect {
        collector.updatePlayback(audioSessionId, isPlaying, positionMs)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "VISUALIZER",
                    style = MaterialTheme.typography.labelLarge,
                    color = ReveriePurple,
                )
                Text(
                    text = selectedStyle.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            StylePickerRow(
                selected = selectedStyle,
                canAccessPremium = canAccessPremium,
                onSelect = onStyleSelected,
                onPremiumLocked = onPremiumStyleLocked,
            )

            Spacer(modifier = Modifier.height(10.dp))

            VisualizerCanvas(
                style = selectedStyle,
                spectrum = spectrum,
                peaks = peaks,
                waveform = waveform,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun StylePickerRow(
    selected: VisualizerStyle,
    canAccessPremium: Boolean,
    onSelect: (VisualizerStyle) -> Unit,
    onPremiumLocked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VisualizerStyle.entries.forEach { style ->
            val locked = style.isPremium && !canAccessPremium
            FilterChip(
                selected = selected == style,
                onClick = {
                    if (locked) onPremiumLocked() else onSelect(style)
                },
                label = {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = if (locked) {
                    {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Premium",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ReveriePurple.copy(alpha = 0.25f),
                    selectedLabelColor = ReveriePurple,
                    selectedLeadingIconColor = ReveriePurple,
                ),
            )
        }
    }
}
