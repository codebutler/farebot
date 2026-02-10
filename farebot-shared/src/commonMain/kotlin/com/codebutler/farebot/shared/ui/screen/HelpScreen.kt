package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.card_experimental
import farebot.farebot_shared.generated.resources.card_not_supported
import farebot.farebot_shared.generated.resources.keys_required
import farebot.farebot_shared.generated.resources.keys_loaded
import farebot.farebot_shared.generated.resources.card_serial_only
import farebot.farebot_shared.generated.resources.cards_map
import farebot.farebot_shared.generated.resources.show_unsupported_cards
import farebot.farebot_shared.generated.resources.supported_cards
import farebot.farebot_shared.generated.resources.back
import farebot.farebot_shared.generated.resources.menu
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HelpScreen(
    supportedCards: List<CardInfo>,
    supportedCardTypes: Set<CardType>,
    deviceRegion: String? = null,
    loadedKeyBundles: Set<String> = emptySet(),
    onBack: () -> Unit,
    onKeysRequiredTap: () -> Unit = {},
    onNavigateToCardsMap: () -> Unit = {},
) {
    var showUnsupported by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val hasUnsupportedCards = remember(supportedCards, supportedCardTypes) {
        supportedCards.any { it.cardType !in supportedCardTypes }
    }

    val displayedCards = remember(supportedCards, supportedCardTypes, showUnsupported) {
        if (showUnsupported) supportedCards
        else supportedCards.filter { it.cardType in supportedCardTypes }
    }

    val regionComparator = remember(deviceRegion) {
        TransitRegion.DeviceRegionComparator(deviceRegion)
    }

    val groupedCards = remember(displayedCards, regionComparator) {
        displayedCards
            .groupBy { it.region }
            .entries
            .sortedWith(compareBy(regionComparator) { it.key })
            .associate { it.key to it.value }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.supported_cards)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCardsMap) {
                        Icon(Icons.Default.Map, contentDescription = stringResource(Res.string.cards_map))
                    }
                    if (hasUnsupportedCards) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.menu))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.show_unsupported_cards)) },
                                leadingIcon = if (showUnsupported) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                onClick = { showUnsupported = !showUnsupported; menuExpanded = false },
                            )
                        }
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            groupedCards.forEach { (region, cards) ->
                stickyHeader(key = region.translatedName) {
                    val flag = region.flagEmoji ?: "\uD83C\uDF10"
                    Text(
                        text = "$flag ${region.translatedName}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(cards, key = { it.nameRes.key }) { card ->
                    CardInfoItem(
                        card = card,
                        isSupported = card.cardType in supportedCardTypes,
                        keysLoaded = card.keyBundle != null && card.keyBundle in loadedKeyBundles,
                        onKeysRequiredTap = onKeysRequiredTap,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardInfoItem(
    card: CardInfo,
    isSupported: Boolean,
    keysLoaded: Boolean,
    onKeysRequiredTap: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            val cardName = stringResource(card.nameRes)
            val imageRes = card.imageRes
            if (imageRes != null) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = cardName,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = cardName, style = MaterialTheme.typography.titleMedium)
                if (card.keysRequired) {
                    Spacer(modifier = Modifier.width(4.dp))
                    if (keysLoaded) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = stringResource(Res.string.keys_loaded),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(Res.string.keys_required),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onKeysRequiredTap() },
                        )
                    }
                }
            }
            Text(
                text = stringResource(card.locationRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.serialOnly) {
                Text(
                    text = stringResource(Res.string.card_serial_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val extraNoteRes = card.extraNoteRes
            if (extraNoteRes != null) {
                Text(
                    text = stringResource(extraNoteRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (card.preview) {
                Text(
                    text = stringResource(Res.string.card_experimental),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isSupported) {
                Text(
                    text = stringResource(Res.string.card_not_supported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
