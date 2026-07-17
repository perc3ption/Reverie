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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
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

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                RetroScreenTitle(title = "Now Playing")
            }
            item {
                PlayerMediaDisplay(
                    artworkPath = track?.artworkPath,
                    trackTitle = track?.title,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.artist ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.album ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
        }

        UpNextStrip(
            nextTrack = playbackState.nextTrack,
            queueSize = playbackState.queueSize,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PlayerMediaDisplay(
    artworkPath: String?,
    trackTitle: String?,
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
        Spacer(modifier = Modifier.height(10.dp))

        when (selectedView) {
            PlayerMediaView.ALBUM_ART -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AlbumArt(
                        artworkPath = artworkPath,
                        modifier = Modifier.size(200.dp),
                        contentDescription = trackTitle,
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

/**
 * Compact queue peek under transport controls — next track + remaining count
 * in one glanceable strip so it stays above the fold on typical phones.
 */
@Composable
private fun UpNextStrip(
    nextTrack: Track?,
    queueSize: Int,
    modifier: Modifier = Modifier,
) {
    val remaining = (queueSize - 1).coerceAtLeast(0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nextTrack != null) {
                AlbumArt(
                    artworkPath = nextTrack.artworkPath,
                    modifier = Modifier.size(48.dp),
                    contentDescription = nextTrack.title,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UP NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = nextTrack.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = nextTrack.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "UP NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (queueSize <= 1) "End of queue" else "Nothing queued",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            QueueCountPill(remaining = remaining, hasNext = nextTrack != null)
        }
    }
}

@Composable
private fun QueueCountPill(
    remaining: Int,
    hasNext: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = when {
                    remaining <= 0 && !hasNext -> "1 playing"
                    remaining == 1 -> "1 left"
                    else -> "$remaining left"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
