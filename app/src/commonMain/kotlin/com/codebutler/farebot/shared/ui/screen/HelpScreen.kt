package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitRegion
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.card_experimental
import farebot.app.generated.resources.chip_keys_required_info
import farebot.app.generated.resources.chip_preview_info
import farebot.app.generated.resources.chip_serial_only_info
import farebot.app.generated.resources.chip_unsupported_info
import farebot.app.generated.resources.credits
import farebot.app.generated.resources.legend_experimental
import farebot.app.generated.resources.legend_keys_required
import farebot.app.generated.resources.legend_serial_only
import farebot.app.generated.resources.legend_unsupported
import farebot.app.generated.resources.view_sample
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreContent(
    supportedCards: List<CardInfo>,
    supportedCardTypes: Set<CardType>,
    deviceRegion: String?,
    loadedKeyBundles: Set<String>,
    showUnsupported: Boolean,
    showSerialOnly: Boolean = false,
    showKeysRequired: Boolean = false,
    showExperimental: Boolean = false,
    onKeysRequiredTap: () -> Unit,
    onStatusChipTap: (String) -> Unit = {},
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayedCards =
        remember(
            supportedCards,
            supportedCardTypes,
            loadedKeyBundles,
            showUnsupported,
            showSerialOnly,
            showKeysRequired,
            showExperimental,
        ) {
            supportedCards.filter { card ->
                (showUnsupported || card.cardType in supportedCardTypes) &&
                    (showSerialOnly || !card.serialOnly) &&
                    (showKeysRequired || !card.keysRequired || card.keyBundle in loadedKeyBundles) &&
                    (showExperimental || !card.preview)
            }
        }

    // Pre-resolve card names and locations for search
    val cardNames: Map<String, String> =
        displayedCards.associate { card ->
            card.nameRes.key to FormattedString(card.nameRes).resolve()
        }
    val cardLocations: Map<String, String> =
        displayedCards.associate { card ->
            card.nameRes.key to FormattedString(card.locationRes).resolve()
        }

    val regionComparator =
        remember(deviceRegion) {
            TransitRegion.DeviceRegionComparator(deviceRegion)
        }

    val groupedCards =
        remember(displayedCards, regionComparator, searchQuery, cardNames, cardLocations) {
            val filtered =
                if (searchQuery.isBlank()) {
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
    val indexToRegion =
        remember(groupedCards) {
            buildList {
                var index = 0
                groupedCards.forEach { (region, cards) ->
                    add(index to region)
                    index += 1 + cards.size // header + cards
                }
            }
        }

    // Build flat index mapping card name keys to their position in the grid
    val cardKeyToIndex =
        remember(groupedCards) {
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
            val centerItem =
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = item.offset.y + item.size.height / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
            val centerIndex = centerItem?.index ?: gridState.firstVisibleItemIndex
            indexToRegion.lastOrNull { (startIndex, _) -> startIndex <= centerIndex }?.second
        }
    }

    // Filter map markers to only show cards visible in the list
    val resolvedMarkerNames: Map<Int, String> =
        mapMarkers.mapIndexed { idx, marker -> idx to marker.name }.toMap()

    val displayedCardNames =
        remember(displayedCards, cardNames) {
            displayedCards.mapNotNull { cardNames[it.nameRes.key] }.toSet()
        }
    val visibleMarkers =
        remember(mapMarkers, displayedCardNames, resolvedMarkerNames) {
            mapMarkers.filterIndexed { idx, _ -> resolvedMarkerNames[idx] in displayedCardNames }
        }

    // Compute focus markers for the current visible region
    val focusMarkers =
        remember(currentRegion, groupedCards, cardNames, visibleMarkers) {
            val region = currentRegion ?: return@remember visibleMarkers
            val regionCards = groupedCards[region] ?: return@remember visibleMarkers
            val regionCardNames = regionCards.mapNotNull { cardNames[it.nameRes.key] }.toSet()
            val filtered =
                visibleMarkers.filter { marker ->
                    val idx = mapMarkers.indexOf(marker)
                    resolvedMarkerNames[idx] in regionCardNames
                }
            filtered.ifEmpty { visibleMarkers }
        }

    Column(modifier = modifier.fillMaxSize()) {
        // Fixed map (stays visible while list scrolls)
        if (visibleMarkers.isNotEmpty()) {
            PlatformCardsMap(
                markers = visibleMarkers,
                focusMarkers = focusMarkers,
                onMarkerTap = { markerName ->
                    val matchingCard =
                        displayedCards.find { card ->
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
                modifier =
                    Modifier
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
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                items(cards, key = { it.nameRes.key }) { card ->
                    CardImageTile(
                        card = card,
                        cardName = cardNames[card.nameRes.key] ?: "",
                        isSupported = card.cardType in supportedCardTypes,
                        isKeysRequired = card.keysRequired && card.keyBundle !in loadedKeyBundles,
                        onTap = {
                            selectedCardKey = card.nameRes.key
                        },
                    )
                }
            }
        }

        // Legend bar
        if (showUnsupported || showKeysRequired || showSerialOnly || showExperimental) {
            HorizontalDivider()
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                        icon = Icons.Default.SubtitlesOff,
                        label = stringResource(Res.string.legend_serial_only),
                    )
                }
                if (showExperimental) {
                    LegendEntry(
                        icon = Icons.Default.Science,
                        label = stringResource(Res.string.legend_experimental),
                    )
                }
            }
        }
    }

    // Bottom sheet for selected card details
    val selectedCard =
        selectedCardKey?.let { key ->
            supportedCards.find { it.nameRes.key == key }
        }
    if (selectedCard != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCardKey = null },
            sheetState = sheetState,
        ) {
            CardDetailSheet(
                card = selectedCard,
                cardName = cardNames[selectedCard.nameRes.key] ?: "",
                cardLocation = cardLocations[selectedCard.nameRes.key] ?: "",
                isSupported = selectedCard.cardType in supportedCardTypes,
                isKeysRequired = selectedCard.keysRequired && selectedCard.keyBundle !in loadedKeyBundles,
                onStatusChipTap = onStatusChipTap,
                onSampleCardTap =
                    if (selectedCard.sampleDumpFile != null && onSampleCardTap != null) {
                        {
                            scope.launch {
                                sheetState.hide()
                                selectedCardKey = null
                                onSampleCardTap(selectedCard)
                            }
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun LegendEntry(
    icon: ImageVector,
    label: String,
) {
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
    isSupported: Boolean,
    isKeysRequired: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier =
            Modifier
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
                modifier =
                    Modifier
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

        // Status badge icons (top-right corner)
        val badges =
            buildList {
                if (!isSupported) add(Icons.Default.MobileOff)
                if (isKeysRequired) add(Icons.Default.Lock)
                if (card.serialOnly) add(Icons.Default.SubtitlesOff)
                if (card.preview) add(Icons.Default.Science)
            }
        if (badges.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (icon in badges) {
                    Box(
                        modifier =
                            Modifier
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardDetailSheet(
    card: CardInfo,
    cardName: String,
    cardLocation: String,
    isSupported: Boolean,
    isKeysRequired: Boolean,
    onStatusChipTap: (String) -> Unit = {},
    onSampleCardTap: (() -> Unit)? = null,
    showImage: Boolean = true,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        // Card image
        if (showImage) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.586f)
                        .clip(RoundedCornerShape(12.dp)),
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
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = cardName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Card name
        Text(
            text = cardName,
            style = MaterialTheme.typography.titleMedium,
        )

        // Location
        Text(
            text = cardLocation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Status chips (card type + status indicators)
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Card type chip (always shown)
                NonInteractiveChip {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(card.cardType.toString()) },
                        icon = { Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }

                if (!isSupported) {
                    val msg = stringResource(Res.string.chip_unsupported_info)
                    SuggestionChip(
                        onClick = { onStatusChipTap(msg) },
                        label = { Text(stringResource(Res.string.legend_unsupported)) },
                        icon = {
                            Icon(
                                Icons.Default.MobileOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
                if (isKeysRequired) {
                    val msg = stringResource(Res.string.chip_keys_required_info)
                    SuggestionChip(
                        onClick = { onStatusChipTap(msg) },
                        label = { Text(stringResource(Res.string.legend_keys_required)) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
                if (card.serialOnly) {
                    val msg = stringResource(Res.string.chip_serial_only_info)
                    SuggestionChip(
                        onClick = { onStatusChipTap(msg) },
                        label = { Text(stringResource(Res.string.legend_serial_only)) },
                        icon = {
                            Icon(
                                Icons.Default.SubtitlesOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
                if (card.preview) {
                    val msg = stringResource(Res.string.chip_preview_info)
                    SuggestionChip(
                        onClick = { onStatusChipTap(msg) },
                        label = { Text(stringResource(Res.string.card_experimental)) },
                        icon = {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }

        // Extra note
        val extraNoteRes = card.extraNoteRes
        if (extraNoteRes != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(extraNoteRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Credits
        if (card.credits.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.credits),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = card.credits.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // View sample card
        if (onSampleCardTap != null) {
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text(stringResource(Res.string.view_sample)) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSampleCardTap() },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun NonInteractiveChip(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        content()
    }
}
