package com.perceptiveus.reverie.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.ReveriePurple
import com.perceptiveus.reverie.core.design.ReverieTileShape
import com.perceptiveus.reverie.core.design.components.AlbumArt
import com.perceptiveus.reverie.core.design.components.formatArtistAlbum
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Track>,
    currentIndex: Int,
    queueSource: QueueSource,
    disabledTrackIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    onToggleTrackEnabled: (String) -> Unit = {},
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    // Instant jump — animateScrollToItem walks/composes items and janks large queues.
    LaunchedEffect(currentIndex, queue.size) {
        if (currentIndex in queue.indices) {
            listState.scrollToItem(currentIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            QueueSheetHeader(
                queueSource = queueSource,
                queue = queue,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            if (queue.isEmpty()) {
                Text(
                    text = "Play songs from your library to build a queue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            } else {
                val enabledCount = remember(queue, disabledTrackIds) {
                    queue.count { it.id !in disabledTrackIds }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { index, track -> "${track.id}-$index" },
                        contentType = { _, _ -> "queue-row" },
                    ) { index, track ->
                        val isDisabled = track.id in disabledTrackIds
                        QueueSongRow(
                            track = track,
                            index = index,
                            isCurrent = index == currentIndex,
                            isDisabled = isDisabled,
                            canToggleOff = enabledCount > 1,
                            canMoveUp = index > 0,
                            canMoveDown = index < queue.lastIndex,
                            onClick = { onTrackSelected(index) },
                            onToggleEnabled = { onToggleTrackEnabled(track.id) },
                            onMoveUp = { onMoveTrack(index, index - 1) },
                            onMoveDown = { onMoveTrack(index, index + 1) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSheetHeader(
    queueSource: QueueSource,
    queue: List<Track>,
    modifier: Modifier = Modifier,
) {
    val presentation = remember(queueSource, queue) {
        queueHeaderPresentation(queueSource, queue)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueHeaderArtwork(
            artworkPath = presentation.artworkPath,
            fallbackIcon = presentation.fallbackIcon,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = presentation.kindLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = presentation.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QueueHeaderArtwork(
    artworkPath: String,
    fallbackIcon: ImageVector,
) {
    if (artworkPath.isNotBlank()) {
        AlbumArt(
            artworkPath = artworkPath,
            modifier = Modifier.size(64.dp),
            contentDescription = null,
        )
    } else {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

private data class QueueHeaderPresentation(
    val kindLabel: String,
    val title: String,
    val subtitle: String,
    val artworkPath: String,
    val fallbackIcon: ImageVector,
)

private fun queueHeaderPresentation(
    source: QueueSource,
    queue: List<Track>,
): QueueHeaderPresentation {
    val songCountLabel = when (queue.size) {
        0 -> "No songs"
        1 -> "1 song"
        else -> "${queue.size} songs"
    }
    val firstArt = queue.firstOrNull { it.artworkPath.isNotBlank() }?.artworkPath.orEmpty()

    return when (source) {
        QueueSource.Library -> QueueHeaderPresentation(
            kindLabel = "LIBRARY",
            title = "All Songs",
            subtitle = songCountLabel,
            artworkPath = firstArt,
            fallbackIcon = Icons.Default.LibraryMusic,
        )
        is QueueSource.Playlist -> {
            val details = listOfNotNull(
                songCountLabel,
                source.description.trim().takeIf { it.isNotEmpty() },
            ).joinToString(" · ")
            QueueHeaderPresentation(
                kindLabel = "PLAYLIST",
                title = source.name,
                subtitle = details,
                artworkPath = source.coverPath.ifBlank { firstArt },
                fallbackIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
            )
        }
        is QueueSource.Album -> {
            val details = buildList {
                add(source.artist)
                if (source.year > 0) add(source.year.toString())
                add(songCountLabel)
            }.joinToString(" · ")
            QueueHeaderPresentation(
                kindLabel = "ALBUM",
                title = source.title,
                subtitle = details,
                artworkPath = source.artworkPath.ifBlank { firstArt },
                fallbackIcon = Icons.Default.Album,
            )
        }
        is QueueSource.Artist -> QueueHeaderPresentation(
            kindLabel = "ARTIST",
            title = source.name,
            subtitle = songCountLabel,
            artworkPath = source.artworkPath.ifBlank { firstArt },
            fallbackIcon = Icons.Default.Person,
        )
        is QueueSource.Folder -> QueueHeaderPresentation(
            kindLabel = "FOLDER",
            title = source.name,
            subtitle = songCountLabel,
            artworkPath = firstArt,
            fallbackIcon = Icons.Default.Folder,
        )
        is QueueSource.SmartPlaylist -> QueueHeaderPresentation(
            kindLabel = "SMART PLAYLIST",
            title = source.name,
            subtitle = songCountLabel,
            artworkPath = firstArt,
            fallbackIcon = Icons.Default.AutoAwesome,
        )
        QueueSource.RecentlyPlayed -> QueueHeaderPresentation(
            kindLabel = "QUEUE",
            title = "Recently Played",
            subtitle = songCountLabel,
            artworkPath = firstArt,
            fallbackIcon = Icons.Default.History,
        )
        QueueSource.Unknown -> QueueHeaderPresentation(
            kindLabel = "QUEUE",
            title = "Queue",
            subtitle = songCountLabel,
            artworkPath = firstArt,
            fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic,
        )
    }
}

@Composable
private fun QueueSongRow(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isDisabled: Boolean,
    canToggleOff: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val contentAlpha = if (isDisabled) 0.45f else 1f
    val rowColor = when {
        isCurrent && !isDisabled -> ReveriePurple.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surface
    }

    // Flat row — GlassSurface / IconButton chrome was too heavy for 100+ item queues.
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ReverieTileShape,
        color = rowColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(start = 2.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp),
            ) {
                QueueRowIconAction(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move up",
                    enabled = canMoveUp,
                    onClick = onMoveUp,
                )
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                QueueRowIconAction(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move down",
                    enabled = canMoveDown,
                    onClick = onMoveDown,
                )
            }
            AlbumArt(
                artworkPath = track.artworkPath.takeIf { it.isNotBlank() },
                modifier = Modifier.size(44.dp),
                contentDescription = track.title,
                listThumbnail = true,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isCurrent && !isDisabled) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        textDecoration = if (isDisabled) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                    ),
                    color = when {
                        isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatArtistAlbum(track.artist, track.album),
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (isDisabled) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent && !isDisabled) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Now playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            QueueRowIconAction(
                icon = if (isDisabled) Icons.Default.Close else Icons.Default.Remove,
                contentDescription = if (isDisabled) {
                    "Include in queue"
                } else {
                    "Skip for this session"
                },
                enabled = isDisabled || canToggleOff,
                onClick = onToggleEnabled,
                tint = if (isDisabled) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun QueueRowIconAction(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(28.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(4.dp)
            .size(20.dp),
        tint = if (enabled) tint else tint.copy(alpha = 0.3f),
    )
}
