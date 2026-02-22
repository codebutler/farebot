package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.img_home_splash
import farebot.app.generated.resources.unknown_card
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Height of the peek region visible for overlapped cards. */
private val CARD_PEEK_HEIGHT = 64.dp

/** Minimum height for overlapped cards — must be taller than CARD_PEEK_HEIGHT to create the overlap effect. */
private val CARD_MIN_HEIGHT = 120.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    supportedCardTypes: Set<CardType> = emptySet(),
    loadedKeyBundles: Set<String> = emptySet(),
    onNavigateToCard: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.items.isEmpty() -> {
                Image(
                    painter = painterResource(Res.drawable.img_home_splash),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .padding(80.dp),
                )
            }
            else -> {
                val grouped = uiState.items.groupBy { it.scannedDate ?: "" }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            // Extra bottom padding so the FAB doesn't cover the last card
                            bottom = 88.dp,
                        ),
                ) {
                    grouped.forEach { (date, items) ->
                        if (date.isNotEmpty()) {
                            stickyHeader(key = "header-$date") {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(vertical = 8.dp),
                                )
                            }
                        }
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            val isLastInGroup = index == items.lastIndex
                            val isSelected = uiState.selectedIds.contains(item.id)
                            WalletCard(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                isLastInGroup = isLastInGroup,
                                onNavigateToCard = onNavigateToCard,
                                onToggleSelection = onToggleSelection,
                            )
                        }
                        // Spacing between date groups
                        item(key = "spacer-$date") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single card in the wallet stack.
 *
 * Overlapped cards use the credit-card aspect ratio so the peek region is consistent.
 * The last card in each group wraps its content height (roughly list-item sized).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WalletCard(
    item: HistoryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isLastInGroup: Boolean,
    onNavigateToCard: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
) {
    val brandBgColor =
        remember(item.brandColor) {
            item.brandColor?.let { colorInt ->
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

    val cardShape = RoundedCornerShape(16.dp)
    Surface(
        shape = cardShape,
        shadowElevation = 12.dp,
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Black.copy(alpha = 0.15f)),
        modifier =
            (if (!isLastInGroup) Modifier.overlapHeight(CARD_PEEK_HEIGHT) else Modifier)
                .fillMaxWidth()
                .then(if (!isLastInGroup) Modifier.heightIn(min = CARD_MIN_HEIGHT) else Modifier)
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelection(item.id)
                        } else {
                            onNavigateToCard(item.id)
                        }
                    },
                    onLongClick = {
                        onToggleSelection(item.id)
                    },
                ),
    ) {
        Box {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Left: checkbox (selection mode) + card name + serial
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection(item.id) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    Column {
                        Text(
                            text = item.cardName ?: stringResource(Res.string.unknown_card),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.serial,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Right: balance + time
                Column(horizontalAlignment = Alignment.End) {
                    if (item.balance != null) {
                        Text(
                            text = item.balance,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )
                    }
                    if (item.scannedTime != null) {
                        Text(
                            text = item.scannedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Selection tint overlay
            if (isSelectionMode && isSelected) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                )
            }
        }
    }
}

/**
 * Custom layout modifier that reduces the measured height of a composable to [peekHeight],
 * causing subsequent items in a LazyColumn to overlap this one.
 */
private fun Modifier.overlapHeight(peekHeight: Dp): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val peekPx = peekHeight.roundToPx()
        // Report only peekHeight as the consumed height so the next item overlaps
        layout(placeable.width, peekPx) {
            placeable.placeRelative(0, 0)
        }
    }
