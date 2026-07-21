package com.perceptiveus.reverie.feature.importmusic

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.import.SupportedAudioFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMusicScreen(
    viewModel: ImportMusicViewModel,
    onNavigateBack: () -> Unit,
) {
    val songCount by viewModel.songCount.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val importMode by viewModel.importMode.collectAsState()
    val destinationRelativePath by viewModel.destinationRelativePath.collectAsState()
    val showImportOptions by viewModel.showImportOptions.collectAsState()
    val pickerType by viewModel.pickerType.collectAsState()
    val showDestinationPicker by viewModel.showDestinationPicker.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current

    val openSongPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        viewModel.importSongUris(uris)
    }

    val openFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) {
                // Provider may not support persistable permissions.
            }
            viewModel.importFolderUri(uri)
        } else {
            viewModel.dismissImportOptions()
        }
    }

    if (showDestinationPicker) {
        DestinationPickerDialog(
            storage = viewModel.storage,
            selectedRelativePath = destinationRelativePath,
            onDismiss = viewModel::dismissDestinationPicker,
            onSelect = viewModel::selectDestination,
            onCreateFolder = viewModel::createDestinationAndSelect,
        )
    }

    if (showImportOptions) {
        ImportOptionsSheet(
            pickerType = pickerType,
            storage = viewModel.storage,
            importMode = importMode,
            destinationRelativePath = destinationRelativePath,
            onPickerTypeChange = viewModel::setPickerType,
            onImportModeChange = viewModel::setImportMode,
            onPickDestination = viewModel::openDestinationPicker,
            onDismiss = viewModel::dismissImportOptions,
            onContinue = {
                viewModel.dismissImportOptions()
                when (pickerType) {
                    ImportPickerType.FILES -> openSongPicker.launch(arrayOf("audio/*", "application/ogg"))
                    ImportPickerType.FOLDER -> openFolderPicker.launch(null)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { RetroScreenTitle(title = "Import") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Import Your Music",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reverie only adds music you choose. " +
                    "We never scan your entire device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPremium) {
                    "Library: $songCount songs"
                } else {
                    "Library: $songCount / ${FeatureAccessChecker.FREE_MAX_SONGS} songs (free tier)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(20.dp))
            SupportedFormatsPanel(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::openImportOptions,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Import Music")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose files or a folder, then copy or move into your Reverie library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = viewModel::scanLibrary,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null)
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text("Scan Library")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "After copying music into the Reverie folder via USB, tap Scan Library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            statusMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportedFormatsPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Supported formats",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SupportedAudioFormats.IMPORT_FORMAT_LABELS.forEach { label ->
                    val limited = label.equals("WMA", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (limited) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        },
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (limited) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
            Text(
                text = SupportedAudioFormats.WMA_PLAYBACK_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
