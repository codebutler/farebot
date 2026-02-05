package com.codebutler.farebot.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.savedstate.read
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codebutler.farebot.base.util.StringResource
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.*
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.platform.PlatformActions
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.ImportResult
import com.codebutler.farebot.shared.ui.navigation.Screen
import com.codebutler.farebot.shared.ui.screen.AddKeyScreen
import com.codebutler.farebot.shared.ui.screen.AdvancedTab
import com.codebutler.farebot.shared.ui.screen.CardAdvancedScreen
import com.codebutler.farebot.shared.ui.screen.CardAdvancedUiState
import com.codebutler.farebot.shared.ui.screen.CardScreen
import com.codebutler.farebot.shared.ui.screen.HelpScreen
import com.codebutler.farebot.shared.ui.screen.HistoryScreen
import com.codebutler.farebot.shared.ui.screen.HomeScreen
import com.codebutler.farebot.shared.ui.screen.KeysScreen
import com.codebutler.farebot.shared.ui.screen.SettingsScreen
import com.codebutler.farebot.shared.ui.screen.SupportedCardInfo
import com.codebutler.farebot.shared.ui.screen.TripMapScreen
import com.codebutler.farebot.shared.ui.screen.TripMapUiState
import com.codebutler.farebot.shared.ui.theme.FareBotTheme
import com.codebutler.farebot.shared.viewmodel.AddKeyViewModel
import com.codebutler.farebot.shared.viewmodel.CardViewModel
import com.codebutler.farebot.shared.viewmodel.HistoryViewModel
import com.codebutler.farebot.shared.viewmodel.HomeViewModel
import com.codebutler.farebot.shared.viewmodel.KeysViewModel
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import org.koin.compose.koinInject
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel

/**
 * FareBot app entry point using Koin DI and shared ViewModels.
 * Used by both Android and iOS platforms.
 */
