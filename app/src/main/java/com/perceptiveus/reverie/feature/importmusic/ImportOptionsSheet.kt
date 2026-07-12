package com.perceptiveus.reverie.feature.importmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.data.storage.ImportMode
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage

enum class ImportPickerType {
    FILES,
    FOLDER,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportOptionsSheet(
    pickerType: ImportPickerType,
    storage: MusicLibraryStorage,
    importMode: ImportMode,
    destinationRelativePath: String,
    onPickerTypeChange: (ImportPickerType) -> Unit,
    onImportModeChange: (ImportMode) -> Unit,
    onPickDestination: () -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Import Music", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Choose what to import, then copy or move it into your Reverie library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(text = "Import type", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ImportPickerType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = pickerType == type,
                        onClick = { onPickerTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ImportPickerType.entries.size,
                        ),
                    ) {
                        Text(
                            when (type) {
                                ImportPickerType.FILES -> "Files"
                                ImportPickerType.FOLDER -> "Folder"
                            },
                        )
                    }
                }
            }
            Text(
                text = when (pickerType) {
                    ImportPickerType.FILES -> "Pick one or more songs."
                    ImportPickerType.FOLDER -> "Pick a folder — Reverie keeps your subfolders."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(text = "Import mode", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ImportMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = importMode == mode,
                        onClick = { onImportModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ImportMode.entries.size,
                        ),
                    ) {
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Destination", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = storage.destinationLabel(destinationRelativePath),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onPickDestination, modifier = Modifier.fillMaxWidth()) {
                    Text("Change destination")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (pickerType) {
                            ImportPickerType.FILES -> "Choose files"
                            ImportPickerType.FOLDER -> "Choose folder"
                        },
                    )
                }
            }
        }
    }
}
