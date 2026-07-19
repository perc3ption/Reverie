package com.perceptiveus.reverie.feature.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.PremiumBadge
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.LyricsDocument
import com.perceptiveus.reverie.domain.model.QueueSource
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
    onNavigateToSongDetails: (Track) -> Unit,
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
    val canAccessAlbumArt = viewModel.canAccessAlbumArtEditing()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickLyricsFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importLyrics(uri)
    }
    val pickAlbumArt = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importAlbumArt(uri)
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
            queueSource = playbackState.queueSource,
            disabledTrackIds = playbackState.disabledTrackIds,
            onDismiss = { showQueueSheet = false },
            onTrackSelected = { index ->
                viewModel.playQueueIndex(index)
                showQueueSheet = false
            },
            onToggleTrackEnabled = viewModel::toggleQueueTrackEnabled,
            onMoveTrack = viewModel::moveQueueItem,
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
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
                    RetroScreenTitle(title = "Now Playing")
                    QueueSourceLabel(
                        source = playbackState.queueSource,
                        queueSize = playbackState.queueSize,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

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
                    canAccessAlbumArt = canAccessAlbumArt,
                    canImportAlbumArt = track != null,
                    onAlbumArtLocked = {
                        upgradeFeature = AppFeature.ALBUM_ART_EDITING
                        showUpgradeDialog = true
                    },
                    onImportAlbumArt = {
                        pickAlbumArt.launch(arrayOf("image/*"))
                    },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (track != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = { onNavigateToSongDetails(track) },
                                    onClickLabel = "Open song details",
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        Text(
                            text = "No track",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = track?.artist ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Spacer(modifier = Modifier.height(8.dp))
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
private fun QueueSourceLabel(
    source: QueueSource,
    queueSize: Int,
    modifier: Modifier = Modifier,
) {
    if (source is QueueSource.Unknown && queueSize <= 0) return

    val title = when (source) {
        QueueSource.Library -> "Playing all songs"
        is QueueSource.Playlist -> "Playlist · ${source.name}"
        is QueueSource.Album -> buildString {
            append("Album · ${source.title}")
            if (source.year > 0) append(" · ${source.year}")
        }
        is QueueSource.Artist -> "Artist · ${source.name}"
        is QueueSource.Folder -> "Folder · ${source.name}"
        QueueSource.RecentlyPlayed -> "Recently played"
        QueueSource.Unknown -> if (queueSize > 0) "Queue" else return
    }
    val subtitle = when (source) {
        is QueueSource.Playlist -> source.description.trim().takeIf { it.isNotEmpty() }
        is QueueSource.Album -> source.artist.takeIf { it.isNotBlank() }
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    canAccessAlbumArt: Boolean,
    canImportAlbumArt: Boolean,
    onAlbumArtLocked: () -> Unit,
    onImportAlbumArt: () -> Unit,
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
                PlayerAlbumArtPane(
                    artworkPath = artworkPath,
                    trackTitle = trackTitle,
                    canAccess = canAccessAlbumArt,
                    canImport = canImportAlbumArt,
                    onLockedClick = onAlbumArtLocked,
                    onImportClick = onImportAlbumArt,
                )
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
private fun PlayerAlbumArtPane(
    artworkPath: String?,
    trackTitle: String?,
    canAccess: Boolean,
    canImport: Boolean,
    onLockedClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val hasArt = !artworkPath.isNullOrBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasArt && canImport) {
            Surface(
                onClick = {
                    if (canAccess) onImportClick() else onLockedClick()
                },
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import album art",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = "Import album art",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    if (!canAccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PremiumBadge()
                    }
                }
            }
        } else {
            AlbumArt(
                artworkPath = artworkPath,
                modifier = Modifier.size(200.dp),
                contentDescription = trackTitle,
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

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
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(12.dp, 12.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(3.dp),
                    thumbTrackGapSize = 0.dp,
                    drawStopIndicator = null,
                )
            },
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
        IconButton(onClick = onShuffle, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                modifier = Modifier.size(22.dp),
                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(26.dp),
            )
        }
        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(26.dp),
            )
        }
        IconButton(onClick = onRepeat, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne
                else Icons.Default.Repeat,
                contentDescription = "Repeat",
                modifier = Modifier.size(22.dp),
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
