package com.perceptiveus.reverie.feature.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.ReverieGlass
import com.perceptiveus.reverie.core.design.ReverieTileShape
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.HomeNowPlayingCard
import com.perceptiveus.reverie.core.design.components.LockedFeatureCard
import com.perceptiveus.reverie.core.design.components.QuickAccessCard
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.core.design.glowBorder
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.feature.library.AddToPlaylistDialog
import com.perceptiveus.reverie.feature.player.QueueSheet
import com.perceptiveus.reverie.feature.premium.UpgradeDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToLibraryPlaylists: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSongDetails: (Track) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSmartPlaylists: () -> Unit,
    onNavigateToAudioFx: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val availablePlaylists by viewModel.availablePlaylists.collectAsState()
    val showFirstRunWelcome by viewModel.showFirstRunWelcome.collectAsState()
    val isPremium = viewModel.isPremium()
    val displayTracks = recentlyPlayed.ifEmpty { songs.take(12) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var upgradeFeature by remember { mutableStateOf<AppFeature?>(null) }

    LaunchedEffect(Unit) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showFirstRunWelcome) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFirstRunWelcome,
            title = { Text("Welcome to Reverie") },
            text = {
                Text(
                    "Your music lives on this device. Import a few songs or a folder, then explore Library, Player, and Discover Reverie anytime from Home.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissFirstRunWelcome()
                        onNavigateToImport()
                    },
                ) {
                    Text("Import music")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissFirstRunWelcome()
                        onNavigateToTutorial()
                    },
                ) {
                    Text("Browse tutorial")
                }
            },
        )
    }

    upgradeFeature?.let { feature ->
        UpgradeDialog(
            feature = feature,
            onDismiss = { upgradeFeature = null },
            onUpgradeClick = {
                upgradeFeature = null
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

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            availablePlaylists = availablePlaylists,
            onDismiss = { showPlaylistDialog = false },
            onAddToPlaylist = { playlist ->
                viewModel.addCurrentTrackToPlaylist(playlist.id)
                showPlaylistDialog = false
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylistAndAddCurrentTrack(name)
                showPlaylistDialog = false
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                HomeHeader(onSearchClick = onNavigateToSearch)
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HomeNowPlayingSection(
                    viewModel = viewModel,
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToSongDetails = onNavigateToSongDetails,
                    onViewQueueClick = { showQueueSheet = true },
                    onAddToPlaylistClick = { showPlaylistDialog = true },
                )
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = if (recentlyPlayed.isNotEmpty()) "Recently Played" else "Your Library",
                    action = {
                        TextButton(onClick = onNavigateToLibrary) {
                            Text(
                                text = "View all ›",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
            item {
                RecentlyPlayedRow(
                    tracks = displayTracks,
                    onTrackClick = viewModel::playTrack,
                )
            }
            item {
                SectionHeader(title = "Quick Access")
            }
            item {
                QuickAccessGrid(
                    isPremium = isPremium,
                    onImportClick = onNavigateToImport,
                    onPlaylistsClick = onNavigateToLibraryPlaylists,
                    onAudioFxClick = {
                        if (isPremium) {
                            onNavigateToAudioFx()
                        } else {
                            upgradeFeature = AppFeature.AUDIO_FX
                        }
                    },
                    onSmartPlaylistsClick = {
                        if (isPremium) {
                            onNavigateToSmartPlaylists()
                        } else {
                            upgradeFeature = AppFeature.SMART_PLAYLISTS
                        }
                    },
                    onStatsClick = {
                        if (isPremium) {
                            onNavigateToStats()
                        } else {
                            upgradeFeature = AppFeature.LIBRARY_STATS
                        }
                    },
                    onTutorialClick = onNavigateToTutorial,
                )
            }
            if (!isPremium) {
                item {
                    UnlockPremiumBanner(onLearnMore = onNavigateToPremium)
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun HomeNowPlayingSection(
    viewModel: HomeViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSongDetails: (Track) -> Unit,
    onViewQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
) {
    val track by remember(viewModel) {
        viewModel.playbackState
            .map { it.currentTrack }
            .distinctUntilChanged { old, new ->
                old?.id == new?.id &&
                    old?.title == new?.title &&
                    old?.artist == new?.artist &&
                    old?.album == new?.album &&
                    old?.artworkPath == new?.artworkPath &&
                    old?.durationMs == new?.durationMs
            }
    }.collectAsState(initial = viewModel.playbackState.value.currentTrack)
    val progress by viewModel.playerProgress.collectAsState()

    HomeNowPlayingCard(
        track = track,
        isPlaying = progress.isPlaying,
        positionMs = progress.positionMs,
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::skipToNext,
        onPrevious = viewModel::skipToPrevious,
        onSeek = viewModel::seekTo,
        onClick = onNavigateToPlayer,
        onSongDetailsClick = onNavigateToSongDetails,
        onViewQueueClick = onViewQueueClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
    )
}

@Composable
private fun HomeHeader(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            RetroScreenTitle(title = "Reverie")
            Text(
                text = "Your music. Your world.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Surface(
            onClick = onSearchClick,
            shape = CircleShape,
            color = ReverieGlass,
            modifier = Modifier
                .size(44.dp)
                .glowBorder(
                    shape = CircleShape,
                    glowRadius = 4.dp,
                    borderAlpha = 0.35f,
                    glowAlpha = 0.15f,
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedRow(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
) {
    if (tracks.isEmpty()) {
        Text(
            text = "Import music to start listening",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        tracks.forEach { track ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .clickable { onTrackClick(track) },
            ) {
                AlbumArt(
                    artworkPath = track.artworkPath,
                    modifier = Modifier.size(88.dp),
                    contentDescription = track.title,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun QuickAccessGrid(
    isPremium: Boolean,
    onImportClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onAudioFxClick: () -> Unit,
    onSmartPlaylistsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onTutorialClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessCard(
                title = "Import Music",
                description = "Add songs or folders",
                icon = Icons.Default.FolderOpen,
                modifier = Modifier.weight(1f),
                onClick = onImportClick,
            )
            QuickAccessCard(
                title = "Playlists",
                description = "Your playlists and mixes",
                icon = Icons.Default.LibraryMusic,
                modifier = Modifier.weight(1f),
                onClick = onPlaylistsClick,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isPremium) {
                QuickAccessCard(
                    title = "Audio FX",
                    description = "EQ, bass, loudness, crossfade",
                    icon = Icons.Default.Equalizer,
                    modifier = Modifier.weight(1f),
                    onClick = onAudioFxClick,
                )
                QuickAccessCard(
                    title = "Smart Playlists",
                    description = "Rule-based auto playlists",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = onSmartPlaylistsClick,
                )
            } else {
                LockedFeatureCard(
                    title = "Audio FX",
                    description = "EQ, bass, loudness, crossfade",
                    icon = Icons.Default.Equalizer,
                    modifier = Modifier.weight(1f),
                    onClick = onAudioFxClick,
                )
                LockedFeatureCard(
                    title = "Smart Playlists",
                    description = "Rule-based auto playlists",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = onSmartPlaylistsClick,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isPremium) {
                QuickAccessCard(
                    title = "Stats",
                    description = "Library insights",
                    icon = Icons.Default.QueryStats,
                    modifier = Modifier.weight(1f),
                    onClick = onStatsClick,
                )
            } else {
                LockedFeatureCard(
                    title = "Stats",
                    description = "Library insights",
                    icon = Icons.Default.QueryStats,
                    modifier = Modifier.weight(1f),
                    onClick = onStatsClick,
                )
            }
            QuickAccessCard(
                title = "Tutorial",
                description = "Learn the ropes",
                icon = Icons.Default.MenuBook,
                modifier = Modifier.weight(1f),
                onClick = onTutorialClick,
            )
        }
    }
}

@Composable
private fun UnlockPremiumBanner(onLearnMore: () -> Unit) {
    Surface(
        onClick = onLearnMore,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .glowBorder(
                shape = ReverieTileShape,
                glowRadius = 6.dp,
                borderAlpha = 0.45f,
                glowAlpha = 0.22f,
            ),
        shape = ReverieTileShape,
        color = ReverieGlass,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock Premium",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Advanced features, visualizers, smart playlists and more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = androidx.compose.ui.graphics.Color.Transparent,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                ),
            ) {
                Text(
                    text = "Learn More",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
