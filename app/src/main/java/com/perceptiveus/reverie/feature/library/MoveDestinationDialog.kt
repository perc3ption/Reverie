package com.perceptiveus.reverie.feature.library

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MoveDestinationDialog(
    destinations: List<LibraryViewModel.MoveDestination>,
    onDismiss: () -> Unit,
    onConfirm: (relativePath: String) -> Unit,
) {
    var selectedPath by remember(destinations) {
        mutableStateOf(destinations.firstOrNull()?.relativePath.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to") },
        text = {
            Column {
                Text(
                    text = "Choose a folder in your Reverie library.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (destinations.isEmpty()) {
                    Text(
                        text = "No valid destinations for this selection.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .padding(top = 8.dp),
                    ) {
                        items(destinations, key = { it.relativePath.ifEmpty { "_root" } }) { dest ->
                            val selected = dest.relativePath == selectedPath
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPath = dest.relativePath },
                                headlineContent = {
                                    Text(dest.label)
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
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedPath) },
                enabled = destinations.isNotEmpty(),
            ) {
                Text("Move here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
