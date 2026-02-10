package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun PlatformTripMap(uiState: TripMapUiState) {
    val startStation = uiState.startStation
    val endStation = uiState.endStation

    val startLatLng = startStation?.let {
        val lat = it.latitude?.toDouble()
        val lng = it.longitude?.toDouble()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }
    val endLatLng = endStation?.let {
        val lat = it.latitude?.toDouble()
        val lng = it.longitude?.toDouble()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }

    if (startLatLng == null && endLatLng == null) return

    val cameraPositionState = rememberCameraPositionState {
        val bounds = LatLngBounds.Builder().apply {
            startLatLng?.let { include(it) }
            endLatLng?.let { include(it) }
        }.build()
        position = CameraPosition.fromLatLngZoom(bounds.center, 13f)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        cameraPositionState = cameraPositionState,
    ) {
        if (startLatLng != null) {
            Marker(
                state = remember { MarkerState(position = startLatLng) },
                title = startStation.stationName,
                snippet = startStation.companyName,
            )
        }
        if (endLatLng != null) {
            Marker(
                state = remember { MarkerState(position = endLatLng) },
                title = endStation.stationName,
                snippet = endStation.companyName,
            )
        }
    }
}
