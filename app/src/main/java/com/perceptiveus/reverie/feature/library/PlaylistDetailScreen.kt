package com.perceptiveus.reverie.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.ReverieArtShape
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.GlassSurface
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    onNavigateBack: () -> Unit,
    onSongClick: (Track) -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist by viewModel.playlist.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val availableTracks by viewModel.availableTracks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDescription by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val pickCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importCover(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.deleted.collectLatest {
            onNavigateBack()
        }
    }

    if (showAddDialog) {
        AddSongsToPlaylistDialog(
            availableTracks = availableTracks,
            onDismiss = { showAddDialog = false },
            onAddTracks = { selected ->
                viewModel.addTracks(selected)
                showAddDialog = false
            },
        )
    }

    if (showEditDescription) {
        val current = playlist
        if (current != null) {
            EditDescriptionDialog(
                initial = current.description,
                onDismiss = { showEditDescription = false },
                onSave = { description ->
                    viewModel.saveDescription(description)
                    showEditDescription = false
                },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete playlist?") },
            text = {
                Text(
                    "\"${playlist?.name.orEmpty()}\" will be permanently deleted. " +
                        "Songs in your library are not removed.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deletePlaylist()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Outer app Scaffold already applies system bar insets.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Playlist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val current = playlist
        if (current == null) {
            Text(
                text = "Playlist not found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                PlaylistHeader(
                    playlist = current,
                    trackCount = tracks.size,
                    onPlayAll = {
                        viewModel.playAll()
                        onNavigateToPlayer()
                    },
                    onChangeCover = {
                        pickCover.launch(arrayOf("image/*"))
                    },
                    onEditDescription = { showEditDescription = true },
                )
            }

            item {
                PlaylistSongsHeader(onAddClick = { showAddDialog = true })
            }

            if (tracks.isEmpty()) {
                item {
                    Text(
                        text = "This playlist has no songs yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    PlaylistTrackRow(
                        track = track,
                        onClick = { viewModel.playFrom(track) },
                        onDetailsClick = { onSongClick(track) },
                        onRemoveClick = { viewModel.removeTrack(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    trackCount: Int,
    onPlayAll: () -> Unit,
    onChangeCover: () -> Unit,
    onEditDescription: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassSurface(
                onClick = onChangeCover,
                shape = ReverieArtShape,
                emphasized = true,
            ) {
                if (playlist.coverPath.isNotBlank()) {
                    AlbumArt(
                        artworkPath = playlist.coverPath,
                        modifier = Modifier.size(96.dp),
                        contentDescription = "Playlist cover",
                    )
                } else {
                    Column(
                        modifier = Modifier.size(96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Add cover",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Cover",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when (trackCount) {
                        1 -> "1 song"
                        else -> "$trackCount songs"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditDescription),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = playlist.description.ifBlank { "Add a description…" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (playlist.description.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit description",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onPlayAll,
            enabled = trackCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Play All")
        }
    }
}

@Composable
private fun EditDescriptionDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playlist description") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) {
                Text("Save")
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
private fun PlaylistSongsHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Songs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Add")
        }
    }
}

@Composable
private fun AddSongsToPlaylistDialog(
    availableTracks: List<Track>,
    onDismiss: () -> Unit,
    onAddTracks: (List<Track>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val filtered = remember(query, availableTracks) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            availableTracks
        } else {
            availableTracks.filter { track ->
                track.title.contains(trimmed, ignoreCase = true) ||
                    track.artist.contains(trimmed, ignoreCase = true) ||
                    track.album.contains(trimmed, ignoreCase = true)
            }
        }
    }
    val selectedTracks = remember(selectedIds, availableTracks) {
        availableTracks.filter { it.id in selectedIds }
    }
    val allFilteredSelected = filtered.isNotEmpty() && filtered.all { it.id in selectedIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Songs") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search library") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when {
                    availableTracks.isEmpty() -> {
                        Text(
                            text = "All library songs are already in this playlist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    filtered.isEmpty() -> {
                        Text(
                            text = "No matching songs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (selectedIds.isEmpty()) {
                                    "${filtered.size} songs"
                                } else {
                                    "${selectedIds.size} selected"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = {
                                    selectedIds = if (allFilteredSelected) {
                                        selectedIds - filtered.map { it.id }.toSet()
                                    } else {
                                        selectedIds + filtered.map { it.id }
                                    }
                                },
                            ) {
                                Text(if (allFilteredSelected) "Clear" else "Select all")
                            }
                        }
                        filtered.forEach { track ->
                            val selected = track.id in selectedIds
                            Surface(
                                onClick = {
                                    selectedIds = if (selected) {
                                        selectedIds - track.id
                                    } else {
                                        selectedIds + track.id
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) {
                                                selectedIds + track.id
                                            } else {
                                                selectedIds - track.id
                                            }
                                        },
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyLarge,
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
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddTracks(selectedTracks) },
                enabled = selectedTracks.isNotEmpty(),
            ) {
                Text(
                    when (selectedTracks.size) {
                        0 -> "Add"
                        1 -> "Add 1 song"
                        else -> "Add ${selectedTracks.size} songs"
                    },
                )
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
private fun PlaylistTrackRow(
    track: Track,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onRemoveClick: () -> Unit,
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
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (track.artworkPath.isNotBlank()) {
                AlbumArt(
                    artworkPath = track.artworkPath,
                    modifier = Modifier.size(40.dp),
                    contentDescription = track.title,
                    listThumbnail = true,
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
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDetailsClick) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Song details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
