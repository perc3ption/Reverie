package com.perceptiveus.reverie.feature.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.ReverieArtShape
import com.perceptiveus.reverie.core.design.ReverieGlass
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.GlassSurface
import com.perceptiveus.reverie.core.design.components.PremiumBadge
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.formatArtistAlbum
import com.perceptiveus.reverie.core.design.glowBorder
import com.perceptiveus.reverie.core.design.glowRing
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.PlayerProgress
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.feature.player.lyrics.LyricsPanel
import com.perceptiveus.reverie.feature.player.visualizer.MusicVisualizer
import com.perceptiveus.reverie.feature.player.visualizer.VisualizerStyle
import com.perceptiveus.reverie.feature.premium.UpgradeDialog
import com.perceptiveus.reverie.playback.PlaybackAudioAnalyzer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

private val PlayerMediaHeight = 218.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToPremium: () -> Unit,
    onNavigateToSongDetails: (Track) -> Unit,
    onNavigateToAudioFx: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val track = playbackState.currentTrack
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeFeature by remember { mutableStateOf(AppFeature.ADVANCED_VISUALIZERS) }
    var selectedStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM) }
    var showVisualizer by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val lyricsBringIntoView = remember { BringIntoViewRequester() }
    val sleepTimerEndMs by viewModel.sleepTimerEndMs.collectAsState()
    var sleepTimerNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val canAccessVisualizers = viewModel.canAccessAdvancedVisualizers()
    val canAccessLyrics = viewModel.canAccessLyrics()
    val canAccessAlbumArt = viewModel.canAccessAlbumArtEditing()
    val canAccessAudioFx = viewModel.canAccessAudioFx()
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

    LaunchedEffect(playbackState.errorMessage) {
        val message = playbackState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
    }

    LaunchedEffect(sleepTimerEndMs) {
        while (sleepTimerEndMs != null) {
            sleepTimerNowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    // One-shot scroll when lyrics open — no ongoing cost while the panel stays visible.
    LaunchedEffect(showLyrics) {
        if (showLyrics) {
            withFrameNanos { } // wait one frame so the panel is laid out
            lyricsBringIntoView.bringIntoView()
        }
    }

    val sleepTimerRemainingMs = viewModel.sleepTimerRemainingMs(sleepTimerNowMs)
    val sleepTimerActive = sleepTimerEndMs != null && sleepTimerRemainingMs > 0L

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

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = sleepTimerEndMs?.let { viewModel.sleepTimerRemainingMs(sleepTimerNowMs) },
            onDismiss = { showSleepTimerDialog = false },
            onStart = viewModel::startSleepTimer,
            onCancel = viewModel::cancelSleepTimer,
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
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    RetroScreenTitle(title = "Now Playing")
                    QueueSourceLabel(
                        source = playbackState.queueSource,
                        queueSize = playbackState.queueSize,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                PlayerMediaPane(
                    showVisualizer = showVisualizer,
                    artworkPath = track?.artworkPath,
                    trackTitle = track?.title,
                    visualizerFrame = viewModel.visualizerFrame,
                    playerProgress = viewModel.playerProgress,
                    selectedStyle = selectedStyle,
                    canAccessVisualizers = canAccessVisualizers,
                    onStyleSelected = { selectedStyle = it },
                    onVisualizerLocked = {
                        upgradeFeature = AppFeature.ADVANCED_VISUALIZERS
                        showUpgradeDialog = true
                    },
                    canAccessAlbumArt = canAccessAlbumArt,
                    canImportAlbumArt = track != null,
                    onAlbumArtLocked = {
                        upgradeFeature = AppFeature.ALBUM_ART_EDITING
                        showUpgradeDialog = true
                    },
                    onImportAlbumArt = { pickAlbumArt.launch(arrayOf("image/*")) },
                )

                Spacer(modifier = Modifier.height(18.dp))

                PlayerTrackInfo(
                    track = track,
                    onTitleClick = { track?.let(onNavigateToSongDetails) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                PlayerTransportControls(
                    playerProgress = viewModel.playerProgress,
                    durationMs = track?.durationMs ?: 1L,
                    shuffleEnabled = playbackState.shuffleEnabled,
                    repeatMode = playbackState.repeatMode,
                    onSeek = viewModel::seekTo,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::skipToNext,
                    onPrevious = viewModel::skipToPrevious,
                    onShuffle = viewModel::toggleShuffle,
                    onRepeat = viewModel::cycleRepeatMode,
                )

                Spacer(modifier = Modifier.height(14.dp))

                PlayerActionRow(
                    lyricsSelected = showLyrics,
                    visualizerSelected = showVisualizer,
                    sleepSelected = sleepTimerActive,
                    lyricsLocked = !canAccessLyrics,
                    audioFxLocked = !canAccessAudioFx,
                    onLyricsClick = {
                        if (!canAccessLyrics) {
                            upgradeFeature = AppFeature.LYRICS
                            showUpgradeDialog = true
                        }
                        showLyrics = !showLyrics
                    },
                    onVisualizerClick = { showVisualizer = !showVisualizer },
                    onSleepClick = { showSleepTimerDialog = true },
                    onEqualizerClick = {
                        if (canAccessAudioFx) {
                            onNavigateToAudioFx()
                        } else {
                            upgradeFeature = AppFeature.AUDIO_FX
                            showUpgradeDialog = true
                        }
                    },
                )

                if (showLyrics) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val progress by viewModel.playerProgress.collectAsState()
                    LyricsPanel(
                        lyrics = lyrics,
                        positionMs = progress.positionMs,
                        hasAccess = canAccessLyrics,
                        canImport = !track?.filePath.isNullOrBlank(),
                        onLockedClick = {
                            upgradeFeature = AppFeature.LYRICS
                            showUpgradeDialog = true
                        },
                        onImportClick = { pickLyricsFile.launch(arrayOf("*/*")) },
                        onDismiss = { showLyrics = false },
                        modifier = Modifier.bringIntoViewRequester(lyricsBringIntoView),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            UpNextStrip(
                nextTrack = playbackState.nextTrack,
                queueSize = playbackState.queueSize,
                onClick = { showQueueSheet = true },
                modifier = Modifier.align(Alignment.BottomCenter),
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
        is QueueSource.SmartPlaylist -> "Smart playlist · ${source.name}"
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
private fun PlayerTrackInfo(
    track: Track?,
    onTitleClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = track?.title ?: "No track",
            style = MaterialTheme.typography.headlineSmall.copy(
                textDecoration = if (track != null) TextDecoration.Underline else TextDecoration.None,
            ),
            color = if (track != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (track != null) {
                        Modifier.clickable(
                            onClick = onTitleClick,
                            onClickLabel = "Open song details",
                        )
                    } else {
                        Modifier
                    },
                ),
        )
        Text(
            text = formatArtistAlbum(track?.artist, track?.album),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlayerTransportControls(
    playerProgress: StateFlow<PlayerProgress>,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    val progress by playerProgress.collectAsState()
    PlaybackProgress(
        positionMs = progress.positionMs,
        durationMs = durationMs,
        onSeek = onSeek,
    )
    PlaybackControls(
        isPlaying = progress.isPlaying,
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
        onPlayPause = onPlayPause,
        onNext = onNext,
        onPrevious = onPrevious,
        onShuffle = onShuffle,
        onRepeat = onRepeat,
    )
}

@Composable
private fun PlayerMediaPane(
    showVisualizer: Boolean,
    artworkPath: String?,
    trackTitle: String?,
    visualizerFrame: StateFlow<PlaybackAudioAnalyzer.Frame>,
    playerProgress: StateFlow<PlayerProgress>,
    selectedStyle: VisualizerStyle,
    canAccessVisualizers: Boolean,
    onStyleSelected: (VisualizerStyle) -> Unit,
    onVisualizerLocked: () -> Unit,
    canAccessAlbumArt: Boolean,
    canImportAlbumArt: Boolean,
    onAlbumArtLocked: () -> Unit,
    onImportAlbumArt: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerMediaHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (showVisualizer) {
            MusicVisualizer(
                frameFlow = visualizerFrame,
                playerProgress = playerProgress,
                selectedStyle = selectedStyle,
                canAccessPremium = canAccessVisualizers,
                onStyleSelected = onStyleSelected,
                onPremiumStyleLocked = onVisualizerLocked,
                areaHeight = PlayerMediaHeight,
            )
        } else {
            PlayerAlbumArtContent(
                artworkPath = artworkPath,
                trackTitle = trackTitle,
                canAccess = canAccessAlbumArt,
                canImport = canImportAlbumArt,
                onLockedClick = onAlbumArtLocked,
                onImportClick = onImportAlbumArt,
            )
        }
    }
}

@Composable
private fun PlayerAlbumArtContent(
    artworkPath: String?,
    trackTitle: String?,
    canAccess: Boolean,
    canImport: Boolean,
    onLockedClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val hasArt = !artworkPath.isNullOrBlank()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Subtle CD disc peeking behind the cover — offset within the group so art stays centered.
            Box(
                modifier = Modifier
                    .offset(x = 10.dp)
                    .size(184.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1E))
                    .glowBorder(
                        shape = CircleShape,
                        glowRadius = 8.dp,
                        borderAlpha = 0.45f,
                        glowAlpha = 0.22f,
                    ),
            )
            if (!hasArt && canImport) {
                GlassSurface(
                    onClick = {
                        if (canAccess) onImportClick() else onLockedClick()
                    },
                    modifier = Modifier.size(200.dp),
                    shape = ReverieArtShape,
                    emphasized = true,
                    glow = true,
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
                    modifier = Modifier
                        .size(200.dp)
                        .glowBorder(
                            shape = ReverieArtShape,
                            glowRadius = 12.dp,
                            borderAlpha = 0.75f,
                            glowAlpha = 0.45f,
                        ),
                    contentDescription = trackTitle,
                )
            }
        }
    }
}

@Composable
private fun PlayerActionRow(
    lyricsSelected: Boolean,
    visualizerSelected: Boolean,
    sleepSelected: Boolean,
    lyricsLocked: Boolean,
    audioFxLocked: Boolean,
    onLyricsClick: () -> Unit,
    onVisualizerClick: () -> Unit,
    onSleepClick: () -> Unit,
    onEqualizerClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PlayerActionButton(
            icon = Icons.Default.MenuBook,
            label = "LYRICS",
            selected = lyricsSelected,
            locked = lyricsLocked,
            onClick = onLyricsClick,
        )
        PlayerActionButton(
            icon = Icons.Default.GraphicEq,
            label = if (visualizerSelected) "ART" else "VISUALIZER",
            selected = visualizerSelected,
            onClick = onVisualizerClick,
        )
        PlayerActionButton(
            icon = Icons.Default.Bedtime,
            label = "SLEEP",
            selected = sleepSelected,
            onClick = onSleepClick,
        )
        PlayerActionButton(
            icon = Icons.Default.Equalizer,
            label = "EQUALIZER",
            locked = audioFxLocked,
            onClick = onEqualizerClick,
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    locked: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                ReverieGlass
            },
            modifier = Modifier
                .size(52.dp)
                .glowBorder(
                    shape = CircleShape,
                    glowRadius = if (selected) 6.dp else 3.dp,
                    borderAlpha = if (selected) 0.7f else 0.35f,
                    glowAlpha = if (selected) 0.28f else 0.12f,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(22.dp),
                )
                if (locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(10.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
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
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
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
            Text(
                formatMs(shownPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatMs(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier
                .size(56.dp)
                .glowRing(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface,
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

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
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

    GlassSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        emphasized = true,
        glow = true,
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
                        text = formatArtistAlbum(nextTrack.artist, nextTrack.album),
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
