package com.perceptiveus.reverie.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.core.settings.AppThemePreference

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName by viewModel.displayName.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            RetroScreenTitle(
                title = "Settings",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        item {
            SectionHeader(title = "Profile")
            OutlinedTextField(
                value = displayName,
                onValueChange = viewModel::setDisplayName,
                label = { Text("Display Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Appearance")
            ThemeSelector(
                selected = themePreference,
                onSelected = viewModel::setThemePreference,
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Premium")
            PremiumStatusCard(
                isPremium = entitlements.isPremium,
                onToggleForTesting = viewModel::togglePremiumForTesting,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onNavigateToPremium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    if (entitlements.isPremium) "View Premium Features" else "Explore Premium Features",
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.restorePurchases { restored ->
                        restoreMessage = if (restored) {
                            "Purchases restored."
                        } else {
                            "No purchases found to restore."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Restore Purchases")
            }
            restoreMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "About")
            AboutSection()
        }
    }
}

@Composable
private fun ThemeSelector(
    selected: AppThemePreference,
    onSelected: (AppThemePreference) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        AppThemePreference.entries.forEachIndexed { index, preference ->
            SegmentedButton(
                selected = selected == preference,
                onClick = { onSelected(preference) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AppThemePreference.entries.size,
                ),
            ) {
                Text(preference.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
private fun PremiumStatusCard(
    isPremium: Boolean,
    onToggleForTesting: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)) {
                Text(
                    text = if (isPremium) "Premium Active" else "Free Plan",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (isPremium) {
                        "All premium features unlocked."
                    } else {
                        "Up to ${FeatureAccessChecker.FREE_MAX_SONGS} songs, " +
                            "${FeatureAccessChecker.FREE_MAX_PLAYLISTS} playlists."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Dev-only toggle; remove when Play Billing ships.
            Button(onClick = onToggleForTesting) {
                Text(if (isPremium) "Revoke" else "Unlock")
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "Reverie v1.0",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = "A local, offline music player for curated collections. " +
                "No accounts, no cloud, no ads.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "Your music stays on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
