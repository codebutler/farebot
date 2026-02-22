package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.back
import farebot.app.generated.resources.flipper_bytes
import farebot.app.generated.resources.flipper_connecting_message
import farebot.app.generated.resources.flipper_import_keys
import farebot.app.generated.resources.flipper_import_progress
import farebot.app.generated.resources.flipper_import_selected
import farebot.app.generated.resources.flipper_importing
import farebot.app.generated.resources.flipper_no_files
import farebot.app.generated.resources.flipper_retry
import farebot.app.generated.resources.flipper_up
import farebot.app.generated.resources.flipper_zero
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlipperScreen(
    uiState: FlipperUiState,
    onRetry: () -> Unit,
    onNavigateToDirectory: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onImportSelected: () -> Unit,
    onImportKeys: () -> Unit,
    onClearImportMessage: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.importCompleteMessage) {
        val message = uiState.importCompleteMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearImportMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.connectionState) {
                            FlipperConnectionState.Connected ->
                                uiState.deviceInfo["hardware.name"] ?: stringResource(Res.string.flipper_zero)
                            FlipperConnectionState.Connecting -> stringResource(Res.string.flipper_zero)
                            FlipperConnectionState.Disconnected -> stringResource(Res.string.flipper_zero)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {},
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.connectionState) {
                FlipperConnectionState.Disconnected -> {
                    DisconnectedContent(
                        error = uiState.error,
                        onRetry = onRetry,
                    )
                }

                FlipperConnectionState.Connecting -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(Res.string.flipper_connecting_message))
                    }
                }

                FlipperConnectionState.Connected -> {
                    ConnectedContent(
                        uiState = uiState,
                        onNavigateToDirectory = onNavigateToDirectory,
                        onNavigateUp = onNavigateUp,
                        onToggleSelection = onToggleSelection,
                        onImportSelected = onImportSelected,
                        onImportKeys = onImportKeys,
                    )
                }
            }

            if (uiState.importProgress != null) {
                ImportProgressOverlay(uiState.importProgress)
            }
        }
    }
}

@Composable
private fun DisconnectedContent(
    error: String?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(onClick = onRetry) {
            Text(stringResource(Res.string.flipper_retry))
        }
    }
}

@Composable
private fun ConnectedContent(
    uiState: FlipperUiState,
    onNavigateToDirectory: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onImportSelected: () -> Unit,
    onImportKeys: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Breadcrumb path bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.currentPath != "/ext/nfc") {
                TextButton(onClick = onNavigateUp) {
                    Text(stringResource(Res.string.flipper_up))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = uiState.currentPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        HorizontalDivider()

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.files.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.flipper_no_files),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.files) { file ->
                    FileListItem(
                        file = file,
                        isSelected = uiState.selectedFiles.contains(file.path),
                        onTap = {
                            if (file.isDirectory) {
                                onNavigateToDirectory(file.path)
                            } else {
                                onToggleSelection(file.path)
                            }
                        },
                        onToggleSelection = { onToggleSelection(file.path) },
                    )
                    HorizontalDivider()
                }
            }
        }

        // Bottom action bar
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Button(
                onClick = onImportSelected,
                enabled = uiState.selectedFiles.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.flipper_import_selected, uiState.selectedFiles.size))
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onImportKeys,
            ) {
                Text(stringResource(Res.string.flipper_import_keys))
            }
        }
    }
}

@Composable
private fun FileListItem(
    file: FlipperFileItem,
    isSelected: Boolean,
    onTap: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint =
                if (file.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!file.isDirectory && file.size > 0) {
                Text(
                    text = stringResource(Res.string.flipper_bytes, file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!file.isDirectory) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
            )
        }
    }
}

@Composable
private fun ImportProgressOverlay(progress: ImportProgress) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(32.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.flipper_importing, progress.currentFile),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                        stringResource(
                            Res.string.flipper_import_progress,
                            progress.currentIndex + 1,
                            progress.totalFiles,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
