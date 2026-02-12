package com.codebutler.farebot.shared.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.farebot_app.generated.resources.Res
import farebot.farebot_app.generated.resources.card_experimental
import farebot.farebot_app.generated.resources.legend_keys_required
import farebot.farebot_app.generated.resources.legend_serial_only
import farebot.farebot_app.generated.resources.legend_unsupported
import farebot.farebot_app.generated.resources.view_sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    searchQuery: String = "",
    topBarHeight: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var selectedCardKey by remember { mutableStateOf<String?>(null) }

    val displayedCards = remember(supportedCards, supportedCardTypes, loadedKeyBundles, showUnsupported, showSerialOnly, showKeysRequired) {
        supportedCards.filter { card ->
            (showUnsupported || card.cardType in supportedCardTypes) &&
                (showSerialOnly || !card.serialOnly) &&
                (showKeysRequired || !card.keysRequired || card.keyBundle in loadedKeyBundles)
        }
    }

    // Pre-resolve card names and locations for search
    val cardNames = remember(displayedCards) {
        displayedCards.associate { card ->
            card.nameRes.key to runBlocking { getString(card.nameRes) }
        }
    }
    val cardLocations = remember(displayedCards) {
        displayedCards.associate { card ->
            card.nameRes.key to runBlocking { getString(card.locationRes) }
        }
    }

    val regionComparator = remember(deviceRegion) {
        TransitRegion.DeviceRegionComparator(deviceRegion)
    }

    val groupedCards = remember(displayedCards, regionComparator, searchQuery, cardNames, cardLocations) {
        val filtered = if (searchQuery.isBlank()) {
            displayedCards
        } else {
            displayedCards.filter { card ->
                val name = cardNames[card.nameRes.key] ?: ""
                val location = cardLocations[card.nameRes.key] ?: ""
                name.contains(searchQuery, ignoreCase = true) ||
                    location.contains(searchQuery, ignoreCase = true) ||
                    card.region.translatedName.contains(searchQuery, ignoreCase = true)
            }
        }
        filtered
            .groupBy { it.region }
            .entries
            .sortedWith(compareBy(regionComparator) { it.key })
            .associate { it.key to it.value }
    }

    // Build index-to-region mapping for the grid
    val indexToRegion = remember(groupedCards) {
        buildList {
            var index = 0
            groupedCards.forEach { (region, cards) ->
                add(index to region)
                index += 1 + cards.size // header + cards
            }
        }
    }

    // Build flat index mapping card name keys to their position in the grid
    val cardKeyToIndex = remember(groupedCards) {
        val map = mutableMapOf<String, Int>()
        var index = 0
        groupedCards.forEach { (_, cards) ->
            index++ // header
            cards.forEach { card ->
                map[card.nameRes.key] = index
                index++
            }
        }
        map
    }

    // Track which region is currently visible based on the center of the viewport
    val currentRegion by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset.y + item.size.height / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }
            val centerIndex = centerItem?.index ?: gridState.firstVisibleItemIndex
            indexToRegion.lastOrNull { (startIndex, _) -> startIndex <= centerIndex }?.second
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
        if (mapMarkers.isNotEmpty()) {
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
                                gridState.animateScrollToItem(targetIndex)
                            }
                        }
                    }
                },
                topPadding = topBarHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp + topBarHeight),
            )
        }

        // Scrollable card grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        ) {
            groupedCards.forEach { (region, cards) ->
                item(
                    key = region.translatedName,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    val flag = region.flagEmoji ?: "\uD83C\uDF10"
                    Text(
                        text = "$flag ${region.translatedName}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                items(cards, key = { it.nameRes.key }) { card ->
                    CardImageTile(
                        card = card,
                        cardName = cardNames[card.nameRes.key] ?: "",
                        cardLocation = cardLocations[card.nameRes.key] ?: "",
                        isSupported = card.cardType in supportedCardTypes,
                        isKeysRequired = card.keysRequired && card.keyBundle !in loadedKeyBundles,
                        isSelected = selectedCardKey == card.nameRes.key,
                        onTap = {
                            selectedCardKey = if (selectedCardKey == card.nameRes.key) null else card.nameRes.key
                        },
                        onSampleCardTap = onSampleCardTap,
                    )
                }
            }
        }

        // Legend bar
        if (showUnsupported || showKeysRequired || showSerialOnly) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showUnsupported) {
                    LegendEntry(
                        icon = Icons.Default.MobileOff,
                        label = stringResource(Res.string.legend_unsupported),
                    )
                }
                if (showKeysRequired) {
                    LegendEntry(
                        icon = Icons.Default.Lock,
                        label = stringResource(Res.string.legend_keys_required),
                    )
                }
                if (showSerialOnly) {
                    LegendEntry(
                        icon = Icons.Default.VisibilityOff,
                        label = stringResource(Res.string.legend_serial_only),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendEntry(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CardImageTile(
    card: CardInfo,
    cardName: String,
    cardLocation: String,
    isSupported: Boolean,
    isKeysRequired: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onSampleCardTap: ((CardInfo) -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onTap() },
    ) {
        val imageRes = card.imageRes
        if (imageRes != null) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = cardName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Placeholder for cards without images
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        // Info overlay
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CardInfoOverlay(
                card = card,
                cardName = cardName,
                cardLocation = cardLocation,
                onSampleCardTap = onSampleCardTap,
            )
        }

        // Status badge icons (top-right corner, rendered after overlay to stay on top)
        val badges = buildList {
            if (!isSupported) add(Icons.Default.MobileOff)
            if (isKeysRequired) add(Icons.Default.Lock)
            if (card.serialOnly) add(Icons.Default.VisibilityOff)
        }
        if (badges.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (icon in badges) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardInfoOverlay(
    card: CardInfo,
    cardName: String,
    cardLocation: String,
    onSampleCardTap: ((CardInfo) -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                text = cardName,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Text(
                text = cardLocation,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            val extraNoteRes = card.extraNoteRes
            if (extraNoteRes != null) {
                Text(
                    text = stringResource(extraNoteRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            if (card.preview) {
                Text(
                    text = stringResource(Res.string.card_experimental),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            if (card.sampleDumpFile != null && onSampleCardTap != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onSampleCardTap(card) },
                ) {
                    Icon(
                        Icons.Default.ExpandCircleDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).graphicsLayer(rotationZ = -90f),
                        tint = Color(0xFF64B5F6),
                    )
                    Text(
                        text = stringResource(Res.string.view_sample),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64B5F6),
                    )
                }
            }
        }
    }
}
