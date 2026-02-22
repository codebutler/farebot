package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MobileOff
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.img_home_splash
import farebot.app.generated.resources.unknown_card
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
                            .widthIn(max = 320.dp)
                            .padding(80.dp),
                )
            }
            else -> {
                val grouped = uiState.items.groupBy { it.scannedDate ?: "" }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                        items(items, key = { it.id }) { item ->
                            val isSelected = uiState.selectedIds.contains(item.id)
                            val dotColor =
                                if (item.brandColor != null) {
                                    Color(0xFF000000.toInt() or item.brandColor)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            val supportingParts =
                                buildList {
                                    add(item.serial)
                                    if (item.parseError != null) add(item.parseError)
                                }
                            ListItem(
                                headlineContent = {
                                    Text(text = item.cardName ?: stringResource(Res.string.unknown_card))
                                },
                                supportingContent = {
                                    Text(
                                        text = supportingParts.joinToString("\n"),
                                        fontFamily = FontFamily.Monospace,
                                    )
                                },
                                leadingContent = {
                                    if (uiState.isSelectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { onToggleSelection(item.id) },
                                        )
                                    } else {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(width = 14.dp, height = 10.dp)
                                                    .background(dotColor, RoundedCornerShape(2.dp)),
                                        )
                                    }
                                },
                                trailingContent = {
                                    val badges =
                                        buildList {
                                            if (item.cardType != null &&
                                                item.cardType !in supportedCardTypes
                                            ) {
                                                add(Icons.Default.MobileOff)
                                            }
                                            if (item.keysRequired &&
                                                item.keyBundle !in loadedKeyBundles
                                            ) {
                                                add(Icons.Default.Lock)
                                            }
                                            if (item.serialOnly) add(Icons.Default.SubtitlesOff)
                                            if (item.preview) add(Icons.Default.Science)
                                        }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (badges.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                for (icon in badges) {
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .size(16.dp)
                                                                .background(
                                                                    Color.Black.copy(alpha = 0.6f),
                                                                    CircleShape,
                                                                ),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Icon(
                                                            icon,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(10.dp),
                                                            tint = Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (item.scannedTime != null) {
                                            Text(
                                                text = item.scannedTime,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                modifier =
                                    Modifier.combinedClickable(
                                        onClick = {
                                            if (uiState.isSelectionMode) {
                                                onToggleSelection(item.id)
                                            } else {
                                                onNavigateToCard(item.id)
                                            }
                                        },
                                        onLongClick = {
                                            onToggleSelection(item.id)
                                        },
                                    ),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
