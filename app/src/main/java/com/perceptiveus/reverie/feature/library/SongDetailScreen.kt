package com.perceptiveus.reverie.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.PremiumBadge
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
    val isSavingMetadata by viewModel.isSavingMetadata.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTagDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showEditMetadataDialog by remember { mutableStateOf(false) }
    var upgradeFeature by remember { mutableStateOf<AppFeature?>(null) }
    val canAccessTags = viewModel.canAccessTags()
    val canAccessLyrics = viewModel.canAccessLyrics()
    val canAccessRatings = viewModel.canAccessRatings()
    val canAccessAlbumArt = viewModel.canAccessAlbumArtEditing()

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

    LaunchedEffect(viewModel) {
        viewModel.metadataSaved.collectLatest {
            showEditMetadataDialog = false
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

    if (showEditMetadataDialog) {
        val current = track
        if (current != null) {
            EditMetadataDialog(
                track = current,
                isSaving = isSavingMetadata,
                onDismiss = { if (!isSavingMetadata) showEditMetadataDialog = false },
                onSave = { title, artist, album, year, genre ->
                    viewModel.saveMetadata(title, artist, album, year, genre)
                },
            )
        }
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
        // Outer app Scaffold already applies system bar insets.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Song") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        canAccessRatings = canAccessRatings,
                        canAccessAlbumArt = canAccessAlbumArt,
                        onPlay = viewModel::play,
                        onAddToQueue = viewModel::addToQueue,
                        onRatingClick = { rating ->
                            if (canAccessRatings) {
                                viewModel.setRating(rating)
                            } else {
                                upgradeFeature = AppFeature.RATINGS
                            }
                        },
                        onImportAlbumArt = {
                            if (canAccessAlbumArt) {
                                pickAlbumArt.launch(arrayOf("image/*"))
                            } else {
                                upgradeFeature = AppFeature.ALBUM_ART_EDITING
                            }
                        },
                    )
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
                    title = "Details",
                    actionText = "Edit",
                    actionIcon = Icons.Default.Edit,
                    onActionClick = { showEditMetadataDialog = true },
                    collapsible = true,
                    initiallyExpanded = true,
                ) {
                    MetadataSection(
                        track = current,
                        tags = tags,
                        onAddTag = {
                            if (canAccessTags) showTagDialog = true else upgradeFeature = AppFeature.TAGS
                        },
                        onRemoveTag = viewModel::removeTag,
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
    canAccessRatings: Boolean,
    canAccessAlbumArt: Boolean,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onRatingClick: (Int) -> Unit,
    onImportAlbumArt: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArt(
            artworkPath = track.artworkPath,
            modifier = Modifier.size(192.dp),
            contentDescription = track.title,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(
                onClick = onImportAlbumArt,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    text = if (track.artworkPath.isNotBlank()) "Reimport Art" else "Import Art",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (!canAccessAlbumArt) {
                PremiumBadge()
            }
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOf(track.artist, track.album)
                .filter { it.isNotBlank() }
                .joinToString(" | "),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        SongRatingRow(
            rating = track.rating,
            canAccess = canAccessRatings,
            onRatingClick = onRatingClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = onAddToQueue,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Add to Queue",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SongRatingRow(
    rating: Int,
    canAccess: Boolean,
    onRatingClick: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (star in 1..5) {
            val filled = star <= rating
            IconButton(
                onClick = { onRatingClick(star) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Rate $star of 5",
                    tint = if (filled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        if (!canAccess) {
            Spacer(modifier = Modifier.size(4.dp))
            PremiumBadge()
        }
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionIcon: ImageVector = Icons.Default.Add,
    onActionClick: (() -> Unit)? = null,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title, initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val showContent = !collapsible || expanded

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (collapsible) {
                        Modifier.clickable { expanded = !expanded }
                    } else {
                        Modifier
                    },
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (collapsible) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(22.dp),
                    )
                }
            }
            if (actionText != null && onActionClick != null) {
                TextButton(onClick = onActionClick) {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(actionText)
                }
            }
        }
        AnimatedVisibility(visible = showContent) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun MetadataSection(
    track: Track,
    tags: List<Tag>,
    onAddTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
) {
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
        MetadataRow(
            label = "Rating",
            value = if (track.rating > 0) "${track.rating} / 5" else "Unrated",
        )
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
        MetadataTagsRow(
            tags = tags,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataTagsRow(
    tags: List<Tag>,
    onAddTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(100.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                if (tags.isEmpty()) {
                    Text(
                        text = "None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    FlowRow(
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
                                    modifier = Modifier.padding(
                                        start = 12.dp,
                                        end = 4.dp,
                                        top = 4.dp,
                                        bottom = 4.dp,
                                    ),
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
                TextButton(
                    onClick = onAddTag,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Add tag")
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
private fun EditMetadataDialog(
    track: Track,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, year: String, genre: String) -> Unit,
) {
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var year by remember(track.id) {
        mutableStateOf(if (track.year > 0) track.year.toString() else "")
    }
    var genre by remember(track.id) { mutableStateOf(track.genre) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit metadata") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Changes are written into the audio file, so they stay if you copy it elsewhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Year") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, artist, album, year, genre) },
                enabled = !isSaving && title.isNotBlank(),
            ) {
                Text(if (isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
    )
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
