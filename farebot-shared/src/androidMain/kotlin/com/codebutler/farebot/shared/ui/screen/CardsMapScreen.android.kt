package com.codebutler.farebot.shared.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun PlatformCardsMap(markers: List<CardsMapMarker>, modifier: Modifier) {
    if (markers.isEmpty()) return

    val cameraPositionState = rememberCameraPositionState {
        val bounds = LatLngBounds.Builder().apply {
            markers.forEach { include(LatLng(it.latitude, it.longitude)) }
        }.build()
        position = CameraPosition.fromLatLngZoom(bounds.center, 1f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
    ) {
        markers.forEach { marker ->
            Marker(
                state = remember(marker) { MarkerState(position = LatLng(marker.latitude, marker.longitude)) },
                title = marker.name,
                snippet = marker.location,
            )
        }
    }
}
