package com.perceptiveus.reverie.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.LockedFeatureCard
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeaturesScreen(
    featureAccessChecker: FeatureAccessChecker,
    onNavigateBack: () -> Unit,
    onTogglePremiumForTesting: () -> Unit,
) {
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var selectedFeature by remember { mutableStateOf<AppFeature?>(null) }
    val isPremium = featureAccessChecker.isPremium()

    if (showUpgradeDialog) {
        UpgradeDialog(
            feature = selectedFeature,
            onDismiss = { showUpgradeDialog = false },
            onUpgradeClick = {
                showUpgradeDialog = false
                // TODO: Navigate to Play Billing purchase flow.
                onTogglePremiumForTesting()
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Premium") },
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = if (isPremium) {
                        "You have Premium access. Thank you for supporting Reverie!"
                    } else {
                        "Unlock advanced features for your curated music library."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(premiumFeatureItems) { item ->
                LockedFeatureCard(
                    title = item.title,
                    description = item.description,
                    icon = item.icon,
                    onClick = {
                        if (!isPremium) {
                            selectedFeature = item.feature
                            showUpgradeDialog = true
                        }
                    },
                )
            }
        }
    }
}

private data class PremiumFeatureItem(
    val feature: AppFeature,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val premiumFeatureItems = listOf(
    PremiumFeatureItem(AppFeature.UNLIMITED_LIBRARY, "Unlimited Library", "No song limit on imports.", Icons.Default.LibraryMusic),
    PremiumFeatureItem(AppFeature.FAVORITES, "Favorites", "Quick access to loved tracks.", Icons.Default.Favorite),
    PremiumFeatureItem(AppFeature.TAGS, "Tags", "Organize with custom tags.", Icons.Default.Label),
    PremiumFeatureItem(AppFeature.COLLECTIONS, "Collections", "Custom listening collections.", Icons.Default.Collections),
    PremiumFeatureItem(AppFeature.SMART_PLAYLISTS, "Smart Playlists", "Rule-based auto playlists.", Icons.Default.AutoAwesome),
    PremiumFeatureItem(AppFeature.PLAYBACK_SCOPE, "Playback Scope", "Control shuffle and play scope.", Icons.Default.Tune),
    PremiumFeatureItem(AppFeature.ADVANCED_VISUALIZERS, "Advanced Visualizers", "Premium spectrum effects.", Icons.Default.Slideshow),
    PremiumFeatureItem(AppFeature.ADVANCED_SEARCH, "Advanced Search", "Filters and saved searches.", Icons.Default.ManageSearch),
    PremiumFeatureItem(AppFeature.LYRICS, "Lyrics", "Synced lyrics display.", Icons.Default.MusicNote),
    PremiumFeatureItem(AppFeature.METADATA_EDITING, "Metadata Editing", "Edit track info.", Icons.Default.Tune),
    PremiumFeatureItem(AppFeature.ALBUM_ART_EDITING, "Album Art", "Custom cover art.", Icons.Default.Palette),
    PremiumFeatureItem(AppFeature.LIBRARY_STATS, "Library Stats", "Collection insights.", Icons.Default.QueryStats),
    PremiumFeatureItem(AppFeature.UNLIMITED_PLAYLISTS, "Unlimited Playlists", "Beyond the 3-playlist free limit.", Icons.Default.PlaylistPlay),
)
