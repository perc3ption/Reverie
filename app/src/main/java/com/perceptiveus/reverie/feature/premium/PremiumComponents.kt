package com.perceptiveus.reverie.feature.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.PremiumBadge
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.AppFeature

@Composable
fun PremiumLockedScreen(
    feature: AppFeature,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            PremiumBadge()
            Spacer(modifier = Modifier.height(12.dp))
            RetroScreenTitle(title = feature.name.replace('_', ' '))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = featureDescription(feature),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Upgrade to Premium")
            }
            if (onDismiss != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Go Back")
                }
            }
        }
    }
}

@Composable
fun UpgradeDialog(
    feature: AppFeature?,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = if (feature != null) {
                    "Unlock ${feature.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}"
                } else {
                    "Upgrade to Premium"
                },
            )
        },
        text = {
            Text(
                text = if (feature != null) {
                    featureDescription(feature)
                } else {
                    "Get unlimited library, favorites, tags, smart playlists, advanced visualizers, and more."
                },
            )
        },
        confirmButton = {
            Button(onClick = onUpgradeClick) {
                Text("Learn More")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
    )
}

private fun featureDescription(feature: AppFeature): String = when (feature) {
    AppFeature.UNLIMITED_LIBRARY -> "Import and enjoy unlimited songs beyond the free 500-track limit."
    AppFeature.METADATA_EDITING -> "Edit track titles, artists, albums, and other metadata."
    AppFeature.ALBUM_ART_EDITING -> "Set custom album art for any track or album."
    AppFeature.TAGS -> "Organize your library with custom tags."
    AppFeature.FAVORITES -> "Mark tracks as favorites for quick access."
    AppFeature.UNLIMITED_PLAYLISTS -> "Create more than 3 playlists."
    AppFeature.LYRICS -> "View synced lyrics while listening."
    AppFeature.PLAYBACK_SCOPE -> "Control what plays and shuffles across your library."
    AppFeature.COLLECTIONS -> "Create custom listening collections."
    AppFeature.SMART_PLAYLISTS -> "Build rule-based playlists that update automatically."
    AppFeature.ADVANCED_VISUALIZERS -> "Access premium spectrum and waveform visualizers."
    AppFeature.ADVANCED_SEARCH -> "Use advanced filters and saved searches."
    AppFeature.LIBRARY_STATS -> "View detailed library statistics and insights."
}
