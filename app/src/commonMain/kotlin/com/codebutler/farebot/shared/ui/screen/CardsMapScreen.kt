package com.codebutler.farebot.shared.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.base.util.FormattedString

data class CardsMapMarker(
    val name: FormattedString,
    val location: FormattedString,
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
