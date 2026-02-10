package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ElevatedCard
import com.codebutler.farebot.transit.Trip
import farebot.farebot_app.generated.resources.Res
import farebot.farebot_app.generated.resources.back
import farebot.farebot_app.generated.resources.menu
import farebot.farebot_app.generated.resources.advanced
import farebot.farebot_app.generated.resources.balance
import farebot.farebot_app.generated.resources.copy
import farebot.farebot_app.generated.resources.save
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen(
    uiState: CardUiState,
    onBack: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToTripMap: (String) -> Unit,
    onExportShare: () -> Unit = {},
    onExportSave: () -> Unit = {},
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
                                style = MaterialTheme.typography.bodySmall
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

                        items(uiState.transactions) { item ->
                            when (item) {
                                is TransactionItem.DateHeader -> {
                                    DateHeaderRow(item)
                                }
                                is TransactionItem.SectionHeader -> {
                                    SectionHeaderRow(item)
                                }
                                is TransactionItem.TripItem -> {
                                    TripRow(item, onNavigateToTripMap)
                                    HorizontalDivider()
                                }
                                is TransactionItem.RefillItem -> {
                                    RefillRow(item)
                                    HorizontalDivider()
                                }
                                is TransactionItem.SubscriptionItem -> {
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

@Composable
private fun WarningBanner(warning: String) {
    Text(
        text = warning,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp)
    )
}

@Composable
private fun DateHeaderRow(header: TransactionItem.DateHeader) {
    Text(
        text = header.date,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoItemRow(item: InfoItem) {
    if (item.isHeader) {
        Text(
            text = item.title ?: "",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            if (item.title != null) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (item.value != null) {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TripRow(
    trip: TransactionItem.TripItem,
    onNavigateToTripMap: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { mod ->
                if (trip.hasLocation && trip.tripKey != null) {
                    mod.clickable { onNavigateToTripMap(trip.tripKey) }
                } else mod
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(tripModeIcon(trip.mode)),
            contentDescription = trip.mode?.name,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(
                if (trip.isRejected) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            ),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (trip.route != null) {
                Text(text = trip.route, style = MaterialTheme.typography.bodyMedium)
            }
            if (trip.agency != null) {
                Text(
                    text = trip.agency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trip.stations != null) {
                Text(
                    text = trip.stations,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trip.isTransfer) {
                Text(
                    text = "Transfer",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trip.isRejected) {
                Text(
                    text = "Rejected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (trip.fare != null) {
                Text(text = trip.fare, style = MaterialTheme.typography.bodyMedium)
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
}

@Composable
private fun RefillRow(refill: TransactionItem.RefillItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(48.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.refill),
                style = MaterialTheme.typography.bodyMedium
            )
            if (refill.agency != null) {
                Text(
                    text = refill.agency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = refill.amount, style = MaterialTheme.typography.bodyMedium)
            if (refill.time != null) {
                Text(
                    text = refill.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubscriptionRow(sub: TransactionItem.SubscriptionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(48.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (sub.name != null) {
                Text(text = sub.name, style = MaterialTheme.typography.bodyMedium)
            }
            if (sub.agency != null) {
                Text(
                    text = sub.agency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = sub.validRange,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (sub.remainingTrips != null) {
                Text(
                    text = sub.remainingTrips,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (sub.state != null) {
            Text(
                text = sub.state,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
