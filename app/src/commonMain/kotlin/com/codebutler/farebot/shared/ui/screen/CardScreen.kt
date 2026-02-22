package com.codebutler.farebot.shared.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.transit.Trip
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.advanced
import farebot.app.generated.resources.back
import farebot.app.generated.resources.copied_to_clipboard
import farebot.app.generated.resources.delete
import farebot.app.generated.resources.ic_transaction_banned_32dp
import farebot.app.generated.resources.ic_transaction_bus_32dp
import farebot.app.generated.resources.ic_transaction_ferry_32dp
import farebot.app.generated.resources.ic_transaction_metro_32dp
import farebot.app.generated.resources.ic_transaction_pos_32dp
import farebot.app.generated.resources.ic_transaction_train_32dp
import farebot.app.generated.resources.ic_transaction_tram_32dp
import farebot.app.generated.resources.ic_transaction_tvm_32dp
import farebot.app.generated.resources.ic_transaction_unknown_32dp
import farebot.app.generated.resources.ic_transaction_vend_32dp
import farebot.app.generated.resources.menu
import farebot.app.generated.resources.refill
import farebot.app.generated.resources.scan_history
import farebot.app.generated.resources.share
import farebot.app.generated.resources.trip_mode_bus
import farebot.app.generated.resources.trip_mode_cablecar
import farebot.app.generated.resources.trip_mode_ferry
import farebot.app.generated.resources.trip_mode_metro
import farebot.app.generated.resources.trip_mode_monorail
import farebot.app.generated.resources.trip_mode_other
import farebot.app.generated.resources.trip_mode_pos
import farebot.app.generated.resources.trip_mode_toll_road
import farebot.app.generated.resources.trip_mode_train
import farebot.app.generated.resources.trip_mode_tram
import farebot.app.generated.resources.trip_mode_trolleybus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardScreen(
    uiState: CardUiState,
    onBack: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToTripMap: (String) -> Unit,
    onShare: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onShowScanHistory: () -> Unit = {},
    onNavigateToScan: (String) -> Unit = {},
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(Res.string.back),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        else -> {
            CardContentScreen(
                uiState = uiState,
                onBack = onBack,
                onNavigateToAdvanced = onNavigateToAdvanced,
                onNavigateToTripMap = onNavigateToTripMap,
                onShare = onShare,
                onDelete = onDelete,
                onShowScanHistory = onShowScanHistory,
            )
        }
    }

    // Scan history bottom sheet (overlays everything)
    if (uiState.showScanHistory && uiState.scanHistory.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = onShowScanHistory,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = stringResource(Res.string.scan_history),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                for (entry in uiState.scanHistory) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!entry.isCurrent) {
                                        onNavigateToScan(entry.savedCardId)
                                    }
                                }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.scannedDate,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (entry.scannedTime.isNotEmpty()) {
                                Text(
                                    text = entry.scannedTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        RadioButton(
                            selected = entry.isCurrent,
                            onClick = {
                                if (!entry.isCurrent) {
                                    onNavigateToScan(entry.savedCardId)
                                }
                            },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CardContentScreen(
    uiState: CardUiState,
    onBack: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToTripMap: (String) -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?,
    onShowScanHistory: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val brandBgColor =
        remember(uiState.brandColor) {
            uiState.brandColor?.let { colorInt ->
                Color(
                    red = (colorInt shr 16 and 0xFF) / 255f,
                    green = (colorInt shr 8 and 0xFF) / 255f,
                    blue = (colorInt and 0xFF) / 255f,
                )
            }
        }
    val fallbackColor = MaterialTheme.colorScheme.primaryContainer
    val backgroundColor = brandBgColor ?: fallbackColor
    val textColor = remember(backgroundColor) { contrastingTextColor(backgroundColor) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val hasSheetContent =
            uiState.warning != null || uiState.infoItems.isNotEmpty() || uiState.transactions.isNotEmpty()
        val sheetPeekHeight = if (hasSheetContent) maxHeight * 0.30f else 0.dp
        var wasExpanded by rememberSaveable { mutableStateOf(false) }
        val scaffoldState =
            rememberBottomSheetScaffoldState(
                bottomSheetState =
                    rememberStandardBottomSheetState(
                        initialValue = if (wasExpanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
                    ),
            )
        LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
            wasExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
        }
        val clipboardManager = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        val copiedMessage = stringResource(Res.string.copied_to_clipboard)
        val copyToClipboard: (String) -> Unit = { text ->
            clipboardManager.setText(AnnotatedString(text))
            scope.launch { scaffoldState.snackbarHostState.showSnackbar(copiedMessage) }
        }
        val targetExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
        val cornerRadius by animateDpAsState(
            targetValue = if (targetExpanded) 0.dp else 24.dp,
            animationSpec = tween(durationMillis = 300),
        )
        val contentScale by animateFloatAsState(
            targetValue = if (targetExpanded) 1f else (maxWidth.value - 24f) / maxWidth.value,
            animationSpec = tween(durationMillis = 300),
        )

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = sheetPeekHeight,
            sheetShape = RectangleShape,
            sheetContainerColor = Color.Transparent,
            sheetDragHandle = null,
            sheetShadowElevation = 8.dp,
            sheetTonalElevation = 0.dp,
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    title = {},
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = textColor,
                            actionIconContentColor = textColor,
                        ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back),
                            )
                        }
                    },
                    actions = {
                        if (uiState.currentScanLabel != null) {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(end = 4.dp)
                                        .background(
                                            color = textColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp),
                                        ).clickable(onClick = onShowScanHistory)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = uiState.currentScanLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                )
                            }
                        }
                        if (!uiState.isSample) {
                            IconButton(onClick = onShare) {
                                Icon(
                                    platformShareIcon,
                                    contentDescription = stringResource(Res.string.share),
                                )
                            }
                        }
                        if (uiState.hasAdvancedData || onDelete != null) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(Res.string.menu),
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                if (uiState.hasAdvancedData) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.advanced)) },
                                        onClick = {
                                            menuExpanded = false
                                            onNavigateToAdvanced()
                                        },
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.delete)) },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete()
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            },
            sheetContent = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = contentScale
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }.clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    // Drag handle
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 32.dp, height = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(2.dp),
                                    ),
                        )
                    }
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    ) {
                        // Warning banner
                        if (uiState.warning != null) {
                            item {
                                WarningBanner(uiState.warning)
                            }
                        }

                        // Info items
                        if (uiState.infoItems.isNotEmpty()) {
                            item {
                                SectionHeaderRow(TransactionItem.SectionHeader("Info"))
                            }
                            items(uiState.infoItems) { infoItem ->
                                InfoItemRow(
                                    item = infoItem,
                                    onLongClick = {
                                        val text = listOfNotNull(infoItem.title, infoItem.value).joinToString(": ")
                                        copyToClipboard(text)
                                    },
                                )
                            }
                            item {
                                HorizontalDivider()
                            }
                        }

                        // Transactions (with sticky headers)
                        uiState.transactions.forEach { txnItem ->
                            when (txnItem) {
                                is TransactionItem.DateHeader -> {
                                    stickyHeader(key = txnItem.date) {
                                        DateHeaderRow(txnItem)
                                    }
                                }
                                is TransactionItem.SectionHeader -> {
                                    stickyHeader(key = txnItem.title) {
                                        SectionHeaderRow(txnItem)
                                    }
                                }
                                is TransactionItem.TripItem -> {
                                    item {
                                        TripRow(
                                            trip = txnItem,
                                            onNavigateToTripMap = onNavigateToTripMap,
                                            onLongClick = {
                                                val text =
                                                    listOfNotNull(
                                                        txnItem.agency,
                                                        txnItem.route,
                                                        txnItem.stations,
                                                        txnItem.fare,
                                                        txnItem.time,
                                                    ).joinToString(" · ")
                                                copyToClipboard(text)
                                            },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                                is TransactionItem.RefillItem -> {
                                    item {
                                        RefillRow(
                                            refill = txnItem,
                                            onLongClick = {
                                                val text =
                                                    listOfNotNull(
                                                        txnItem.agency,
                                                        txnItem.amount,
                                                        txnItem.time,
                                                    ).joinToString(" · ")
                                                copyToClipboard(text)
                                            },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                                is TransactionItem.SubscriptionItem -> {
                                    item {
                                        SubscriptionRow(
                                            sub = txnItem,
                                            onLongClick = {
                                                val text =
                                                    listOfNotNull(
                                                        txnItem.name,
                                                        txnItem.agency,
                                                        txnItem.validRange,
                                                        txnItem.remainingTrips,
                                                        txnItem.state,
                                                    ).joinToString(" · ")
                                                copyToClipboard(text)
                                            },
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            // Hero area: card image, serial, balance — centered above the sheet
            var showCardDetailSheet by remember { mutableStateOf(false) }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Card image (tappable to show card detail sheet)
                val cardImageRes = uiState.cardInfo?.imageRes
                if (cardImageRes != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.586f)
                                .clickable { showCardDetailSheet = true },
                    ) {
                        Image(
                            painter = painterResource(cardImageRes),
                            contentDescription = uiState.cardName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (uiState.serialNumber != null) {
                    Text(
                        text = uiState.serialNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        modifier =
                            Modifier.combinedClickable(
                                onLongClick = { copyToClipboard(uiState.serialNumber) },
                                onClick = {},
                            ),
                    )
                }
                if (uiState.balances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    for (balanceItem in uiState.balances) {
                        Column(
                            modifier =
                                Modifier.combinedClickable(
                                    onLongClick = { copyToClipboard(balanceItem.balance) },
                                    onClick = {},
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (balanceItem.name != null) {
                                Text(
                                    text = balanceItem.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor.copy(alpha = 0.7f),
                                )
                            }
                            Text(
                                text = balanceItem.balance,
                                style = MaterialTheme.typography.displayMedium,
                                color = textColor,
                            )
                        }
                    }
                }
                if (uiState.emptyStateMessage != null &&
                    uiState.balances.isEmpty() &&
                    uiState.transactions.isEmpty()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.emptyStateMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.7f),
                    )
                }
            }

            // Card detail bottom sheet (same as Explore tab)
            val cardInfo = uiState.cardInfo
            if (showCardDetailSheet && cardInfo != null) {
                val cardName = stringResource(cardInfo.nameRes)
                val cardLocation = stringResource(cardInfo.locationRes)
                ModalBottomSheet(
                    onDismissRequest = { showCardDetailSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    CardDetailSheet(
                        card = cardInfo,
                        cardName = cardName,
                        cardLocation = cardLocation,
                        isSupported = true,
                        isKeysRequired = cardInfo.keysRequired,
                        showImage = false,
                    )
                }
            }
        }
    }
}

internal fun contrastingTextColor(color: Color): Color {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

@Composable
private fun WarningBanner(warning: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = warning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun DateHeaderRow(header: TransactionItem.DateHeader) {
    Text(
        text = header.date,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SectionHeaderRow(header: TransactionItem.SectionHeader) {
    Text(
        text = header.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun InfoItemRow(
    item: InfoItem,
    onLongClick: () -> Unit = {},
) {
    if (item.isHeader) {
        ListItem(
            headlineContent = {
                Text(
                    text = item.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
    } else {
        ListItem(
            headlineContent = {
                Text(
                    text = item.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            trailingContent =
                if (item.value != null) {
                    {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
            modifier =
                Modifier.combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {},
                ),
        )
    }
}

@Composable
private fun TripRow(
    trip: TransactionItem.TripItem,
    onNavigateToTripMap: (String) -> Unit,
    onLongClick: () -> Unit = {},
) {
    val agencyRoute = listOfNotNull(trip.agency, trip.route).joinToString(" ").ifEmpty { null }
    // If no agency/route, promote stations to headline so title isn't blank.
    // If still nothing, fall back to mode name.
    val headline: String
    val stationsInSupporting: Boolean
    if (agencyRoute != null) {
        headline = agencyRoute
        stationsInSupporting = true
    } else if (trip.stations != null) {
        headline = trip.stations
        stationsInSupporting = false
    } else {
        headline = tripModeName(trip.mode)
        stationsInSupporting = false
    }
    val supportingParts =
        buildList {
            if (stationsInSupporting && trip.stations != null) add(trip.stations)
            if (trip.isTransfer) add("Transfer")
            if (trip.isRejected) add("Rejected")
        }
    ListItem(
        headlineContent = {
            Text(text = headline)
        },
        supportingContent =
            if (supportingParts.isNotEmpty()) {
                { Text(text = supportingParts.joinToString(" \u00b7 ")) }
            } else {
                null
            },
        leadingContent = {
            Image(
                painter = painterResource(tripModeIcon(trip.mode)),
                contentDescription = trip.mode?.name,
                modifier = Modifier.size(32.dp),
                colorFilter = if (trip.isRejected) ColorFilter.tint(MaterialTheme.colorScheme.error) else null,
            )
        },
        trailingContent =
            if (trip.fare != null || trip.time != null) {
                {
                    Column(horizontalAlignment = Alignment.End) {
                        if (trip.fare != null) {
                            Text(text = trip.fare, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (trip.time != null) {
                            Text(
                                text = trip.time,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                null
            },
        modifier =
            Modifier.combinedClickable(
                onLongClick = onLongClick,
                onClick = {
                    if (trip.hasLocation && trip.tripKey != null) {
                        onNavigateToTripMap(trip.tripKey)
                    }
                },
            ),
    )
}

@Composable
private fun RefillRow(
    refill: TransactionItem.RefillItem,
    onLongClick: () -> Unit = {},
) {
    ListItem(
        headlineContent = { Text(text = stringResource(Res.string.refill)) },
        supportingContent =
            if (refill.agency != null) {
                { Text(text = refill.agency) }
            } else {
                null
            },
        leadingContent = {
            Image(
                painter = painterResource(Res.drawable.ic_transaction_tvm_32dp),
                contentDescription = stringResource(Res.string.refill),
                modifier = Modifier.size(32.dp),
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = refill.amount, style = MaterialTheme.typography.bodyLarge)
                if (refill.time != null) {
                    Text(
                        text = refill.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        modifier =
            Modifier.combinedClickable(
                onLongClick = onLongClick,
                onClick = {},
            ),
    )
}

@Composable
private fun SubscriptionRow(
    sub: TransactionItem.SubscriptionItem,
    onLongClick: () -> Unit = {},
) {
    val supportingParts =
        buildList {
            if (sub.agency != null) add(sub.agency)
            add(sub.validRange)
            if (sub.remainingTrips != null) add(sub.remainingTrips)
        }
    ListItem(
        headlineContent = { Text(text = sub.name ?: sub.agency ?: "") },
        supportingContent =
            if (supportingParts.isNotEmpty()) {
                { Text(text = supportingParts.joinToString("\n")) }
            } else {
                null
            },
        trailingContent =
            if (sub.state != null) {
                { Text(text = sub.state, style = MaterialTheme.typography.labelMedium) }
            } else {
                null
            },
        modifier =
            Modifier.combinedClickable(
                onLongClick = onLongClick,
                onClick = {},
            ),
    )
}

private fun tripModeIcon(mode: Trip.Mode?): DrawableResource =
    when (mode) {
        Trip.Mode.BUS -> Res.drawable.ic_transaction_bus_32dp
        Trip.Mode.TRAIN -> Res.drawable.ic_transaction_train_32dp
        Trip.Mode.TRAM -> Res.drawable.ic_transaction_tram_32dp
        Trip.Mode.METRO -> Res.drawable.ic_transaction_metro_32dp
        Trip.Mode.FERRY -> Res.drawable.ic_transaction_ferry_32dp
        Trip.Mode.TICKET_MACHINE -> Res.drawable.ic_transaction_tvm_32dp
        Trip.Mode.VENDING_MACHINE -> Res.drawable.ic_transaction_vend_32dp
        Trip.Mode.POS -> Res.drawable.ic_transaction_pos_32dp
        Trip.Mode.BANNED -> Res.drawable.ic_transaction_banned_32dp
        else -> Res.drawable.ic_transaction_unknown_32dp
    }

@Composable
private fun tripModeName(mode: Trip.Mode?): String =
    when (mode) {
        Trip.Mode.BUS -> stringResource(Res.string.trip_mode_bus)
        Trip.Mode.TRAIN -> stringResource(Res.string.trip_mode_train)
        Trip.Mode.TRAM -> stringResource(Res.string.trip_mode_tram)
        Trip.Mode.METRO -> stringResource(Res.string.trip_mode_metro)
        Trip.Mode.FERRY -> stringResource(Res.string.trip_mode_ferry)
        Trip.Mode.POS -> stringResource(Res.string.trip_mode_pos)
        Trip.Mode.TROLLEYBUS -> stringResource(Res.string.trip_mode_trolleybus)
        Trip.Mode.TOLL_ROAD -> stringResource(Res.string.trip_mode_toll_road)
        Trip.Mode.MONORAIL -> stringResource(Res.string.trip_mode_monorail)
        Trip.Mode.CABLECAR -> stringResource(Res.string.trip_mode_cablecar)
        else -> stringResource(Res.string.trip_mode_other)
    }
