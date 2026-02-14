package com.codebutler.farebot.shared.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CardsMapMarker(
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
)

expect val platformHasCardsMap: Boolean

@Composable
expect fun PlatformCardsMap(
    markers: List<CardsMapMarker>,
    modifier: Modifier,
    onMarkerTap: ((String) -> Unit)? = null,
    focusMarkers: List<CardsMapMarker> = emptyList(),
    topPadding: Dp = 0.dp,
)
