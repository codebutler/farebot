package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.copy
import farebot.farebot_shared.generated.resources.delete
import farebot.farebot_shared.generated.resources.history
import farebot.farebot_shared.generated.resources.import_clipboard
import farebot.farebot_shared.generated.resources.import_file
import farebot.farebot_shared.generated.resources.no_scanned_cards
import farebot.farebot_shared.generated.resources.save
import farebot.farebot_shared.generated.resources.share
import farebot.farebot_shared.generated.resources.unknown_card
import farebot.farebot_shared.generated.resources.back
import farebot.farebot_shared.generated.resources.cancel
import farebot.farebot_shared.generated.resources.delete_selected_cards
import farebot.farebot_shared.generated.resources.menu
import farebot.farebot_shared.generated.resources.n_selected
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onNavigateToCard: (String) -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit,
    onExportShare: () -> Unit,
    onExportSave: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.delete)) },
            text = { Text(stringResource(Res.string.delete_selected_cards, uiState.selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDeleteSelected()
                }) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.n_selected, uiState.selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(Res.string.history)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.menu))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.import_file)) },
                                onClick = { menuExpanded = false; onImportFile() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.import_clipboard)) },
                                onClick = { menuExpanded = false; onImportClipboard() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.share)) },
                                onClick = { menuExpanded = false; onExportShare() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.save)) },
                                onClick = { menuExpanded = false; onExportSave() }
                            )
                        }
                    },
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.items.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.no_scanned_cards),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.items) { item ->
                            val isSelected = uiState.selectedIds.contains(item.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                onToggleSelection(item.id)
                                            } else {
                                                onNavigateToCard(item.id)
                                            }
                                        },
                                        onLongClick = {
                                            onToggleSelection(item.id)
                                        },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelection(item.id) },
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.cardName ?: stringResource(Res.string.unknown_card),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = item.serial,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.scannedAt != null) {
                                        Text(
                                            text = item.scannedAt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (item.parseError != null) {
                                        Text(
                                            text = item.parseError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                if (!uiState.isSelectionMode) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { onDeleteItem(item.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(Res.string.delete),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
