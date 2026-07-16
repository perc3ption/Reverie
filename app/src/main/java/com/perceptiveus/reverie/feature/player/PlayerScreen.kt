package com.perceptiveus.reverie.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArtPlaceholder
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.feature.premium.UpgradeDialog

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val track = playbackState.currentTrack
    var showUpgradeDialog by remember { mutableStateOf(false) }

    if (showUpgradeDialog) {
        UpgradeDialog(
            feature = AppFeature.ADVANCED_VISUALIZERS,
            onDismiss = { showUpgradeDialog = false },
            onUpgradeClick = {
                showUpgradeDialog = false
                onNavigateToPremium()
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            RetroScreenTitle(title = "Now Playing")
        }
        item {
            // Placeholder for CD jewel-case artwork; replace with loaded album art later.
            AlbumArtPlaceholder(
                modifier = Modifier
                    .size(280.dp)
                    .padding(vertical = 8.dp),
                label = "♪",
            )
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = track?.title ?: "No track",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = track?.artist ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = track?.album ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            PlaybackProgress(
                positionMs = playbackState.positionMs,
                durationMs = track?.durationMs ?: 1L,
                onSeek = viewModel::seekTo,
            )
        }
        item {
            PlaybackControls(
                isPlaying = playbackState.isPlaying,
                shuffleEnabled = playbackState.shuffleEnabled,
                repeatMode = playbackState.repeatMode,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipToNext,
                onPrevious = viewModel::skipToPrevious,
                onShuffle = viewModel::toggleShuffle,
                onRepeat = viewModel::cycleRepeatMode,
            )
        }
        item {
            BasicSpectrumVisualizer()
        }
        item {
            if (!viewModel.canAccessAdvancedVisualizers()) {
                AdvancedVisualizerLockedCard(onClick = { showUpgradeDialog = true })
            }
        }
        item {
            MetadataGrid(
                format = "Local file",
                bitDepth = track?.let { formatMs(it.durationMs) } ?: "—",
                nextUp = playbackState.nextTrack?.title ?: "—",
                queueSize = playbackState.queueSize,
            )
        }
    }
}

@Composable
private fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(positionMs, durationMs, isDragging) {
        if (!isDragging) {
            sliderPosition = (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                isDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek((sliderPosition * safeDuration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val shownPosition = (sliderPosition * safeDuration).toLong()
            Text(formatMs(shownPosition), style = MaterialTheme.typography.bodySmall)
            Text(formatMs(durationMs), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
        }
        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next")
        }
        IconButton(onClick = onRepeat) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne
                else Icons.Default.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BasicSpectrumVisualizer() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BASIC SPECTRUM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Placeholder bars; replace with real audio FFT from ExoPlayer visualizer.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                val barHeights = listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.5f, 0.75f, 0.35f)
                barHeights.forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .fillMaxWidth()
                            .height((64 * fraction).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedVisualizerLockedCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Advanced Visualizers", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Upgrade for waveform, particle, and retro VU modes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MetadataGrid(
    format: String,
    bitDepth: String,
    nextUp: String,
    queueSize: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetadataCard(label = format, modifier = Modifier.weight(1f))
            MetadataCard(label = bitDepth, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetadataCard(label = "Next Up: $nextUp", modifier = Modifier.weight(1f))
            MetadataCard(label = "Queue: $queueSize songs", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetadataCard(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
