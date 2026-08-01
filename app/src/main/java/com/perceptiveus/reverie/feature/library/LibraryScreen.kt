package com.perceptiveus.reverie.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DisabledByDefault
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.reverieGlassColor
import com.perceptiveus.reverie.core.design.ReverieTileShape
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.GlassSurface
import com.perceptiveus.reverie.core.design.components.LockedFeatureCard
import com.perceptiveus.reverie.core.design.components.QuickAccessCard
import com.perceptiveus.reverie.core.design.components.ReverieScreenHeader
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.SmartPlaylist
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.feature.premium.UpgradeDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPremiumFeatureClick: () -> Unit,
    onSongDetailsClick: (Track) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSmartPlaylistClick: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSmartPlaylists: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToAudioFx: () -> Unit,
    requestedTab: LibraryTab? = null,
    onRequestedTabConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.FOLDERS) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var lockedFeature by remember { mutableStateOf<AppFeature?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistPendingDelete by remember { mutableStateOf<Playlist?>(null) }
    var smartPlaylistPendingDelete by remember { mutableStateOf<SmartPlaylist?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveDestinationDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestedTab) {
        val tab = requestedTab ?: return@LaunchedEffect
        selectedTab = tab
        onRequestedTabConsumed()
    }

    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val smartPlaylists by viewModel.smartPlaylists.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val folderBrowser by viewModel.folderBrowser.collectAsState()
    val artistBrowser by viewModel.artistBrowser.collectAsState()
    val albumBrowser by viewModel.albumBrowser.collectAsState()
    val showAllSongs by viewModel.showAllSongs.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsState()
    val selectedFolderPaths by viewModel.selectedFolderPaths.collectAsState()
    val selectionCount by viewModel.selectionCount.collectAsState()
    val bulkDeleteInProgress by viewModel.bulkDeleteInProgress.collectAsState()
    val isPremium = viewModel.isPremium()

    val libraryCanGoBack = selectionMode ||
        showAllSongs ||
        artistBrowser.selectedArtist != null ||
        albumBrowser.selectedAlbum != null ||
        folderBrowser.canNavigateUp

    BackHandler(enabled = libraryCanGoBack) {
        viewModel.handleLibraryBack()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != LibraryTab.FOLDERS) {
            viewModel.clearFolderSelection()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showUpgradeDialog && lockedFeature != null) {
        UpgradeDialog(
            feature = lockedFeature,
            onDismiss = { showUpgradeDialog = false },
            onUpgradeClick = {
                showUpgradeDialog = false
                onPremiumFeatureClick()
            },
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
        )
    }

    playlistPendingDelete?.let { playlist ->
        ConfirmDeletePlaylistDialog(
            playlistName = playlist.name,
            onDismiss = { playlistPendingDelete = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist)
                playlistPendingDelete = null
            },
        )
    }

    smartPlaylistPendingDelete?.let { playlist ->
        ConfirmDeletePlaylistDialog(
            playlistName = playlist.name,
            title = "Delete smart playlist?",
            body = "\"${playlist.name}\" will be permanently deleted. Songs in your library are not removed.",
            onDismiss = { smartPlaylistPendingDelete = null },
            onConfirm = {
                viewModel.deleteSmartPlaylist(playlist)
                smartPlaylistPendingDelete = null
            },
        )
    }

    if (showBulkDeleteConfirm) {
        val folderCount = selectedFolderPaths.size
        val songCount = selectedTrackIds.size
        val summary = buildString {
            if (songCount > 0) append(if (songCount == 1) "1 song" else "$songCount songs")
            if (songCount > 0 && folderCount > 0) append(" and ")
            if (folderCount > 0) {
                append(if (folderCount == 1) "1 folder" else "$folderCount folders")
                append(" (including songs inside)")
            }
        }
        ConfirmDeletePlaylistDialog(
            playlistName = summary,
            title = "Delete from library?",
            body = "$summary will be permanently removed from your Reverie library folder and cannot be undone.",
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirm = {
                showBulkDeleteConfirm = false
                viewModel.deleteSelectedLibraryItems()
            },
        )
    }

    if (showMoveDestinationDialog) {
        MoveDestinationDialog(
            destinations = viewModel.moveDestinations(),
            onDismiss = { showMoveDestinationDialog = false },
            onConfirm = { destination ->
                showMoveDestinationDialog = false
                viewModel.moveSelectedLibraryItems(destination)
            },
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                showCreateFolderDialog = false
                viewModel.createFolderInCurrentLocation(name)
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        // Outer app Scaffold already applies system insets; don't add a second top gap.
        contentWindowInsets = WindowInsets(0.dp),
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            LibraryTopBar(onSearchClick = onNavigateToSearch)
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    LibraryTab.FOLDERS -> "Folders"
                                    LibraryTab.PLAYLISTS -> "Playlists"
                                    LibraryTab.ARTISTS -> "Artists"
                                    LibraryTab.ALBUMS -> "Albums"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (selectedTab) {
                    LibraryTab.PLAYLISTS -> {
                        if (showAllSongs) {
                            item {
                                AllSongsHeader(
                                    songCount = songs.size,
                                    onNavigateBack = viewModel::closeAllSongs,
                                    onPlayAll = {
                                        viewModel.playAllSongs()
                                        onNavigateToPlayer()
                                    },
                                )
                            }
                            if (songs.isEmpty()) {
                                item {
                                    Text(
                                        text = "No songs yet. Import music to build your library.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            } else {
                                items(songs, key = { "all-${it.id}" }) { track ->
                                    SongListItem(
                                        track = track,
                                        onClick = { viewModel.playSong(track) },
                                        onDetailsClick = { onSongDetailsClick(track) },
                                        onAddToQueue = { viewModel.addToQueue(track) },
                                    )
                                }
                            }
                        } else {
                            item {
                                SectionHeader(title = "Library")
                            }
                            item {
                                AllSongsCard(
                                    songCount = songs.size,
                                    onOpen = viewModel::openAllSongs,
                                    onPlayAll = {
                                        viewModel.playAllSongs()
                                        onNavigateToPlayer()
                                    },
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                PlaylistsSectionHeader(
                                    onCreateClick = { showCreatePlaylistDialog = true },
                                )
                            }
                            if (playlists.isEmpty() && smartPlaylists.isEmpty()) {
                                item {
                                    Text(
                                        text = "No playlists yet. Tap + to create one.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            } else {
                                if (playlists.isNotEmpty()) {
                                    items(playlists, key = { "playlist-${it.id}" }) { playlist ->
                                        PlaylistListItem(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist) },
                                            onPlayClick = {
                                                viewModel.playPlaylist(playlist)
                                                if (playlist.trackCount > 0) {
                                                    onNavigateToPlayer()
                                                }
                                            },
                                            onDeleteClick = { playlistPendingDelete = playlist },
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = "No manual playlists yet. Tap + to create one.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                                if (smartPlaylists.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Smart Playlists",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        )
                                    }
                                    items(smartPlaylists, key = { "smart-${it.id}" }) { smart ->
                                        SmartPlaylistListItem(
                                            playlist = smart,
                                            onClick = { onSmartPlaylistClick(smart.id) },
                                            onPlayClick = {
                                                viewModel.playSmartPlaylist(smart) {
                                                    onNavigateToPlayer()
                                                }
                                            },
                                            onDeleteClick = { smartPlaylistPendingDelete = smart },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    LibraryTab.FOLDERS -> {
                        item {
                            FolderBrowserHeader(
                                breadcrumb = folderBrowser.breadcrumb,
                                canNavigateUp = folderBrowser.canNavigateUp,
                                subtreeSongCount = folderBrowser.subtreeSongs.size,
                                selectionMode = selectionMode,
                                selectionCount = selectionCount,
                                actionsEnabled = !bulkDeleteInProgress,
                                hasSelection = selectionCount > 0,
                                onNavigateUp = viewModel::navigateFolderUp,
                                onPlayAll = {
                                    if (viewModel.playAllInCurrentFolder()) {
                                        onNavigateToPlayer()
                                    }
                                },
                                onSelectAll = viewModel::selectAllInCurrentFolder,
                                onClearSelection = viewModel::clearFolderSelection,
                                onDeleteClick = {
                                    if (selectionCount == 0) {
                                        viewModel.enterFolderSelectionMode()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Select songs or folders, then choose Delete again.",
                                            )
                                        }
                                    } else {
                                        showBulkDeleteConfirm = true
                                    }
                                },
                                onMoveClick = {
                                    if (selectionCount == 0) {
                                        viewModel.enterFolderSelectionMode()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Select songs or folders, then choose Move again.",
                                            )
                                        }
                                    } else {
                                        showMoveDestinationDialog = true
                                    }
                                },
                                onCreateFolderClick = { showCreateFolderDialog = true },
                            )
                        }
                    if (folderBrowser.childFolders.isEmpty() && folderBrowser.songs.isEmpty()) {
                        item {
                            Text(
                                text = if (folderBrowser.path.isEmpty()) {
                                    "No folders yet. Import a folder to build your library tree."
                                } else {
                                    "This folder is empty."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        items(
                            folderBrowser.childFolders,
                            key = { "folder-${it.id}" },
                        ) { folder ->
                            FolderListItem(
                                folder = folder,
                                selected = folder.relativePath in selectedFolderPaths,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        viewModel.toggleFolderSelected(folder.relativePath)
                                    } else {
                                        viewModel.openFolder(folder.relativePath)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleFolderSelected(folder.relativePath)
                                },
                            )
                        }
                        items(
                            folderBrowser.songs,
                            key = { "song-${it.id}" },
                        ) { track ->
                            SongListItem(
                                track = track,
                                selected = track.id in selectedTrackIds,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        viewModel.toggleTrackSelected(track.id)
                                    } else {
                                        viewModel.playSongInFolder(track)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleTrackSelected(track.id)
                                },
                                onDetailsClick = { onSongDetailsClick(track) },
                                onAddToQueue = { viewModel.addToQueue(track) },
                            )
                        }
                    }
                }
                LibraryTab.ARTISTS -> {
                    val selectedArtist = artistBrowser.selectedArtist
                    if (selectedArtist != null) {
                        item {
                            ArtistBrowserHeader(
                                artistName = selectedArtist,
                                songCount = artistBrowser.songs.size,
                                onNavigateBack = viewModel::clearSelectedArtist,
                                onPlayAll = {
                                    if (viewModel.playAllArtistSongs()) {
                                        onNavigateToPlayer()
                                    }
                                },
                            )
                        }
                        if (artistBrowser.songs.isEmpty()) {
                            item {
                                Text(
                                    text = "No songs found for this artist.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        } else {
                            items(
                                artistBrowser.songs,
                                key = { "artist-song-${it.id}" },
                            ) { track ->
                                SongListItem(
                                    track = track,
                                    onClick = { viewModel.playSongByArtist(track) },
                                    onDetailsClick = { onSongDetailsClick(track) },
                                    onAddToQueue = { viewModel.addToQueue(track) },
                                )
                            }
                        }
                    } else if (artists.isEmpty()) {
                        item {
                            Text(
                                text = "No artists yet. Import music to build your library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        items(artists, key = { "artist-${it.id}" }) { artist ->
                            ArtistListItem(
                                artist = artist,
                                onClick = { viewModel.openArtist(artist.name) },
                                onPlayAll = {
                                    if (viewModel.playAllForArtist(artist.name)) {
                                        onNavigateToPlayer()
                                    }
                                },
                            )
                        }
                    }
                }
                LibraryTab.ALBUMS -> {
                    val selectedAlbum = albumBrowser.selectedAlbum
                    if (selectedAlbum != null) {
                        item {
                            AlbumBrowserHeader(
                                album = selectedAlbum,
                                songCount = albumBrowser.songs.size,
                                onNavigateBack = viewModel::clearSelectedAlbum,
                                onPlayAll = {
                                    if (viewModel.playAllAlbumSongs()) {
                                        onNavigateToPlayer()
                                    }
                                },
                            )
                        }
                        if (albumBrowser.songs.isEmpty()) {
                            item {
                                Text(
                                    text = "No songs found for this album.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        } else {
                            items(
                                albumBrowser.songs,
                                key = { "album-song-${it.id}" },
                            ) { track ->
                                SongListItem(
                                    track = track,
                                    onClick = { viewModel.playSongInAlbum(track) },
                                    onDetailsClick = { onSongDetailsClick(track) },
                                    onAddToQueue = { viewModel.addToQueue(track) },
                                )
                            }
                        }
                    } else if (albums.isEmpty()) {
                        item {
                            Text(
                                text = "No albums yet. Import music to build your library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else {
                        items(albums, key = { "album-${it.id}" }) { album ->
                            AlbumListItem(
                                album = album,
                                onClick = { viewModel.openAlbum(album) },
                                onPlayAll = {
                                    if (viewModel.playAllForAlbum(album)) {
                                        onNavigateToPlayer()
                                    }
                                },
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Quick Access")
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAccessCard(
                            title = "Import Music",
                            description = "Add songs or folders",
                            icon = Icons.Default.FolderOpen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToImport,
                        )
                        if (isPremium) {
                            QuickAccessCard(
                                title = "Audio FX",
                                description = "EQ, bass, loudness, crossfade",
                                icon = Icons.Default.Equalizer,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToAudioFx,
                            )
                        } else {
                            LockedFeatureCard(
                                title = "Audio FX",
                                description = "EQ, bass, loudness, crossfade",
                                icon = Icons.Default.Equalizer,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    lockedFeature = AppFeature.AUDIO_FX
                                    showUpgradeDialog = true
                                },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isPremium) {
                            QuickAccessCard(
                                title = "Smart Playlists",
                                description = "Rule-based auto playlists",
                                icon = Icons.Default.AutoAwesome,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToSmartPlaylists,
                            )
                            QuickAccessCard(
                                title = "Stats",
                                description = "Library insights",
                                icon = Icons.Default.QueryStats,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToStats,
                            )
                        } else {
                            LockedFeatureCard(
                                title = "Smart Playlists",
                                description = "Rule-based auto playlists",
                                icon = Icons.Default.AutoAwesome,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    lockedFeature = AppFeature.SMART_PLAYLISTS
                                    showUpgradeDialog = true
                                },
                            )
                            LockedFeatureCard(
                                title = "Stats",
                                description = "Library insights",
                                icon = Icons.Default.QueryStats,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    lockedFeature = AppFeature.LIBRARY_STATS
                                    showUpgradeDialog = true
                                },
                            )
                        }
                    }
                }
            }

            if (!isPremium) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Premium Features")
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LockedFeatureCard(
                            title = "Tags",
                            description = "Organize with custom tags",
                            icon = Icons.Default.Label,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                lockedFeature = AppFeature.TAGS
                                showUpgradeDialog = true
                            },
                        )
                        LockedFeatureCard(
                            title = "Playlist",
                            description = "Custom listening playlists",
                            icon = Icons.Default.Collections,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                lockedFeature = AppFeature.COLLECTIONS
                                showUpgradeDialog = true
                            },
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun AllSongsCard(
    songCount: Int,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
) {
    GlassSurface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        emphasized = true,
        highlighted = true,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text("All Songs", style = MaterialTheme.typography.titleMedium)
                Text(
                    when (songCount) {
                        1 -> "1 song"
                        else -> "$songCount songs"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onPlayAll,
                enabled = songCount > 0,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play all songs",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AllSongsHeader(
    songCount: Int,
    onNavigateBack: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to playlists",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when (songCount) {
                        1 -> "1 song"
                        else -> "$songCount songs"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onPlayAll,
            enabled = songCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play All")
        }
    }
}

@Composable
private fun PlaylistsSectionHeader(onCreateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Playlists",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onCreateClick) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create playlist",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SmartPlaylistListItem(
    playlist: SmartPlaylist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(
                            when (playlist.matchCount) {
                                1 -> "1 song"
                                else -> "${playlist.matchCount} songs"
                            },
                        )
                        append(" · ")
                        append(
                            when (playlist.ruleCount) {
                                1 -> "1 rule"
                                else -> "${playlist.ruleCount} rules"
                            },
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPlayClick) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play smart playlist",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete smart playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistListItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (playlist.coverPath.isNotBlank()) {
                AlbumArt(
                    artworkPath = playlist.coverPath,
                    modifier = Modifier.size(48.dp),
                    contentDescription = playlist.name,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when (playlist.trackCount) {
                        1 -> "1 song"
                        else -> "${playlist.trackCount} songs"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onPlayClick,
                enabled = playlist.trackCount > 0,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play playlist",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                placeholder = { Text("e.g. Favorites") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ConfirmDeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Delete playlist?",
    body: String = "\"$playlistName\" will be permanently deleted. Songs in your library are not removed.",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(body)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongListItem(
    track: Track,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onAddToQueue: () -> Unit,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val fill = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        reverieGlassColor()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onLongClick != null) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                shape = ReverieTileShape,
                color = fill,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                SongListItemContent(
                    track = track,
                    selectionMode = selectionMode,
                    selected = selected,
                )
            }
        } else {
            Surface(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                shape = ReverieTileShape,
                color = fill,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                SongListItemContent(
                    track = track,
                    selectionMode = selectionMode,
                    selected = selected,
                )
            }
        }
        if (!selectionMode) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Song options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Song details") },
                        onClick = {
                            menuExpanded = false
                            onDetailsClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongListItemContent(
    track: Track,
    selectionMode: Boolean,
    selected: Boolean,
) {
    Row(
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        AlbumArt(
            artworkPath = track.artworkPath.takeIf { it.isNotBlank() },
            modifier = Modifier.size(40.dp),
            contentDescription = track.title,
            listThumbnail = true,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${track.artist} · ${track.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
private fun LibraryTopBar(onSearchClick: () -> Unit) {
    ReverieScreenHeader(
        title = "Library",
        actions = {
            GlassSurface(
                onClick = onSearchClick,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}

@Composable
private fun FolderBrowserHeader(
    breadcrumb: String,
    canNavigateUp: Boolean,
    subtreeSongCount: Int,
    selectionMode: Boolean,
    selectionCount: Int,
    actionsEnabled: Boolean,
    hasSelection: Boolean,
    onNavigateUp: () -> Unit,
    onPlayAll: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
) {
    var actionsExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canNavigateUp) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up one folder",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = breadcrumb,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        selectionMode && selectionCount > 0 ->
                            if (selectionCount == 1) "1 selected" else "$selectionCount selected"
                        selectionMode -> "Select songs or folders"
                        subtreeSongCount == 0 -> "No songs in this folder"
                        subtreeSongCount == 1 -> "1 song in this folder & subfolders"
                        else -> "$subtreeSongCount songs in this folder & subfolders"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onPlayAll,
                enabled = subtreeSongCount > 0 && !selectionMode,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play All")
            }
            Box {
                OutlinedButton(
                    onClick = { actionsExpanded = true },
                    enabled = actionsEnabled,
                ) {
                    Text("Actions")
                }
                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false },
                ) {
                    if (selectionMode) {
                        DropdownMenuItem(
                            text = { Text("Select all") },
                            leadingIcon = {
                                Icon(Icons.Default.CheckBox, contentDescription = null)
                            },
                            onClick = {
                                actionsExpanded = false
                                onSelectAll()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Cancel selection") },
                            leadingIcon = {
                                Icon(Icons.Default.DisabledByDefault, contentDescription = null)
                            },
                            onClick = {
                                actionsExpanded = false
                                onClearSelection()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Create folder") },
                        leadingIcon = {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                        },
                        onClick = {
                            actionsExpanded = false
                            onCreateFolderClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        },
                        enabled = !selectionMode || hasSelection,
                        onClick = {
                            actionsExpanded = false
                            onDeleteClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move") },
                        leadingIcon = {
                            Icon(Icons.Default.DriveFileMove, contentDescription = null)
                        },
                        enabled = !selectionMode || hasSelection,
                        onClick = {
                            actionsExpanded = false
                            onMoveClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderListItem(
    folder: MusicFolder,
    onClick: () -> Unit,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    GlassSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        highlighted = selected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        folder.songCount == 1 -> "1 song"
                        else -> "${folder.songCount} songs"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!selectionMode) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArtistBrowserHeader(
    artistName: String,
    songCount: Int,
    onNavigateBack: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to artists",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Artist",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (songCount) {
                        1 -> "1 song"
                        else -> "$songCount songs"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onPlayAll,
            enabled = songCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play All")
        }
    }
}

@Composable
private fun ArtistListItem(
    artist: Artist,
    onClick: () -> Unit,
    onPlayAll: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(artist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${artist.trackCount} songs · ${artist.albumCount} albums",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onPlayAll,
                enabled = artist.trackCount > 0,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play all songs by ${artist.name}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open artist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlbumBrowserHeader(
    album: Album,
    songCount: Int,
    onNavigateBack: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to albums",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Album",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${album.artist} · ${
                        when (songCount) {
                            1 -> "1 song"
                            else -> "$songCount songs"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onPlayAll,
            enabled = songCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play All")
        }
    }
}

@Composable
private fun AlbumListItem(
    album: Album,
    onClick: () -> Unit,
    onPlayAll: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(album.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${album.artist} · ${album.trackCount} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onPlayAll,
                enabled = album.trackCount > 0,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play all songs from ${album.title}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open album",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListItemRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
