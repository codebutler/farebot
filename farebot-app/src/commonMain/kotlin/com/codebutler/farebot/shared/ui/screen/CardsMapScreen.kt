package com.codebutler.farebot.shared.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class CardsMapMarker(
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
)

@Composable
expect fun PlatformCardsMap(
    markers: List<CardsMapMarker>,
    modifier: Modifier,
    onMarkerTap: ((String) -> Unit)? = null,
    focusMarkers: List<CardsMapMarker> = emptyList(),
)
