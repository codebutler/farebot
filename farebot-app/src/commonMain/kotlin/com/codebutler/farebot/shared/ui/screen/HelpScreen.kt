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
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_app.generated.resources.Res
import farebot.farebot_app.generated.resources.card_experimental
import farebot.farebot_app.generated.resources.card_not_supported
import farebot.farebot_app.generated.resources.keys_required
import farebot.farebot_app.generated.resources.keys_loaded
import farebot.farebot_app.generated.resources.card_serial_only
import farebot.farebot_app.generated.resources.search_supported_cards
import farebot.farebot_app.generated.resources.view_sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreContent(
    supportedCards: List<CardInfo>,
    supportedCardTypes: Set<CardType>,
    deviceRegion: String?,
    loadedKeyBundles: Set<String>,
    showUnsupported: Boolean,
    showSerialOnly: Boolean = false,
    showKeysRequired: Boolean = false,
    onKeysRequiredTap: () -> Unit,
    mapMarkers: List<CardsMapMarker> = emptyList(),
    onMapMarkerTap: ((String) -> Unit)? = null,
    onSampleCardTap: ((CardInfo) -> Unit)? = null,
    topBarHeight: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val displayedCards = remember(supportedCards, supportedCardTypes, loadedKeyBundles, showUnsupported, showSerialOnly, showKeysRequired) {
        supportedCards.filter { card ->
            (showUnsupported || card.cardType in supportedCardTypes) &&
                (showSerialOnly || !card.serialOnly) &&
                (showKeysRequired || !card.keysRequired || card.keyBundle in loadedKeyBundles)
        }
    }

    // Pre-resolve card names for search
    val cardNames = remember(displayedCards) {
        displayedCards.associate { card ->
            card.nameRes.key to runBlocking { getString(card.nameRes) }
        }
    }

    val regionComparator = remember(deviceRegion) {
        TransitRegion.DeviceRegionComparator(deviceRegion)
    }

    val groupedCards = remember(displayedCards, regionComparator, searchQuery, cardNames) {
        val filtered = if (searchQuery.isBlank()) {
            displayedCards
        } else {
            displayedCards.filter { card ->
                val name = cardNames[card.nameRes.key] ?: ""
                name.contains(searchQuery, ignoreCase = true) ||
                    card.region.translatedName.contains(searchQuery, ignoreCase = true)
            }
        }
        filtered
            .groupBy { it.region }
            .entries
            .sortedWith(compareBy(regionComparator) { it.key })
            .associate { it.key to it.value }
    }

    // Build index-to-region mapping for the LazyColumn
    // (only sticky headers + card items, map and search are outside)
    val indexToRegion = remember(groupedCards) {
        buildList {
            var index = 0
            groupedCards.forEach { (region, cards) ->
                add(index to region)
                index += 1 + cards.size // header + cards
            }
        }
    }

    // Build flat index mapping card name keys to their position in the LazyColumn
    val cardKeyToIndex = remember(groupedCards) {
        val map = mutableMapOf<String, Int>()
        var index = 0
        groupedCards.forEach { (_, cards) ->
            index++ // sticky header
            cards.forEach { card ->
                map[card.nameRes.key] = index
                index++
            }
        }
        map
    }

    // Track which region is currently visible based on scroll position
    val currentRegion by remember {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            indexToRegion.lastOrNull { (startIndex, _) -> startIndex <= firstVisible }?.second
        }
    }

    // Compute focus markers for the current visible region
    val focusMarkers = remember(currentRegion, groupedCards, cardNames, mapMarkers) {
        val region = currentRegion ?: return@remember mapMarkers
        val regionCards = groupedCards[region] ?: return@remember mapMarkers
        val regionCardNames = regionCards.mapNotNull { cardNames[it.nameRes.key] }.toSet()
        val filtered = mapMarkers.filter { it.name in regionCardNames }
        filtered.ifEmpty { mapMarkers }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Fixed map (stays visible while list scrolls)
        if (mapMarkers.isNotEmpty() && searchQuery.isBlank()) {
            PlatformCardsMap(
                markers = mapMarkers,
                focusMarkers = focusMarkers,
                onMarkerTap = { markerName ->
                    val matchingCard = displayedCards.find { card ->
                        cardNames[card.nameRes.key] == markerName
                    }
                    if (matchingCard != null) {
                        val targetIndex = cardKeyToIndex[matchingCard.nameRes.key]
                        if (targetIndex != null) {
                            scope.launch {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                    }
                },
                topPadding = topBarHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp + topBarHeight),
            )
        }

        // Fixed search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(Res.string.search_supported_cards)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        )

        // Scrollable card list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
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
                        onSampleCardTap = onSampleCardTap,
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
    onSampleCardTap: ((CardInfo) -> Unit)? = null,
) {
    val hasSample = card.sampleDumpFile != null && onSampleCardTap != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .let { mod ->
                if (hasSample) mod.clickable { onSampleCardTap?.invoke(card) } else mod
            },
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
            if (hasSample) {
                Text(
                    text = stringResource(Res.string.view_sample),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
