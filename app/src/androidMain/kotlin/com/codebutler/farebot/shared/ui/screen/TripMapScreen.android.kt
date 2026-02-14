package com.codebutler.farebot.shared.ui.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

    val startLatLng =
        startStation?.let {
            val lat = it.latitude?.toDouble()
            val lng = it.longitude?.toDouble()
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }
    val endLatLng =
        endStation?.let {
            val lat = it.latitude?.toDouble()
            val lng = it.longitude?.toDouble()
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }

    if (startLatLng == null && endLatLng == null) return

    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val errorColor = MaterialTheme.colorScheme.error.toArgb()
    val density = LocalDensity.current.density
    val startIcon =
        remember(primaryColor, density) {
            MapsInitializer.initialize(context)
            createBullseyeBitmapDescriptor(primaryColor, 20f, density)
        }
    val endIcon =
        remember(errorColor, density) {
            createBullseyeBitmapDescriptor(errorColor, 20f, density)
        }

    val cameraPositionState =
        rememberCameraPositionState {
            val bounds =
                LatLngBounds
                    .Builder()
                    .apply {
                        startLatLng?.let { include(it) }
                        endLatLng?.let { include(it) }
                    }.build()
            position = CameraPosition.fromLatLngZoom(bounds.center, 13f)
        }

    GoogleMap(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(300.dp),
        cameraPositionState = cameraPositionState,
    ) {
        if (startLatLng != null) {
            Marker(
                state = remember { MarkerState(position = startLatLng) },
                title = startStation.stationName,
                snippet = startStation.companyName,
                icon = startIcon,
                anchor = Offset(0.5f, 0.5f),
            )
        }
        if (endLatLng != null) {
            Marker(
                state = remember { MarkerState(position = endLatLng) },
                title = endStation.stationName,
                snippet = endStation.companyName,
                icon = endIcon,
                anchor = Offset(0.5f, 0.5f),
            )
        }
    }
}

private fun createBullseyeBitmapDescriptor(
    color: Int,
    sizeDp: Float,
    density: Float,
): BitmapDescriptor {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    // Outer colored circle
    val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
    canvas.drawCircle(center, center, sizePx / 2f, fillPaint)
    // Inner white circle (half the radius, matching StationCard)
    val whitePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
    canvas.drawCircle(center, center, sizePx / 4f, whitePaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
