package com.perceptiveus.reverie.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.GlassSurface
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onSongDetailsClick: (Track) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val results by viewModel.results.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = results.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Songs, artists, albums…") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (results.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when {
                    results.isBlankQuery -> {
                        item {
                            Text(
                                text = "Search your library by song, artist, album, playlist, or folder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                    results.isEmpty -> {
                        item {
                            Text(
                                text = "No matches for \"${results.query.trim()}\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                    else -> {
                        if (results.songs.isNotEmpty()) {
                            item { SectionHeader(title = "Songs") }
                            items(results.songs, key = { "song-${it.id}" }) { track ->
                                SearchSongRow(
                                    track = track,
                                    onClick = {
                                        viewModel.playSong(track)
                                        onNavigateToPlayer()
                                    },
                                    onDetailsClick = { onSongDetailsClick(track) },
                                    onAddToQueue = { viewModel.addToQueue(track) },
                                )
                            }
                        }
                        if (results.artists.isNotEmpty()) {
                            item { SectionHeader(title = "Artists") }
                            items(results.artists, key = { "artist-${it.id}" }) { artist ->
                                SearchArtistRow(
                                    artist = artist,
                                    onClick = {
                                        if (viewModel.playArtist(artist)) {
                                            onNavigateToPlayer()
                                        }
                                    },
                                )
                            }
                        }
                        if (results.albums.isNotEmpty()) {
                            item { SectionHeader(title = "Albums") }
                            items(results.albums, key = { "album-${it.id}" }) { album ->
                                SearchAlbumRow(
                                    album = album,
                                    onClick = {
                                        if (viewModel.playAlbum(album)) {
                                            onNavigateToPlayer()
                                        }
                                    },
                                )
                            }
                        }
                        if (results.playlists.isNotEmpty()) {
                            item { SectionHeader(title = "Playlists") }
                            items(results.playlists, key = { "playlist-${it.id}" }) { playlist ->
                                SearchPlaylistRow(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist) },
                                )
                            }
                        }
                        if (results.folders.isNotEmpty()) {
                            item { SectionHeader(title = "Folders") }
                            items(results.folders, key = { "folder-${it.id}" }) { folder ->
                                SearchFolderRow(
                                    folder = folder,
                                    onClick = {
                                        if (viewModel.playFolder(folder)) {
                                            onNavigateToPlayer()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSongRow(
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
        GlassSurface(
            onClick = onClick,
            modifier = Modifier.weight(1f),
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
                        listThumbnail = true,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artist} · ${track.album}",
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
private fun SearchArtistRow(
    artist: Artist,
    onClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play all",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchAlbumRow(
    album: Album,
    onClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play all",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchPlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistPlay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${playlist.trackCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open playlist",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchFolderRow(
    folder: MusicFolder,
    onClick: () -> Unit,
) {
    GlassSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                    "${folder.songCount} songs · ${folder.albumCount} albums",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play all",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
