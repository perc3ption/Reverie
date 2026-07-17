package com.perceptiveus.reverie.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Tag
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.feature.player.lyrics.LyricsPanel
import com.perceptiveus.reverie.feature.premium.UpgradeDialog
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    viewModel: SongDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track by viewModel.track.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val playlists by viewModel.playlistsContainingTrack.collectAsState()
    val availablePlaylists by viewModel.availablePlaylists.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTagDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var upgradeFeature by remember { mutableStateOf<AppFeature?>(null) }
    val canAccessTags = viewModel.canAccessTags()
    val canAccessLyrics = viewModel.canAccessLyrics()

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

    if (showTagDialog) {
        AddTagDialog(
            availableTags = availableTags,
            onDismiss = { showTagDialog = false },
            onCreateTag = { name ->
                viewModel.addTag(name)
                showTagDialog = false
            },
            onPickExistingTag = { tag ->
                viewModel.addExistingTag(tag.id)
                showTagDialog = false
            },
        )
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            availablePlaylists = availablePlaylists,
            onDismiss = { showPlaylistDialog = false },
            onAddToPlaylist = { playlist ->
                viewModel.addToPlaylist(playlist.id)
                showPlaylistDialog = false
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylistAndAdd(name)
                showPlaylistDialog = false
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Song") },
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
        val current = track
        if (current == null) {
            Text(
                text = "Song not found.",
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                SongHeader(
                    track = current,
                    onPlay = viewModel::play,
                )
            }

            item {
                Section(title = "Details") {
                    MetadataSection(track = current)
                }
            }

            item {
                Section(
                    title = "Tags",
                    actionText = "Add",
                    onActionClick = {
                        if (canAccessTags) showTagDialog = true else upgradeFeature = AppFeature.TAGS
                    },
                ) {
                    TagsSection(
                        tags = tags,
                        onRemoveTag = viewModel::removeTag,
                    )
                }
            }

            item {
                Section(
                    title = "Playlists",
                    actionText = "Add",
                    onActionClick = { showPlaylistDialog = true },
                ) {
                    PlaylistsSection(
                        playlists = playlists,
                        onPlaylistClick = onNavigateToPlaylist,
                    )
                }
            }

            item {
                Section(
                    title = "Lyrics",
                    actionText = if (lyrics != null) "Reimport" else null,
                    onActionClick = if (lyrics != null) {
                        {
                            if (canAccessLyrics) {
                                pickLyricsFile.launch(arrayOf("application/octet-stream", "text/plain", "text/*", "*/*"))
                            } else {
                                upgradeFeature = AppFeature.LYRICS
                            }
                        }
                    } else {
                        null
                    },
                ) {
                    LyricsPanel(
                        lyrics = lyrics,
                        positionMs = 0L,
                        hasAccess = canAccessLyrics,
                        canImport = current.filePath.isNotBlank(),
                        onLockedClick = { upgradeFeature = AppFeature.LYRICS },
                        onImportClick = {
                            pickLyricsFile.launch(arrayOf("application/octet-stream", "text/plain", "text/*", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SongHeader(
    track: Track,
    onPlay: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArt(
            artworkPath = track.artworkPath,
            modifier = Modifier.size(220.dp),
            contentDescription = track.title,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = track.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = track.album,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Play")
        }
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (actionText != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(actionText)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun MetadataSection(track: Track) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MetadataRow(label = "Title", value = track.title)
        MetadataRow(label = "Artist", value = track.artist)
        MetadataRow(label = "Album", value = track.album)
        MetadataRow(
            label = "Year",
            value = if (track.year > 0) track.year.toString() else "-",
        )
        MetadataRow(
            label = "Genre",
            value = track.genre.ifBlank { "-" },
        )
        MetadataRow(label = "Duration", value = formatDuration(track.durationMs))
        MetadataRow(label = "Favorite", value = if (track.isFavorite) "Yes" else "No")
        if (track.dateAdded > 0L) {
            MetadataRow(label = "Date added", value = formatDateAdded(track.dateAdded))
        }
        MetadataRow(
            label = "File name",
            value = track.filePath.substringAfterLast(File.separatorChar).ifBlank { "-" },
        )
        MetadataRow(
            label = "File path",
            value = track.filePath.ifBlank { "-" },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<Tag>,
    onRemoveTag: (Tag) -> Unit,
) {
    if (tags.isEmpty()) {
        EmptySectionText("No tags yet.")
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    IconButton(
                        onClick = { onRemoveTag(tag) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove tag",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
) {
    if (playlists.isEmpty()) {
        EmptySectionText("This song is not in any playlists yet.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        playlists.forEach { playlist ->
            Surface(
                onClick = { onPlaylistClick(playlist) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${playlist.trackCount} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySectionText(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun AddTagDialog(
    availableTags: List<Tag>,
    onDismiss: () -> Unit,
    onCreateTag: (String) -> Unit,
    onPickExistingTag: (Tag) -> Unit,
) {
    var tagName by remember { mutableStateOf("") }
    val filteredExisting = remember(tagName, availableTags) {
        val query = tagName.trim()
        if (query.isBlank()) {
            availableTags
        } else {
            availableTags.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
    val exactMatchExists = availableTags.any { it.name.equals(tagName.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("New tag name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (availableTags.isEmpty()) {
                    Text(
                        text = "No existing tags yet. Create the first one above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Existing tags",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (filteredExisting.isEmpty()) {
                        Text(
                            text = "No matching tags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        filteredExisting.forEach { tag ->
                            OutlinedButton(
                                onClick = { onPickExistingTag(tag) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = tag.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateTag(tagName) },
                enabled = tagName.isNotBlank() && !exactMatchExists,
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
private fun AddToPlaylistDialog(
    availablePlaylists: List<Playlist>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (availablePlaylists.isEmpty()) {
                    Text(
                        text = "No available playlists. Create a new one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    availablePlaylists.forEach { playlist ->
                        OutlinedButton(
                            onClick = { onAddToPlaylist(playlist) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = playlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("New playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreatePlaylist(newPlaylistName) },
                enabled = newPlaylistName.isNotBlank(),
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

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "-"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatDateAdded(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(epochMs))
