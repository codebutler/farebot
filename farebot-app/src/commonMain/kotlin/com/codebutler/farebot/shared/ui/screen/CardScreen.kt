package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ListItem
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ElevatedCard
import com.codebutler.farebot.transit.Trip
import farebot.farebot_app.generated.resources.Res
import farebot.farebot_app.generated.resources.back
import farebot.farebot_app.generated.resources.menu
import farebot.farebot_app.generated.resources.advanced
import farebot.farebot_app.generated.resources.balance
import farebot.farebot_app.generated.resources.current_scan
import farebot.farebot_app.generated.resources.delete
import farebot.farebot_app.generated.resources.n_scans
import farebot.farebot_app.generated.resources.save
import farebot.farebot_app.generated.resources.scan_history
import farebot.farebot_app.generated.resources.share
import farebot.farebot_app.generated.resources.ic_transaction_banned_32dp
import farebot.farebot_app.generated.resources.ic_transaction_bus_32dp
import farebot.farebot_app.generated.resources.ic_transaction_ferry_32dp
import farebot.farebot_app.generated.resources.ic_transaction_metro_32dp
import farebot.farebot_app.generated.resources.ic_transaction_pos_32dp
import farebot.farebot_app.generated.resources.ic_transaction_train_32dp
import farebot.farebot_app.generated.resources.ic_transaction_tram_32dp
import farebot.farebot_app.generated.resources.ic_transaction_tvm_32dp
import farebot.farebot_app.generated.resources.ic_transaction_unknown_32dp
import farebot.farebot_app.generated.resources.ic_transaction_vend_32dp
import farebot.farebot_app.generated.resources.refill
import farebot.farebot_app.generated.resources.unknown_card
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
    onExportShare: () -> Unit = {},
    onExportSave: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onShowScanHistory: () -> Unit = {},
    onNavigateToScan: (String) -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.cardName ?: stringResource(Res.string.unknown_card),
                        )
                        if (uiState.serialNumber != null) {
                            Text(
                                text = uiState.serialNumber,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (uiState.currentScanLabel != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable(onClick = onShowScanHistory)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = uiState.currentScanLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    if (!uiState.isSample || uiState.hasAdvancedData) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.menu))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (!uiState.isSample) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.share)) },
                                    onClick = { menuExpanded = false; onExportShare() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.save)) },
                                    onClick = { menuExpanded = false; onExportSave() }
                                )
                            }
                            if (uiState.hasAdvancedData) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.advanced)) },
                                    onClick = {
                                        menuExpanded = false
                                        onNavigateToAdvanced()
                                    }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.delete)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                },
            )
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
                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Warning banner
                        if (uiState.warning != null) {
                            item {
                                WarningBanner(uiState.warning)
                            }
                        }

                        // Balances
                        if (uiState.balances.isNotEmpty()) {
                            item {
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.balance),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        for (balanceItem in uiState.balances) {
                                            if (balanceItem.name != null) {
                                                Text(
                                                    text = balanceItem.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = balanceItem.balance,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Info items
                        if (uiState.infoItems.isNotEmpty()) {
                            item {
                                SectionHeaderRow(TransactionItem.SectionHeader("Info"))
                            }
                            items(uiState.infoItems) { infoItem ->
                                InfoItemRow(infoItem)
                            }
                            item {
                                HorizontalDivider()
                            }
                        }

                        uiState.transactions.forEach { item ->
                            when (item) {
                                is TransactionItem.DateHeader -> {
                                    stickyHeader(key = item.date) {
                                        DateHeaderRow(item)
                                    }
                                }
                                is TransactionItem.SectionHeader -> {
                                    stickyHeader(key = item.title) {
                                        SectionHeaderRow(item)
                                    }
                                }
                                is TransactionItem.TripItem -> {
                                    item {
                                        TripRow(item, onNavigateToTripMap)
                                        HorizontalDivider()
                                    }
                                }
                                is TransactionItem.RefillItem -> {
                                    item {
                                        RefillRow(item)
                                        HorizontalDivider()
                                    }
                                }
                                is TransactionItem.SubscriptionItem -> {
                                    item {
                                        SubscriptionRow(item)
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Scan history bottom sheet
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!entry.isCurrent) {
                                    onNavigateToScan(entry.savedCardId)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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

@Composable
private fun WarningBanner(warning: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
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
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SectionHeaderRow(header: TransactionItem.SectionHeader) {
    Text(
        text = header.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoItemRow(item: InfoItem) {
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
            headlineContent = { Text(text = item.title ?: "") },
            supportingContent = if (item.value != null) {
                {
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else null,
        )
    }
}

@Composable
private fun TripRow(
    trip: TransactionItem.TripItem,
    onNavigateToTripMap: (String) -> Unit,
) {
    val headline = listOfNotNull(trip.agency, trip.route).joinToString(" ").ifEmpty { null }
    val supportingParts = buildList {
        if (trip.stations != null) add(trip.stations)
        if (trip.isTransfer) add("Transfer")
        if (trip.isRejected) add("Rejected")
    }
    ListItem(
        headlineContent = {
            Text(text = headline ?: "")
        },
        supportingContent = if (supportingParts.isNotEmpty()) {
            { Text(text = supportingParts.joinToString(" \u00b7 ")) }
        } else null,
        leadingContent = {
            Image(
                painter = painterResource(tripModeIcon(trip.mode)),
                contentDescription = trip.mode?.name,
                modifier = Modifier.size(32.dp),
                colorFilter = if (trip.isRejected) ColorFilter.tint(MaterialTheme.colorScheme.error) else null,
            )
        },
        trailingContent = if (trip.fare != null || trip.time != null) {
            {
                Column(horizontalAlignment = Alignment.End) {
                    if (trip.fare != null) {
                        Text(text = trip.fare, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (trip.time != null) {
                        Text(
                            text = trip.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else null,
        modifier = Modifier.let { mod ->
            if (trip.hasLocation && trip.tripKey != null) {
                mod.clickable { onNavigateToTripMap(trip.tripKey) }
            } else mod
        },
    )
}

@Composable
private fun RefillRow(refill: TransactionItem.RefillItem) {
    ListItem(
        headlineContent = { Text(text = stringResource(Res.string.refill)) },
        supportingContent = if (refill.agency != null) {
            { Text(text = refill.agency) }
        } else null,
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = refill.amount, style = MaterialTheme.typography.bodyLarge)
                if (refill.time != null) {
                    Text(
                        text = refill.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
    )
}

@Composable
private fun SubscriptionRow(sub: TransactionItem.SubscriptionItem) {
    val supportingParts = buildList {
        if (sub.agency != null) add(sub.agency)
        add(sub.validRange)
        if (sub.remainingTrips != null) add(sub.remainingTrips)
    }
    ListItem(
        headlineContent = { Text(text = sub.name ?: sub.agency ?: "") },
        supportingContent = if (supportingParts.isNotEmpty()) {
            { Text(text = supportingParts.joinToString("\n")) }
        } else null,
        trailingContent = if (sub.state != null) {
            { Text(text = sub.state, style = MaterialTheme.typography.labelMedium) }
        } else null,
    )
}

private fun tripModeIcon(mode: Trip.Mode?): DrawableResource = when (mode) {
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
