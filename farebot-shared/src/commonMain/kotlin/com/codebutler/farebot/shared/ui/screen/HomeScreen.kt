package com.codebutler.farebot.shared.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.shared.platform.NfcStatus
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.about
import farebot.farebot_shared.generated.resources.app_name
import farebot.farebot_shared.generated.resources.history
import farebot.farebot_shared.generated.resources.img_home_splash
import farebot.farebot_shared.generated.resources.keys
import farebot.farebot_shared.generated.resources.nfc_disabled
import farebot.farebot_shared.generated.resources.nfc_settings
import farebot.farebot_shared.generated.resources.nfc_unavailable
import farebot.farebot_shared.generated.resources.ok
import farebot.farebot_shared.generated.resources.preferences
import farebot.farebot_shared.generated.resources.scan_card
import farebot.farebot_shared.generated.resources.scan_now
import farebot.farebot_shared.generated.resources.supported_cards
import farebot.farebot_shared.generated.resources.back
import farebot.farebot_shared.generated.resources.menu
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    errorMessage: Pair<String, String>?,
    onDismissError: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToKeys: (() -> Unit)? = null,
    onNavigateToPrefs: (() -> Unit)? = null,
    onOpenAbout: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onScanCard: () -> Unit,
    onEmitSample: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(errorMessage.first) },
            text = { Text(errorMessage.second) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.menu))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.history)) },
                            onClick = { menuExpanded = false; onNavigateToHistory() }
                        )
                        if (onNavigateToKeys != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.keys)) },
                                onClick = { menuExpanded = false; onNavigateToKeys() }
                            )
                        }
                        if (onNavigateToPrefs != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.preferences)) },
                                onClick = { menuExpanded = false; onNavigateToPrefs() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.about)) },
                            onClick = { menuExpanded = false; onOpenAbout() }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (uiState.nfcStatus) {
                NfcStatus.UNAVAILABLE -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.nfc_unavailable),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                NfcStatus.DISABLED -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.img_home_splash),
                            contentDescription = stringResource(Res.string.app_name),
                            modifier = Modifier.size(200.dp),
                            alpha = 0.3f,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.nfc_disabled),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = onOpenNfcSettings) {
                                    Text(stringResource(Res.string.nfc_settings))
                                }
                            }
                        }
                    }
                }
                NfcStatus.AVAILABLE -> {
                    Crossfade(targetState = uiState.isLoading) { isLoading ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.img_home_splash),
                                        contentDescription = stringResource(Res.string.app_name),
                                        modifier = Modifier
                                            .size(200.dp)
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = onEmitSample,
                                            ),

                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(Res.string.scan_card),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(onClick = onScanCard) {
                                        Text(stringResource(Res.string.scan_now))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = onNavigateToHelp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.supported_cards),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
