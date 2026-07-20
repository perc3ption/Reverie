package com.perceptiveus.reverie.feature.audiofx

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.design.components.SectionHeader
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.feature.premium.PremiumLockedScreen
import com.perceptiveus.reverie.feature.premium.UpgradeDialog
import com.perceptiveus.reverie.playback.audiofx.AudioFxPresets
import com.perceptiveus.reverie.playback.audiofx.AudioFxSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFxScreen(
    viewModel: AudioFxViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()
    val canAccess = viewModel.canAccess()
    var showUpgrade by remember { mutableStateOf(false) }
    var showRestoreDefaults by remember { mutableStateOf(false) }

    if (showUpgrade) {
        UpgradeDialog(
            feature = AppFeature.AUDIO_FX,
            onDismiss = { showUpgrade = false },
            onUpgradeClick = {
                showUpgrade = false
                onNavigateToPremium()
            },
        )
    }

    if (showRestoreDefaults) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaults = false },
            title = { Text("Restore defaults?") },
            text = {
                Text(
                    "This resets EQ, bass boost, loudness, crossfade, and gapless to their original settings.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreDefaults()
                        showRestoreDefaults = false
                    },
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDefaults = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Audio FX") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canAccess) {
                        TextButton(onClick = { showRestoreDefaults = true }) {
                            Icon(
                                imageVector = Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            Text("Restore")
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!canAccess) {
            PremiumLockedScreen(
                feature = AppFeature.AUDIO_FX,
                onUpgradeClick = { showUpgrade = true },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionHeader(title = "Equalizer")
            }
            item {
                SettingsToggleRow(
                    title = "Enable EQ",
                    subtitle = "Shape tone with bands, preamp, and bass boost",
                    checked = settings.eqEnabled,
                    onCheckedChange = viewModel::setEqEnabled,
                )
            }
            item {
                PresetRow(
                    selectedId = settings.presetId,
                    onSelect = viewModel::applyPreset,
                )
            }
            item {
                TextButton(
                    onClick = viewModel::resetFlat,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text("Reset EQ to Flat")
                }
            }
            item {
                GainSliderCard(
                    title = "Bass Boost",
                    valueLabel = "${settings.bassBoostDb.roundToInt()} dB",
                    value = settings.bassBoostDb,
                    valueRange = 0f..AudioFxSettings.MAX_BASS_BOOST_DB,
                    enabled = settings.eqEnabled,
                    onValueChange = viewModel::setBassBoost,
                )
            }
            item {
                GainSliderCard(
                    title = "Preamp",
                    valueLabel = formatDb(settings.preampDb),
                    value = settings.preampDb,
                    valueRange = AudioFxSettings.MIN_GAIN_DB..AudioFxSettings.MAX_GAIN_DB,
                    enabled = settings.eqEnabled,
                    onValueChange = viewModel::setPreamp,
                )
            }
            item {
                BandEqCard(
                    bandsDb = settings.bandsDb,
                    enabled = settings.eqEnabled,
                    onBandChange = viewModel::setBand,
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Playback Feel")
            }
            item {
                SettingsToggleRow(
                    title = "Loudness Leveling",
                    subtitle = "Soft automatic volume matching across tracks",
                    checked = settings.loudnessEnabled,
                    onCheckedChange = viewModel::setLoudnessEnabled,
                )
            }
            item {
                SettingsToggleRow(
                    title = "Gapless Playback",
                    subtitle = "No pause between tracks (off inserts a short gap)",
                    checked = settings.gaplessEnabled && settings.crossfadeMs == 0,
                    onCheckedChange = viewModel::setGaplessEnabled,
                )
            }
            item {
                GainSliderCard(
                    title = "Crossfade",
                    valueLabel = if (settings.crossfadeMs == 0) {
                        "Off"
                    } else {
                        "${"%.1f".format(settings.crossfadeMs / 1000f)} s"
                    },
                    value = settings.crossfadeMs / 1000f,
                    valueRange = 0f..12f,
                    enabled = true,
                    onValueChange = { seconds ->
                        viewModel.setCrossfadeMs((seconds * 1000f).roundToInt())
                    },
                )
            }
            item {
                Text(
                    text = "Crossfade gently overlaps track changes. Turning it on disables gapless.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showRestoreDefaults = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Restore all defaults")
                }
                Text(
                    text = "Undo every Audio FX change in one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PresetRow(
    selectedId: String,
    onSelect: (com.perceptiveus.reverie.playback.audiofx.AudioFxPreset) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AudioFxPresets.ALL.forEach { preset ->
            FilterChip(
                selected = selectedId == preset.id,
                onClick = { onSelect(preset) },
                label = { Text(preset.label) },
            )
        }
    }
}

@Composable
private fun GainSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun BandEqCard(
    bandsDb: List<Float>,
    enabled: Boolean,
    onBandChange: (Int, Float) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "10-Band EQ",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            bandsDb.forEachIndexed { index, db ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = AudioFxSettings.BAND_LABELS[index],
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                    )
                    Slider(
                        value = db,
                        onValueChange = { onBandChange(index, it) },
                        valueRange = AudioFxSettings.MIN_GAIN_DB..AudioFxSettings.MAX_GAIN_DB,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatDb(db),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

private fun formatDb(db: Float): String {
    val rounded = (db * 10f).roundToInt() / 10f
    return if (rounded > 0f) "+${rounded} dB" else "${rounded} dB"
}
