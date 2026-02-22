package com.codebutler.farebot.shared

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.shared.di.LocalAppGraph
import com.codebutler.farebot.shared.di.graphViewModel
import com.codebutler.farebot.shared.platform.PlatformActions
import com.codebutler.farebot.shared.platform.getDeviceRegion
import com.codebutler.farebot.shared.serialize.ImportResult
import com.codebutler.farebot.shared.ui.layout.LocalWindowWidthSizeClass
import com.codebutler.farebot.shared.ui.layout.windowWidthSizeClass
import com.codebutler.farebot.shared.ui.navigation.Screen
import com.codebutler.farebot.shared.ui.screen.AdvancedTab
import com.codebutler.farebot.shared.ui.screen.CardAdvancedScreen
import com.codebutler.farebot.shared.ui.screen.CardAdvancedUiState
import com.codebutler.farebot.shared.ui.screen.CardScreen
import com.codebutler.farebot.shared.ui.screen.CardsMapMarker
import com.codebutler.farebot.shared.ui.screen.FlipperScreen
import com.codebutler.farebot.shared.ui.screen.HomeScreen
import com.codebutler.farebot.shared.ui.screen.TripMapScreen
import com.codebutler.farebot.shared.ui.screen.TripMapUiState
import com.codebutler.farebot.shared.ui.theme.FareBotTheme
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.app.generated.resources.*
import farebot.app.generated.resources.Res
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun FareBotApp(
    platformActions: PlatformActions,
    supportedCardTypes: Set<CardType> = CardType.entries.toSet() - setOf(CardType.MifareClassic, CardType.Vicinity),
    loadedKeyBundles: Set<String> = emptySet(),
    isDebug: Boolean = false,
    menuEvents: kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.emptyFlow(),
    onSelectedTabChanged: (Int) -> Unit = {},
) {
    FareBotTheme {
        BoxWithConstraints {
            val widthSizeClass = windowWidthSizeClass(maxWidth)
            CompositionLocalProvider(LocalWindowWidthSizeClass provides widthSizeClass) {
                val navController = rememberNavController()
                val graph = LocalAppGraph.current
                val navDataHolder = graph.navDataHolder
                val transitFactoryRegistry = graph.transitFactoryRegistry
                val supportedCards = remember { transitFactoryRegistry.allCards }
                val cardImporter = graph.cardImporter
                val cardPersister = graph.cardPersister
                val cardSerializer = graph.cardSerializer
                val scope = rememberCoroutineScope()

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
                                        ),
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

                LaunchedEffect(Unit) {
                    menuEvents.collect { event ->
                        when (event) {
                            "keys" -> graph.keyManagerPlugin?.navigateToKeys(navController)
                        }
                    }
                }

                val historyViewModel = graphViewModel { historyViewModel }
                val flipperViewModel = graphViewModel { flipperViewModel }
                val flipperTransportFactory = graph.flipperTransportFactory

                NavHost(navController = navController, startDestination = Screen.Home.route) {
                    composable(Screen.Home.route) {
                        val homeViewModel = graphViewModel { homeViewModel }
                        val homeUiState by homeViewModel.uiState.collectAsState()
                        val errorMessage by homeViewModel.errorMessage.collectAsState()

                        val historyUiState by historyViewModel.uiState.collectAsState()

                        LaunchedEffect(Unit) {
                            homeViewModel.startObserving()
                        }

                        LaunchedEffect(Unit) {
                            historyViewModel.loadCards()
                        }

                        LaunchedEffect(Unit) {
                            homeViewModel.navigateToCard.collect { cardKey ->
                                navController.navigate(Screen.Card.createRoute(cardKey))
                            }
                        }

                        LaunchedEffect(Unit) {
                            historyViewModel.navigateToCard.collect { cardKey ->
                                navController.navigate(Screen.Card.createRoute(cardKey))
                            }
                        }

                        HomeScreen(
                            homeUiState = homeUiState,
                            errorMessage = errorMessage,
                            onDismissError = { homeViewModel.dismissError() },
                            onNavigateToAddKeyForCard = { tagId, cardType ->
                                graph.keyManagerPlugin?.navigateToAddKey(navController, tagId, cardType)
                            },
                            onScanCard = { homeViewModel.startActiveScan() },
                            historyUiState = historyUiState,
                            onNavigateToCard = { itemId ->
                                val cardKey = historyViewModel.getCardNavKey(itemId)
                                if (cardKey != null) {
                                    val scanIds = historyViewModel.getCardScanIds(itemId)
                                    val scanIdsKey = if (scanIds.size > 1) navDataHolder.put(scanIds) else null
                                    navController.navigate(Screen.Card.createRoute(cardKey, scanIdsKey, itemId))
                                }
                            },
                            onImportFile = {
                                platformActions.pickFileForImport { text ->
                                    if (text != null) {
                                        val result = historyViewModel.importCardsDetailed(text)
                                        when (result) {
                                            is ImportResult.Success -> {
                                                for (rawCard in result.cards) {
                                                    cardPersister.insertCard(
                                                        SavedCard(
                                                            type = rawCard.cardType(),
                                                            serial = rawCard.tagId().hex(),
                                                            data = cardSerializer.serialize(rawCard),
                                                        ),
                                                    )
                                                }
                                                if (result.cards.size == 1) {
                                                    val rawCard = result.cards.first()
                                                    val navKey = navDataHolder.put(rawCard)
                                                    navController.navigate(Screen.Card.createRoute(navKey))
                                                }
                                                scope.launch {
                                                    platformActions.showToast(
                                                        FormattedString(
                                                            Res.string.imported_cards,
                                                            result.cards.size,
                                                        ).resolveAsync(),
                                                    )
                                                }
                                                historyViewModel.loadCards()
                                            }
                                            is ImportResult.Error -> {
                                                scope.launch {
                                                    platformActions.showToast(
                                                        FormattedString(
                                                            Res.string.import_failed,
                                                            result.message,
                                                        ).resolveAsync(),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onToggleSelection = { itemId -> historyViewModel.toggleSelection(itemId) },
                            onClearSelection = { historyViewModel.clearSelection() },
                            onSelectAll = { historyViewModel.selectAll() },
                            onDeleteSelected = { historyViewModel.deleteSelected() },
                            supportedCards = supportedCards,
                            supportedCardTypes = supportedCardTypes,
                            deviceRegion = getDeviceRegion(),
                            loadedKeyBundles = loadedKeyBundles,
                            mapMarkers =
                                supportedCards
                                    .filter { it.latitude != null && it.longitude != null }
                                    .map { card ->
                                        CardsMapMarker(
                                            name = stringResource(card.nameRes),
                                            location = stringResource(card.locationRes),
                                            latitude = card.latitude!!.toDouble(),
                                            longitude = card.longitude!!.toDouble(),
                                        )
                                    },
                            onKeysRequiredTap = {
                                scope.launch {
                                    platformActions.showToast(FormattedString(Res.string.keys_required).resolveAsync())
                                }
                            },
                            onStatusChipTap = { message ->
                                platformActions.showToast(message)
                            },
                            onNavigateToKeys =
                                graph.keyManagerPlugin?.let { plugin ->
                                    { plugin.navigateToKeys(navController) }
                                },
                            onConnectFlipperBle =
                                if (flipperTransportFactory.isBleSupported) {
                                    {
                                        flipperViewModel.connectBle()
                                        navController.navigate(Screen.Flipper.route)
                                    }
                                } else {
                                    null
                                },
                            onConnectFlipperUsb =
                                if (flipperTransportFactory.isUsbSupported) {
                                    {
                                        flipperViewModel.connectUsb()
                                        navController.navigate(Screen.Flipper.route)
                                    }
                                } else {
                                    null
                                },
                            onOpenAbout = { platformActions.openUrl("https://codebutler.github.io/farebot") },
                            onOpenNfcSettings = platformActions.openNfcSettings,
                            onToggleShowAllScans = { historyViewModel.toggleShowAllScans() },
                            onAddAllSamples = {
                                scope.launch {
                                    var count = 0
                                    for (cardInfo in supportedCards) {
                                        val fileName = cardInfo.sampleDumpFile ?: continue
                                        val bytes = Res.readBytes("files/samples/$fileName")
                                        val result =
                                            if (fileName.endsWith(".mfc")) {
                                                cardImporter.importMfcDump(bytes)
                                            } else {
                                                cardImporter.importCards(bytes.decodeToString())
                                            }
                                        if (result is ImportResult.Success) {
                                            for (rawCard in result.cards) {
                                                cardPersister.insertCard(
                                                    SavedCard(
                                                        type = rawCard.cardType(),
                                                        serial = rawCard.tagId().hex(),
                                                        data = cardSerializer.serialize(rawCard),
                                                    ),
                                                )
                                                count++
                                            }
                                        }
                                    }
                                    historyViewModel.loadCards()
                                    platformActions.showToast(getString(Res.string.imported_cards, count))
                                }
                            },
                            onSampleCardTap = { cardInfo ->
                                val fileName = cardInfo.sampleDumpFile ?: return@HomeScreen
                                scope.launch {
                                    try {
                                        val bytes = Res.readBytes("files/samples/$fileName")
                                        val result =
                                            if (fileName.endsWith(".mfc")) {
                                                cardImporter.importMfcDump(bytes)
                                            } else {
                                                cardImporter.importCards(bytes.decodeToString())
                                            }
                                        if (result is ImportResult.Success && result.cards.isNotEmpty()) {
                                            val rawCard = result.cards.first()
                                            val navKey = navDataHolder.put(rawCard)
                                            val cardName = getString(cardInfo.nameRes)
                                            navController.navigate(Screen.SampleCard.createRoute(navKey, cardName))
                                        }
                                    } catch (e: Exception) {
                                        platformActions.showToast("Failed to load sample: ${e.message}")
                                    }
                                }
                            },
                            menuEvents = menuEvents,
                            onSelectedTabChanged = onSelectedTabChanged,
                        )
                    }

                    composable(Screen.Flipper.route) {
                        val flipperUiState by flipperViewModel.uiState.collectAsState()

                        FlipperScreen(
                            uiState = flipperUiState,
                            onRetry = { flipperViewModel.retry() },
                            onNavigateToDirectory = { path -> flipperViewModel.navigateToDirectory(path) },
                            onNavigateUp = { flipperViewModel.navigateUp() },
                            onToggleSelection = { path -> flipperViewModel.toggleFileSelection(path) },
                            onClearSelection = { flipperViewModel.clearSelection() },
                            onImportSelected = { flipperViewModel.importSelectedFiles() },
                            onImportKeys = { flipperViewModel.importKeyDictionary() },
                            onClearImportMessage = { flipperViewModel.clearImportMessage() },
                            onBack = {
                                flipperViewModel.disconnect()
                                navController.popBackStack()
                            },
                        )
                    }

                    graph.keyManagerPlugin?.run {
                        registerKeyRoutes(
                            navController = navController,
                            cardKeysPersister = graph.cardKeysPersister,
                            cardScanner = graph.cardScanner,
                            onPickFile = { callback -> platformActions.pickFileForBytes(callback) },
                        )
                    }

                    composable(
                        route = Screen.Card.route,
                        arguments =
                            listOf(
                                navArgument("cardKey") { type = NavType.StringType },
                                navArgument("scanIdsKey") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("currentScanId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                            ),
                        enterTransition = {
                            if (initialState.destination.route == Screen.Card.route) fadeIn() else null
                        },
                        exitTransition = {
                            if (targetState.destination.route == Screen.Card.route) fadeOut() else null
                        },
                    ) { backStackEntry ->
                        val cardKey = backStackEntry.arguments?.read { getStringOrNull("cardKey") } ?: return@composable
                        val scanIdsKey = backStackEntry.arguments?.read { getStringOrNull("scanIdsKey") }
                        val currentScanId = backStackEntry.arguments?.read { getStringOrNull("currentScanId") }

                        @Suppress("UNCHECKED_CAST")
                        val scanIds = scanIdsKey?.let { navDataHolder.get<List<String>>(it) } ?: emptyList()
                        val viewModel = graphViewModel { cardViewModel }
                        val uiState by viewModel.uiState.collectAsState()

                        LaunchedEffect(cardKey) {
                            viewModel.loadCard(cardKey, scanIds, currentScanId)
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
                            onShare = {
                                val json = viewModel.exportCard()
                                if (json != null) {
                                    val name = uiState.cardName?.lowercase()?.replace(' ', '-') ?: "card"
                                    val serial = uiState.serialNumber ?: ""
                                    val fileName = "farebot-$name-$serial.json"
                                    platformActions.shareFile(json, fileName, "application/json")
                                }
                            },
                            onDelete = {
                                viewModel.deleteCard()
                                historyViewModel.loadCards()
                                navController.popBackStack()
                            },
                            onShowScanHistory = {
                                viewModel.toggleScanHistory()
                            },
                            onNavigateToScan = { savedCardId ->
                                val navKey = viewModel.navigateToScan(savedCardId)
                                if (navKey != null) {
                                    val route = Screen.Card.createRoute(navKey, scanIdsKey, savedCardId)
                                    navController.navigate(route) {
                                        popUpTo(Screen.Card.route) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }

                    composable(
                        route = Screen.SampleCard.route,
                        arguments =
                            listOf(
                                navArgument("cardKey") { type = NavType.StringType },
                                navArgument("cardName") { type = NavType.StringType },
                            ),
                    ) { backStackEntry ->
                        val cardKey = backStackEntry.arguments?.read { getStringOrNull("cardKey") } ?: return@composable
                        val cardName =
                            backStackEntry.arguments?.read { getStringOrNull("cardName") } ?: return@composable
                        val viewModel = graphViewModel { cardViewModel }
                        val uiState by viewModel.uiState.collectAsState()

                        LaunchedEffect(cardKey) {
                            viewModel.loadSampleCard(cardKey, "Sample: $cardName")
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
                        )
                    }

                    composable(
                        route = Screen.CardAdvanced.route,
                        arguments = listOf(navArgument("cardKey") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val cardKey = backStackEntry.arguments?.read { getStringOrNull("cardKey") } ?: return@composable

                        @Suppress("UNCHECKED_CAST")
                        val data = remember { navDataHolder.get<Pair<Card, TransitInfo?>>(cardKey) }
                        val card = data?.first
                        val transitInfo = data?.second

                        val tabs by produceState(emptyList<AdvancedTab>()) {
                            val tabList = mutableListOf<AdvancedTab>()
                            if (transitInfo != null) {
                                val transitInfoUi = transitInfo.getAdvancedUi()
                                if (transitInfoUi != null) {
                                    tabList.add(AdvancedTab(transitInfo.cardName, transitInfoUi))
                                }
                            }
                            if (card != null) {
                                tabList.add(
                                    AdvancedTab(FormattedString(card.cardType.toString()), card.getAdvancedUi()),
                                )
                            }
                            value = tabList
                        }

                        CardAdvancedScreen(
                            uiState = CardAdvancedUiState(tabs = tabs),
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Screen.TripMap.route,
                        arguments = listOf(navArgument("tripKey") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val tripKey = backStackEntry.arguments?.read { getStringOrNull("tripKey") } ?: return@composable

                        val trip = remember { navDataHolder.get<Trip>(tripKey) }
                        val uiState =
                            remember {
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
    }
}