@Composable
fun FareBotApp(
    platformActions: PlatformActions,
    supportedCards: List<SupportedCardInfo> = emptyList(),
    isMifareClassicSupported: Boolean = false,
    onNavigateToPrefs: (() -> Unit)? = null,
) {
    FareBotTheme {
        val navController = rememberNavController()
        val navDataHolder = koinInject<NavDataHolder>()
        val stringResource = koinInject<StringResource>()
        val cardImporter = koinInject<CardImporter>()
        val cardPersister = koinInject<CardPersister>()
        val cardSerializer = koinInject<CardSerializer>()

        LaunchedEffect(Unit) {
            cardImporter.pendingImport.collect { content ->
                when (val result = cardImporter.importCards(content)) {
                    is ImportResult.Success -> {
                        for (rawCard in result.cards) {
                            cardPersister.insertCard(
                                SavedCard(
                                    type = rawCard.cardType(),
                                    serial = rawCard.tagId().hex(),
                                    data = cardSerializer.serialize(rawCard),
                                )
                            )
                        }
                        if (result.cards.size == 1) {
                            val rawCard = result.cards.first()
                            val navKey = navDataHolder.put(rawCard)
                            navController.navigate(Screen.Card.createRoute(navKey))
                        }
                        if (result.cards.size > 1) {
                            platformActions.showToast(getString(Res.string.imported_cards, result.cards.size))
                        }
                    }
                    is ImportResult.Error -> {
                        platformActions.showToast(getString(Res.string.import_failed, result.message))
                    }
                }
            }
        }

        NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                val viewModel = koinViewModel<HomeViewModel>()
                val uiState by viewModel.uiState.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.startObserving()
                }

                LaunchedEffect(Unit) {
                    viewModel.navigateToCard.collect { cardKey ->
                        navController.navigate(Screen.Card.createRoute(cardKey))
                    }
                }

                HomeScreen(
                    uiState = uiState,
                    errorMessage = errorMessage,
                    onDismissError = { viewModel.dismissError() },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                    onNavigateToKeys = { navController.navigate(Screen.Keys.route) },
                    onNavigateToPrefs = when {
                        onNavigateToPrefs != null -> onNavigateToPrefs
                        else -> { { navController.navigate(Screen.Settings.route) } }
                    },
                    onOpenAbout = { platformActions.openUrl("https://codebutler.github.io/farebot") },
                    onOpenNfcSettings = { platformActions.openNfcSettings() },
                    onScanCard = { viewModel.startActiveScan() },
                    onEmitSample = { viewModel.emitSampleCard() },
                )
            }

            composable(Screen.Help.route) {
                HelpScreen(
                    supportedCards = supportedCards,
                    isMifareClassicSupported = isMifareClassicSupported,
                    onBack = { navController.popBackStack() },
                    onKeysRequiredTap = {
                        platformActions.showToast(runBlocking { getString(Res.string.keys_required) })
                    },
                )
            }

            composable(Screen.History.route) {
                val viewModel = koinViewModel<HistoryViewModel>()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.loadCards()
                }

                LaunchedEffect(Unit) {
                    viewModel.navigateToCard.collect { cardKey ->
                        navController.navigate(Screen.Card.createRoute(cardKey))
                    }
                }

                HistoryScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNavigateToCard = { itemId ->
                        val cardKey = viewModel.getCardNavKey(itemId)
                        if (cardKey != null) {
                            navController.navigate(Screen.Card.createRoute(cardKey))
                        }
                    },
                    onImportFile = {
                        platformActions.pickFileForImport { text ->
                            if (text != null) {
                                val count = viewModel.importCards(text)
                                platformActions.showToast(runBlocking { getString(Res.string.imported_cards, count) })
                                viewModel.loadCards()
                            }
                        }
                    },
                    onImportClipboard = {
                        val text = platformActions.getClipboardText()
                        if (text != null) {
                            val count = viewModel.importCards(text)
                            platformActions.showToast(runBlocking { getString(Res.string.imported_cards, count) })
                            viewModel.loadCards()
                        }
                    },
                    onExportShare = {
                        val json = viewModel.exportCards()
                        platformActions.shareText(json)
                    },
                    onExportSave = {
                        val json = viewModel.exportCards()
                        platformActions.saveFileForExport(json, "farebot-export.json")
                    },
                    onDeleteItem = { itemId -> viewModel.deleteItem(itemId) },
                    onToggleSelection = { itemId -> viewModel.toggleSelection(itemId) },
                    onClearSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { viewModel.deleteSelected() },
                )
            }

            composable(Screen.Keys.route) {
                val viewModel = koinViewModel<KeysViewModel>()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.loadKeys()
                }

                KeysScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNavigateToAddKey = { navController.navigate(Screen.AddKey.route) },
                    onDeleteKey = { keyId -> viewModel.deleteKey(keyId) },
                    onToggleSelection = { keyId -> viewModel.toggleSelection(keyId) },
                    onClearSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { viewModel.deleteSelected() },
                )
            }

            composable(Screen.AddKey.route) {
                val viewModel = koinViewModel<AddKeyViewModel>()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.startObservingTags()
                }

                LaunchedEffect(Unit) {
                    viewModel.keySaved.collect {
                        navController.popBackStack()
                    }
                }

                AddKeyScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onSaveKey = { cardId, cardType, keyData ->
                        viewModel.saveKey(cardId, cardType, keyData)
                    },
                    onEnterManually = { viewModel.enterManualMode() },
                    onImportFile = {
                        platformActions.pickFileForBytes { bytes ->
                            if (bytes != null) {
                                viewModel.importKeyFile(bytes)
                            }
                        }
                    },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Card.route,
                arguments = listOf(navArgument("cardKey") { type = NavType.StringType })
            ) { backStackEntry ->
                val cardKey = backStackEntry.arguments?.read { getStringOrNull("cardKey") } ?: return@composable
                val viewModel = koinViewModel<CardViewModel>()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(cardKey) {
                    viewModel.loadCard(cardKey)
                }

                CardScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNavigateToAdvanced = {
                        val advKey = viewModel.getAdvancedCardKey()
                        if (advKey != null) {
                            navController.navigate(Screen.CardAdvanced.createRoute(advKey))
                        }
                    },
                    onNavigateToTripMap = { tripKey ->
                        navController.navigate(Screen.TripMap.createRoute(tripKey))
                    },
                    onExportShare = {
                        val json = viewModel.exportCard()
                        if (json != null) {
                            platformActions.shareText(json)
                        }
                    },
                    onExportSave = {
                        val json = viewModel.exportCard()
                        if (json != null) {
                            platformActions.saveFileForExport(json, "farebot-card.json")
                        }
                    },
                )
            }

            composable(
                route = Screen.CardAdvanced.route,
                arguments = listOf(navArgument("cardKey") { type = NavType.StringType })
            ) { backStackEntry ->
                val cardKey = backStackEntry.arguments?.read { getStringOrNull("cardKey") } ?: return@composable

                @Suppress("UNCHECKED_CAST")
                val data = remember { navDataHolder.get<Pair<Card, TransitInfo?>>(cardKey) }
                val card = data?.first
                val transitInfo = data?.second

                val tabs = remember {
                    val tabList = mutableListOf<AdvancedTab>()
                    if (transitInfo != null) {
                        val transitInfoUi = transitInfo.getAdvancedUi(stringResource)
                        if (transitInfoUi != null) {
                            tabList.add(AdvancedTab(transitInfo.cardName, transitInfoUi))
                        }
                    }
                    if (card != null) {
                        tabList.add(AdvancedTab(card.cardType.toString(), card.getAdvancedUi(stringResource)))
                    }
                    tabList
                }

                CardAdvancedScreen(
                    uiState = CardAdvancedUiState(tabs = tabs),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.TripMap.route,
                arguments = listOf(navArgument("tripKey") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripKey = backStackEntry.arguments?.read { getStringOrNull("tripKey") } ?: return@composable

                val trip = remember { navDataHolder.get<Trip>(tripKey) }
                val uiState = remember {
                    TripMapUiState(
                        startStation = trip?.startStation,
                        endStation = trip?.endStation,
                        routeName = trip?.routeName,
                        agencyName = trip?.agencyName,
                    )
                }

                TripMapScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
