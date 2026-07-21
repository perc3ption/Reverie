package com.perceptiveus.reverie.feature.smartplaylist

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perceptiveus.reverie.core.design.components.GlassSurface
import com.perceptiveus.reverie.core.design.components.RetroScreenTitle
import com.perceptiveus.reverie.domain.model.SmartPlaylistField
import com.perceptiveus.reverie.domain.model.SmartPlaylistOperator
import com.perceptiveus.reverie.domain.model.SmartPlaylistSort
import com.perceptiveus.reverie.domain.model.Tag
import com.perceptiveus.reverie.domain.model.displayName
import com.perceptiveus.reverie.domain.model.operatorsFor
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistEditorScreen(
    viewModel: SmartPlaylistEditorViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (playlistId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name by viewModel.name.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val trackLimit by viewModel.trackLimit.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val tags by viewModel.availableTags.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.userMessages.collectLatest { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.savedPlaylistId.collectLatest(onSaved)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    RetroScreenTitle(
                        title = if (viewModel.isEditing) "Edit Smart Playlist" else "New Smart Playlist",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                OutlinedTextField(
                    value = name,
                    onValueChange = viewModel::setName,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                EnumDropdown(
                    label = "Sort by",
                    selected = sortOrder,
                    options = SmartPlaylistSort.entries,
                    optionLabel = { it.displayName() },
                    onSelected = viewModel::setSortOrder,
                )
            }
            item {
                OutlinedTextField(
                    value = trackLimit,
                    onValueChange = viewModel::setTrackLimit,
                    label = { Text("Track limit (1–500)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = "Rules (all must match)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(rules, key = { it.key }) { rule ->
                RuleEditorCard(
                    rule = rule,
                    tags = tags,
                    onFieldChange = { viewModel.setField(rule.key, it) },
                    onOperatorChange = { op ->
                        viewModel.updateRule(rule.key) { it.copy(operator = op) }
                    },
                    onValueChange = { value ->
                        viewModel.updateRule(rule.key) { it.copy(value = value) }
                    },
                    onSecondaryChange = { value ->
                        viewModel.updateRule(rule.key) { it.copy(valueSecondary = value) }
                    },
                    onRemove = { viewModel.removeRule(rule.key) },
                )
            }
            item {
                OutlinedButton(
                    onClick = viewModel::addRule,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add rule")
                }
            }
            item {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (viewModel.isEditing) "Save changes" else "Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditorCard(
    rule: EditableSmartRule,
    tags: List<Tag>,
    onFieldChange: (SmartPlaylistField) -> Unit,
    onOperatorChange: (SmartPlaylistOperator) -> Unit,
    onValueChange: (String) -> Unit,
    onSecondaryChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        emphasized = true,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Rule",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove rule")
                }
            }
            EnumDropdown(
                label = "Field",
                selected = rule.field,
                options = SmartPlaylistField.entries,
                optionLabel = { it.displayName() },
                onSelected = onFieldChange,
            )
            EnumDropdown(
                label = "Operator",
                selected = rule.operator,
                options = operatorsFor(rule.field),
                optionLabel = { it.displayName() },
                onSelected = onOperatorChange,
            )
            when (rule.field) {
                SmartPlaylistField.TAG -> {
                    var tagExpanded by remember { mutableStateOf(false) }
                    val selectedTag = tags.firstOrNull {
                        it.id == rule.value || it.name.equals(rule.value, ignoreCase = true)
                    }
                    ExposedDropdownMenuBox(
                        expanded = tagExpanded,
                        onExpandedChange = { if (tags.isNotEmpty()) tagExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedTag?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = tags.isNotEmpty(),
                            label = { Text("Tag") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false },
                        ) {
                            tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag.name) },
                                    onClick = {
                                        onValueChange(tag.id)
                                        tagExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (tags.isEmpty()) {
                        Text(
                            text = "Add tags on Song Detail first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SmartPlaylistField.YEAR -> {
                    OutlinedTextField(
                        value = rule.value,
                        onValueChange = onValueChange,
                        label = {
                            Text(if (rule.operator == SmartPlaylistOperator.BETWEEN) "From year" else "Year")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (rule.operator == SmartPlaylistOperator.BETWEEN) {
                        OutlinedTextField(
                            value = rule.valueSecondary,
                            onValueChange = onSecondaryChange,
                            label = { Text("To year") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                SmartPlaylistField.RATING -> {
                    OutlinedTextField(
                        value = rule.value,
                        onValueChange = { onValueChange(it.filter(Char::isDigit).take(1)) },
                        label = { Text("Stars (0–5)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SmartPlaylistField.DATE_ADDED,
                SmartPlaylistField.RECENTLY_PLAYED,
                -> {
                    OutlinedTextField(
                        value = rule.value,
                        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
                        label = { Text("Days") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SmartPlaylistField.PLAY_COUNT -> {
                    OutlinedTextField(
                        value = rule.value,
                        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
                        label = { Text("Minimum plays") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = rule.value,
                        onValueChange = onValueChange,
                        label = { Text("Value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
