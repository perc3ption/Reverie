package com.perceptiveus.reverie.feature.smartplaylist

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.domain.model.SmartPlaylistField
import com.perceptiveus.reverie.domain.model.SmartPlaylistOperator
import com.perceptiveus.reverie.domain.model.SmartPlaylistRule
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.domain.model.displayName
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistDetailScreen(
    viewModel: SmartPlaylistDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onSongDetailsClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist by viewModel.playlist.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.deleted.collectLatest { onNavigateBack() }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete smart playlist?") },
            text = { Text("\"${playlist?.name.orEmpty()}\" will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = playlist?.name ?: "Smart Playlist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = when (tracks.size) {
                                    0 -> "No matching songs"
                                    1 -> "1 matching song"
                                    else -> "${tracks.size} matching songs"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (rules.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                rules.forEach { rule ->
                                    Text(
                                        text = "• ${ruleSummary(rule)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.playAll()
                                    if (tracks.isNotEmpty()) onNavigateToPlayer()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = tracks.isNotEmpty(),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play all")
                            }
                        }
                    }
                    items(tracks, key = { it.id }) { track ->
                        SmartPlaylistTrackRow(
                            track = track,
                            onPlay = {
                                viewModel.playFrom(track)
                                onNavigateToPlayer()
                            },
                            onDetails = { onSongDetailsClick(track) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistTrackRow(
    track: Track,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onPlay)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(
                artworkPath = track.artworkPath,
                modifier = Modifier.size(48.dp),
                contentDescription = track.title,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clickable(onClick = onDetails),
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(track.artist, track.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun ruleSummary(rule: SmartPlaylistRule): String {
    val field = rule.field.displayName()
    val op = rule.operator.displayName()
    return when (rule.field) {
        SmartPlaylistField.YEAR -> when (rule.operator) {
            SmartPlaylistOperator.BETWEEN ->
                "$field $op ${rule.value}–${rule.valueSecondary}"
            else -> "$field $op ${rule.value}"
        }
        SmartPlaylistField.DATE_ADDED,
        SmartPlaylistField.RECENTLY_PLAYED,
        -> "$field in last ${rule.value} days"
        SmartPlaylistField.PLAY_COUNT -> "$field $op ${rule.value}"
        SmartPlaylistField.RATING -> "$field $op ${rule.value}★"
        else -> "$field $op ${rule.value}"
    }
}
