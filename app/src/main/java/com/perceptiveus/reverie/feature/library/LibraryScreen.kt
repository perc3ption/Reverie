package com.perceptiveus.reverie.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.LockedFeatureCard
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.feature.premium.UpgradeDialog

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPremiumFeatureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.FOLDERS) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var lockedFeature by remember { mutableStateOf<AppFeature?>(null) }

    val folders by viewModel.folders.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isPremium = viewModel.isPremium()

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

    Column(modifier = modifier.fillMaxSize()) {
        LibraryTopBar()
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
                            text = tab.name,
                            style = MaterialTheme.typography.labelLarge,
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
                LibraryTab.FOLDERS -> {
                    items(folders) { folder ->
                        FolderListItem(folder = folder)
                    }
                }
                LibraryTab.ARTISTS -> {
                    items(artists) { artist ->
                        ArtistListItem(artist = artist)
                    }
                }
                LibraryTab.ALBUMS -> {
                    items(albums) { album ->
                        AlbumListItem(album = album)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Quick Access")
            }

            if (!viewModel.canAccess(AppFeature.FAVORITES)) {
                item {
                    LockedFeatureCard(
                        title = "Favorites",
                        description = "128 songs",
                        icon = Icons.Default.Favorite,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            lockedFeature = AppFeature.FAVORITES
                            showUpgradeDialog = true
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(title = "Premium Features")
            }

            if (!isPremium) {
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
                            title = "Collections",
                            description = "Custom listening collections",
                            icon = Icons.Default.Collections,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                lockedFeature = AppFeature.COLLECTIONS
                                showUpgradeDialog = true
                            },
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    LockedFeatureCard(
                        title = "Playback Scope",
                        description = "Control what plays and shuffles",
                        icon = Icons.Default.Tune,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            lockedFeature = AppFeature.PLAYBACK_SCOPE
                            showUpgradeDialog = true
                        },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    LockedFeatureCard(
                        title = "Smart Playlists",
                        description = "Rule-based auto playlists",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            lockedFeature = AppFeature.SMART_PLAYLISTS
                            showUpgradeDialog = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RetroScreenTitle(title = "Library")
        Row {
            IconButton(onClick = { /* TODO: Basic search */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }
    }
}

@Composable
private fun FolderListItem(folder: MusicFolder) {
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
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${folder.songCount} songs · ${folder.albumCount} albums",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
        }
    }
}

@Composable
private fun ArtistListItem(artist: Artist) {
    ListItemRow(
        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = artist.name,
        subtitle = "${artist.trackCount} songs · ${artist.albumCount} albums",
    )
}

@Composable
private fun AlbumListItem(album: Album) {
    ListItemRow(
        icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = album.title,
        subtitle = "${album.artist} · ${album.trackCount} tracks",
    )
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
