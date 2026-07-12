package com.perceptiveus.reverie.feature.importmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage

@Composable
fun DestinationPickerDialog(
    storage: MusicLibraryStorage,
    selectedRelativePath: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
) {
    val destinations = remember { storage.listAllSubdirectoryPaths() }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose destination") },
        text = {
            Column {
                Text(
                    text = "Imported files will be placed inside your Reverie library folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(top = 8.dp),
                ) {
                    items(destinations) { path ->
                        val selected = path == selectedRelativePath
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(path) },
                            headlineContent = {
                                Text(storage.destinationLabel(path))
                            },
                            leadingContent = {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            },
                            trailingContent = {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("Create subfolder") },
                    placeholder = { Text("e.g. Rock Classics") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = newFolderName.trim()
                    if (trimmed.isNotEmpty()) {
                        val relativePath = if (selectedRelativePath.isEmpty()) {
                            trimmed
                        } else {
                            "$selectedRelativePath/$trimmed"
                        }
                        onCreateFolder(relativePath)
                    } else {
                        onSelect(selectedRelativePath)
                    }
                },
            ) {
                Text("Use destination")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
