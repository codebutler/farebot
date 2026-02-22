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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import com.codebutler.farebot.shared.ui.layout.ContentWidthConstraint
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.add_key
import farebot.app.generated.resources.back
import farebot.app.generated.resources.cancel
import farebot.app.generated.resources.delete
import farebot.app.generated.resources.delete_selected_keys
import farebot.app.generated.resources.keys
import farebot.app.generated.resources.n_selected
import farebot.app.generated.resources.no_keys
import farebot.app.generated.resources.select_all
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KeysScreen(
    uiState: KeysUiState,
    onBack: () -> Unit,
    onNavigateToAddKey: () -> Unit,
    onDeleteKey: (String) -> Unit,
    onToggleSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.delete)) },
            text = { Text(stringResource(Res.string.delete_selected_keys, uiState.selectedIds.size)) },
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
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(Res.string.select_all))
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(Res.string.keys)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToAddKey) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_key))
                        }
                    },
                )
            }
        },
    ) { padding ->
        ContentWidthConstraint {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.keys.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.no_keys),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.keys) { keyItem ->
                                val isSelected = uiState.selectedIds.contains(keyItem.id)
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (uiState.isSelectionMode) {
                                                        onToggleSelection(keyItem.id)
                                                    }
                                                },
                                                onLongClick = {
                                                    onToggleSelection(keyItem.id)
                                                },
                                            ).padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (uiState.isSelectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { onToggleSelection(keyItem.id) },
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = keyItem.cardId,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = keyItem.cardType,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (!uiState.isSelectionMode) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { onDeleteKey(keyItem.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(Res.string.delete),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
