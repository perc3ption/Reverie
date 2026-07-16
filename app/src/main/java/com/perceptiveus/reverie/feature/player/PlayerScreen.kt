package com.perceptiveus.reverie.feature.player

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArtPlaceholder
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.feature.player.visualizer.MusicVisualizer
import com.perceptiveus.reverie.feature.player.visualizer.VisualizerStyle
import com.perceptiveus.reverie.feature.premium.UpgradeDialog

private enum class PlayerMediaView {
    ALBUM_ART,
    VISUALIZER,
}

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val track = playbackState.currentTrack
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var selectedStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM) }
    var mediaView by rememberSaveable { mutableStateOf(PlayerMediaView.ALBUM_ART) }
    val canAccessPremium = viewModel.canAccessAdvancedVisualizers()

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
            PlayerMediaDisplay(
                selectedView = mediaView,
                onViewSelected = { mediaView = it },
                audioSessionId = playbackState.audioSessionId,
                isPlaying = playbackState.isPlaying,
                positionMs = playbackState.positionMs,
                selectedStyle = selectedStyle,
                canAccessPremium = canAccessPremium,
                onStyleSelected = { selectedStyle = it },
                onPremiumStyleLocked = { showUpgradeDialog = true },
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
private fun PlayerMediaDisplay(
    selectedView: PlayerMediaView,
    onViewSelected: (PlayerMediaView) -> Unit,
    audioSessionId: Int,
    isPlaying: Boolean,
    positionMs: Long,
    selectedStyle: VisualizerStyle,
    canAccessPremium: Boolean,
    onStyleSelected: (VisualizerStyle) -> Unit,
    onPremiumStyleLocked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MediaViewToggle(
            selectedView = selectedView,
            onViewSelected = onViewSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (selectedView) {
            PlayerMediaView.ALBUM_ART -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(242.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AlbumArtPlaceholder(
                        modifier = Modifier.size(220.dp),
                        label = "♪",
                    )
                }
            }

            PlayerMediaView.VISUALIZER -> {
                MusicVisualizer(
                    audioSessionId = audioSessionId,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    selectedStyle = selectedStyle,
                    canAccessPremium = canAccessPremium,
                    onStyleSelected = onStyleSelected,
                    onPremiumStyleLocked = onPremiumStyleLocked,
                )
            }
        }
    }
}

@Composable
private fun MediaViewToggle(
    selectedView: PlayerMediaView,
    onViewSelected: (PlayerMediaView) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            MediaViewToggleButton(
                label = "ALBUM ART",
                selected = selectedView == PlayerMediaView.ALBUM_ART,
                onClick = { onViewSelected(PlayerMediaView.ALBUM_ART) },
            )
            Spacer(modifier = Modifier.width(4.dp))
            MediaViewToggleButton(
                label = "VISUALIZER",
                selected = selectedView == PlayerMediaView.VISUALIZER,
                onClick = { onViewSelected(PlayerMediaView.VISUALIZER) },
            )
        }
    }
}

@Composable
private fun MediaViewToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
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
