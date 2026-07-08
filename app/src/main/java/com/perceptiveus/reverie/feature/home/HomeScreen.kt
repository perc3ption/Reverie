package com.perceptiveus.reverie.feature.home

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.AlbumArtPlaceholder
import com.perceptiveus.reverie.core.design.components.QuickAccessCard
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.domain.model.Track

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val isPremium = viewModel.isPremium()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            HomeHeader(onSearchClick = { /* TODO: Basic search */ })
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Recently Played", action = {
                TextButton(onClick = onNavigateToLibrary) { Text("View all") }
            })
        }
        item {
            RecentlyPlayedRow(tracks = recentlyPlayed)
        }
        item {
            SectionHeader(title = "Quick Access")
        }
        item {
            QuickAccessGrid(
                onImportClick = onNavigateToImport,
                onLibraryClick = onNavigateToLibrary,
                onPremiumClick = onNavigateToPremium,
            )
        }
        if (!isPremium) {
            item {
                UnlockPremiumBanner(onLearnMore = onNavigateToPremium)
            }
        }
    }
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
        Column {
            RetroScreenTitle(title = "Reverie")
            Text(
                text = "Your music. Your world.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}

@Composable
private fun RecentlyPlayedRow(tracks: List<Track>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tracks.forEach { track ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AlbumArtPlaceholder(modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun QuickAccessGrid(
    onImportClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onPremiumClick: () -> Unit,
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
                title = "Library",
                description = "Browse your collection",
                icon = Icons.Default.LibraryMusic,
                modifier = Modifier.weight(1f),
                onClick = onLibraryClick,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessCard(
                title = "Playlists",
                description = "Your playlists and mixes",
                icon = Icons.Default.LibraryMusic,
                modifier = Modifier.weight(1f),
                onClick = onLibraryClick,
            )
            QuickAccessCard(
                title = "Premium Features",
                description = "Unlock advanced tools",
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f),
                onClick = onPremiumClick,
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
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Unlock Premium", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Advanced features, visualizers, smart playlists and more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Learn More",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
