package com.perceptiveus.reverie.feature.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.LyricsDocument
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.feature.player.lyrics.LyricsPanel
import com.perceptiveus.reverie.feature.player.visualizer.MusicVisualizer
import com.perceptiveus.reverie.feature.player.visualizer.VisualizerStyle
import com.perceptiveus.reverie.feature.premium.UpgradeDialog
import kotlinx.coroutines.flow.collectLatest

private enum class PlayerMediaView {
    ALBUM_ART,
    VISUALIZER,
    LYRICS,
}

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val track = playbackState.currentTrack
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeFeature by remember { mutableStateOf(AppFeature.ADVANCED_VISUALIZERS) }
    var selectedStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM) }
    var mediaView by rememberSaveable { mutableStateOf(PlayerMediaView.ALBUM_ART) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val canAccessVisualizers = viewModel.canAccessAdvancedVisualizers()
    val canAccessLyrics = viewModel.canAccessLyrics()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickLyricsFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importLyrics(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showUpgradeDialog) {
        UpgradeDialog(
            feature = upgradeFeature,
            onDismiss = { showUpgradeDialog = false },
            onUpgradeClick = {
                showUpgradeDialog = false
                onNavigateToPremium()
            },
        )
    }

    if (showQueueSheet) {
        QueueSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.queueIndex,
            onDismiss = { showQueueSheet = false },
            onTrackSelected = { index ->
                viewModel.playQueueIndex(index)
                showQueueSheet = false
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // Outer app Scaffold already insets for the nav bar; don't add another bottom gap.
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                RetroScreenTitle(
                    title = "Now Playing",
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )

                PlayerMediaDisplay(
                    artworkPath = track?.artworkPath,
                    trackTitle = track?.title,
                    selectedView = mediaView,
                    onViewSelected = { requested ->
                        when {
                            requested == PlayerMediaView.LYRICS && !canAccessLyrics -> {
                                upgradeFeature = AppFeature.LYRICS
                                showUpgradeDialog = true
                                mediaView = PlayerMediaView.LYRICS
                            }
                            else -> mediaView = requested
                        }
                    },
                    audioSessionId = playbackState.audioSessionId,
                    isPlaying = playbackState.isPlaying,
                    positionMs = playbackState.positionMs,
                    selectedStyle = selectedStyle,
                    canAccessVisualizers = canAccessVisualizers,
                    onStyleSelected = { selectedStyle = it },
                    onVisualizerLocked = {
                        upgradeFeature = AppFeature.ADVANCED_VISUALIZERS
                        showUpgradeDialog = true
                    },
                    lyrics = lyrics,
                    canAccessLyrics = canAccessLyrics,
                    canImportLyrics = !track?.filePath.isNullOrBlank(),
                    onLyricsLocked = {
                        upgradeFeature = AppFeature.LYRICS
                        showUpgradeDialog = true
                    },
                    onImportLyrics = {
                        // */* so .lrc shows up — many providers don't map .lrc to text/*
                        pickLyricsFile.launch(arrayOf("*/*"))
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = track?.title ?: "No track",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = track?.artist ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = track?.album ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PlaybackProgress(
                    positionMs = playbackState.positionMs,
                    durationMs = track?.durationMs ?: 1L,
                    onSeek = viewModel::seekTo,
                )
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

            UpNextStrip(
                nextTrack = playbackState.nextTrack,
                queueSize = playbackState.queueSize,
                onClick = { showQueueSheet = true },
            )
        }
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
    canAccessVisualizers: Boolean,
    onStyleSelected: (VisualizerStyle) -> Unit,
    onVisualizerLocked: () -> Unit,
    lyrics: LyricsDocument?,
    canAccessLyrics: Boolean,
    canImportLyrics: Boolean,
    onLyricsLocked: () -> Unit,
    onImportLyrics: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MediaViewToggle(
            selectedView = selectedView,
            onViewSelected = onViewSelected,
            lyricsLocked = !canAccessLyrics,
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
                    canAccessPremium = canAccessVisualizers,
                    onStyleSelected = onStyleSelected,
                    onPremiumStyleLocked = onVisualizerLocked,
                )
            }

            PlayerMediaView.LYRICS -> {
                LyricsPanel(
                    lyrics = lyrics,
                    positionMs = positionMs,
                    hasAccess = canAccessLyrics,
                    canImport = canImportLyrics,
                    onLockedClick = onLyricsLocked,
                    onImportClick = onImportLyrics,
                )
            }
        }
    }
}

@Composable
private fun MediaViewToggle(
    selectedView: PlayerMediaView,
    onViewSelected: (PlayerMediaView) -> Unit,
    lyricsLocked: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MediaViewToggleButton(
                label = "ALBUM ART",
                selected = selectedView == PlayerMediaView.ALBUM_ART,
                onClick = { onViewSelected(PlayerMediaView.ALBUM_ART) },
                modifier = Modifier.weight(1f),
            )
            MediaViewToggleButton(
                label = "VISUALIZER",
                selected = selectedView == PlayerMediaView.VISUALIZER,
                onClick = { onViewSelected(PlayerMediaView.VISUALIZER) },
                modifier = Modifier.weight(1f),
            )
            MediaViewToggleButton(
                label = "LYRICS",
                selected = selectedView == PlayerMediaView.LYRICS,
                showLock = lyricsLocked,
                onClick = { onViewSelected(PlayerMediaView.LYRICS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MediaViewToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLock: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
            if (showLock) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
            }
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

/**
 * Queue peek pinned just above the bottom nav — full-bleed like the mini player.
 * Tap to open the full queue list.
 */
@Composable
private fun UpNextStrip(
    nextTrack: Track?,
    queueSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = (queueSize - 1).coerceAtLeast(0)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nextTrack != null) {
                AlbumArt(
                    artworkPath = nextTrack.artworkPath,
                    modifier = Modifier.size(52.dp),
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
