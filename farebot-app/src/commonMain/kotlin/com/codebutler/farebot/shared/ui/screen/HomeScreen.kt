package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import farebot.farebot_app.generated.resources.search_supported_cards
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.platform.AppPreferences
import com.codebutler.farebot.shared.platform.NfcStatus
import com.codebutler.farebot.shared.viewmodel.ScanError
import com.codebutler.farebot.transit.CardInfo
import farebot.farebot_app.generated.resources.Res
import farebot.farebot_app.generated.resources.ic_cards_stack
import farebot.farebot_app.generated.resources.ic_launcher
import farebot.farebot_app.generated.resources.about
import farebot.farebot_app.generated.resources.add_key
import farebot.farebot_app.generated.resources.app_name
import farebot.farebot_app.generated.resources.cancel
import farebot.farebot_app.generated.resources.delete
import farebot.farebot_app.generated.resources.delete_selected_cards
import farebot.farebot_app.generated.resources.import_file
import farebot.farebot_app.generated.resources.keys
import farebot.farebot_app.generated.resources.menu
import farebot.farebot_app.generated.resources.n_selected
import farebot.farebot_app.generated.resources.nfc_disabled
import farebot.farebot_app.generated.resources.nfc_listening
import farebot.farebot_app.generated.resources.nfc_settings
import farebot.farebot_app.generated.resources.ok
import farebot.farebot_app.generated.resources.scan
import farebot.farebot_app.generated.resources.show_all_scans
import farebot.farebot_app.generated.resources.show_keys_required_cards
import farebot.farebot_app.generated.resources.show_serial_only_cards
import farebot.farebot_app.generated.resources.show_unsupported_cards
import farebot.farebot_app.generated.resources.tab_explore
import farebot.farebot_app.generated.resources.tab_scan
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeUiState: HomeUiState,
    errorMessage: ScanError?,
    onDismissError: () -> Unit,
    onNavigateToAddKeyForCard: (tagId: String, cardType: CardType) -> Unit,
    onScanCard: () -> Unit,
    historyUiState: HistoryUiState,
    onNavigateToCard: (String) -> Unit,
    onImportFile: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    supportedCards: List<CardInfo>,
    supportedCardTypes: Set<CardType>,
    deviceRegion: String?,
    loadedKeyBundles: Set<String>,
    mapMarkers: List<CardsMapMarker>,
    onKeysRequiredTap: () -> Unit,
    onNavigateToKeys: (() -> Unit)?,
    onOpenAbout: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onSampleCardTap: ((CardInfo) -> Unit)? = null,
    onToggleShowAllScans: () -> Unit = {},
) {
    val appPreferences = koinInject<AppPreferences>()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showUnsupported by rememberSaveable {
        mutableStateOf(appPreferences.getBoolean(AppPreferences.KEY_SHOW_UNSUPPORTED, false))
    }
    var showSerialOnly by rememberSaveable {
        mutableStateOf(appPreferences.getBoolean(AppPreferences.KEY_SHOW_SERIAL_ONLY, false))
    }
    var showKeysRequired by rememberSaveable {
        mutableStateOf(appPreferences.getBoolean(AppPreferences.KEY_SHOW_KEYS_REQUIRED, false))
    }
    var exploreSearchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val hasUnsupportedCards = remember(supportedCards, supportedCardTypes) {
        supportedCards.any { it.cardType !in supportedCardTypes }
    }


    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(errorMessage.title) },
            text = { Text(errorMessage.message) },
            confirmButton = {
                if (errorMessage.tagIdHex != null && errorMessage.cardType != null) {
                    TextButton(onClick = {
                        val tagId = errorMessage.tagIdHex
                        val cardType = errorMessage.cardType
                        onDismissError()
                        onNavigateToAddKeyForCard(tagId, cardType)
                    }) {
                        Text(stringResource(Res.string.add_key))
                    }
                } else {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            },
            dismissButton = if (errorMessage.tagIdHex != null) {
                {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            } else null,
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.delete)) },
            text = { Text(stringResource(Res.string.delete_selected_cards, historyUiState.selectedIds.size)) },
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (selectedTab == 0 && historyUiState.isSelectionMode) {
                // Scan tab — selection mode
                TopAppBar(
                    title = { Text(stringResource(Res.string.n_selected, historyUiState.selectedIds.size)) },
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
            } else if (selectedTab == 0) {
                // Scan tab — normal mode
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(Res.drawable.ic_launcher),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.app_name))
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
                                text = { Text(stringResource(Res.string.show_all_scans)) },
                                trailingIcon = if (historyUiState.showAllScans) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                onClick = { onToggleShowAllScans(); menuExpanded = false },
                            )
                            if (onNavigateToKeys != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.keys)) },
                                    onClick = { menuExpanded = false; onNavigateToKeys() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.about)) },
                                onClick = { menuExpanded = false; onOpenAbout() }
                            )
                        }
                    }
                )
            } else {
                // Explore tab — search bar in place of title
                TopAppBar(
                    title = {
                        TextField(
                            value = exploreSearchQuery,
                            onValueChange = { exploreSearchQuery = it },
                            placeholder = { Text(stringResource(Res.string.search_supported_cards)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    ),
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.menu))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (hasUnsupportedCards) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.show_unsupported_cards)) },
                                    trailingIcon = if (showUnsupported) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null,
                                    onClick = {
                                    showUnsupported = !showUnsupported
                                    appPreferences.putBoolean(AppPreferences.KEY_SHOW_UNSUPPORTED, showUnsupported)
                                    menuExpanded = false
                                },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.show_serial_only_cards)) },
                                trailingIcon = if (showSerialOnly) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    showSerialOnly = !showSerialOnly
                                    appPreferences.putBoolean(AppPreferences.KEY_SHOW_SERIAL_ONLY, showSerialOnly)
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.show_keys_required_cards)) },
                                trailingIcon = if (showKeysRequired) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                onClick = {
                                    showKeysRequired = !showKeysRequired
                                    appPreferences.putBoolean(AppPreferences.KEY_SHOW_KEYS_REQUIRED, showKeysRequired)
                                    menuExpanded = false
                                },
                            )
                            if (onNavigateToKeys != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.keys)) },
                                    onClick = { menuExpanded = false; onNavigateToKeys() }
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
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(painterResource(Res.drawable.ic_cards_stack), contentDescription = null) },
                    label = { Text(stringResource(Res.string.tab_scan)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text(stringResource(Res.string.tab_explore)) },
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && homeUiState.requiresActiveScan && homeUiState.nfcStatus != NfcStatus.UNAVAILABLE) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (homeUiState.nfcStatus == NfcStatus.DISABLED) {
                            onOpenNfcSettings()
                        } else {
                            onScanCard()
                        }
                    },
                    icon = {
                        if (homeUiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Nfc, contentDescription = null)
                        }
                    },
                    text = { Text(stringResource(Res.string.scan)) },
                )
            }
        },
    ) { padding ->
        // On the Explore tab, skip top padding so the map extends behind the translucent top bar
        val isExploreTab = selectedTab == 1
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (isExploreTab) 0.dp else padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                )
        ) {
            // NFC disabled banner (scan tab only)
            if (selectedTab == 0 && homeUiState.nfcStatus == NfcStatus.DISABLED) {
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

            // NFC listening banner (Android passive scanning, scan tab only)
            if (selectedTab == 0 && !homeUiState.requiresActiveScan && homeUiState.nfcStatus == NfcStatus.AVAILABLE) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Nfc,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.nfc_listening),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            // Tab content — dismiss keyboard on any touch
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        focusManager.clearFocus()
                    }
                }
            }) {
                when (selectedTab) {
                    0 -> HistoryContent(
                        uiState = historyUiState,
                        onNavigateToCard = onNavigateToCard,
                        onToggleSelection = onToggleSelection,
                    )
                    1 -> ExploreContent(
                        supportedCards = supportedCards,
                        supportedCardTypes = supportedCardTypes,
                        deviceRegion = deviceRegion,
                        loadedKeyBundles = loadedKeyBundles,
                        showUnsupported = showUnsupported,
                        showSerialOnly = showSerialOnly,
                        showKeysRequired = showKeysRequired,
                        onKeysRequiredTap = onKeysRequiredTap,
                        mapMarkers = mapMarkers,
                        onSampleCardTap = onSampleCardTap,
                        searchQuery = exploreSearchQuery,
                        topBarHeight = padding.calculateTopPadding(),
                    )
                }
            }
        }
    }
}
