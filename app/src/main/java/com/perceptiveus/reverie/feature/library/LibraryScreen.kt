package com.perceptiveus.reverie.feature.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
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
    val snackbarHostState = remember { SnackbarHostState() }

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
    val isPremium = viewModel.isPremium()

    val libraryCanGoBack = showAllSongs ||
        artistBrowser.selectedArtist != null ||
        albumBrowser.selectedAlbum != null ||
        folderBrowser.canNavigateUp

    BackHandler(enabled = libraryCanGoBack) {
        viewModel.handleLibraryBack()
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
                                    items(playlists, key = { it.id }) { playlist ->
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
                                onNavigateUp = viewModel::navigateFolderUp,
                                onPlayAll = {
                                    if (viewModel.playAllInCurrentFolder()) {
                                        onNavigateToPlayer()
                                    }
                                },
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
                                onClick = { viewModel.openFolder(folder.relativePath) },
                            )
                        }
                        items(
                            folderBrowser.songs,
                            key = { "song-${it.id}" },
                        ) { track ->
                            SongListItem(
                                track = track,
                                onClick = { viewModel.playSongInFolder(track) },
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
                        items(artists, key = { it.id }) { artist ->
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
                        items(albums, key = { it.id }) { album ->
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
    Surface(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium,
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
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
                    when (playlist.ruleCount) {
                        1 -> "Smart · 1 rule"
                        else -> "Smart · ${playlist.ruleCount} rules"
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
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

@Composable
private fun SongListItem(
    track: Track,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (track.artworkPath.isNotBlank()) {
                    AlbumArt(
                        artworkPath = track.artworkPath,
                        modifier = Modifier.size(40.dp),
                        contentDescription = track.title,
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
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

@Composable
private fun LibraryTopBar(onSearchClick: () -> Unit) {
    ReverieScreenHeader(
        title = "Library",
        actions = {
            Row {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
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
    onNavigateUp: () -> Unit,
    onPlayAll: () -> Unit,
) {
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
                    text = when (subtreeSongCount) {
                        0 -> "No songs in this folder"
                        1 -> "1 song in this folder & subfolders"
                        else -> "$subtreeSongCount songs in this folder & subfolders"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onPlayAll,
            enabled = subtreeSongCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play All")
        }
    }
}

@Composable
private fun FolderListItem(
    folder: MusicFolder,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open folder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
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
