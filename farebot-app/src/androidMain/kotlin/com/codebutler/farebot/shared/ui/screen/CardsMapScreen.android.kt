package com.codebutler.farebot.shared.ui.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.android.gms.maps.CameraUpdateFactory
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
actual fun PlatformCardsMap(
    markers: List<CardsMapMarker>,
    modifier: Modifier,
    onMarkerTap: ((String) -> Unit)?,
    focusMarkers: List<CardsMapMarker>,
    topPadding: Dp,
) {
    if (markers.isEmpty()) return

    val context = LocalContext.current
    val markerColor = MaterialTheme.colorScheme.primary.toArgb()
    val density = LocalDensity.current.density
    val dotIcon =
        remember(markerColor, density) {
            MapsInitializer.initialize(context)
            createDotBitmapDescriptor(markerColor, 14f, 2f, density)
        }

    val cameraPositionState =
        rememberCameraPositionState {
            val bounds =
                LatLngBounds
                    .Builder()
                    .apply {
                        markers.forEach { include(LatLng(it.latitude, it.longitude)) }
                    }.build()
            position = CameraPosition.fromLatLngZoom(bounds.center, 1f)
        }

    LaunchedEffect(focusMarkers) {
        if (focusMarkers.size == 1) {
            val m = focusMarkers.first()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(m.latitude, m.longitude), 10f),
            )
        } else if (focusMarkers.size > 1) {
            val bounds =
                LatLngBounds
                    .Builder()
                    .apply {
                        focusMarkers.forEach { include(LatLng(it.latitude, it.longitude)) }
                    }.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 50),
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        contentPadding = PaddingValues(top = topPadding),
    ) {
        markers.forEach { marker ->
            Marker(
                state = remember(marker) { MarkerState(position = LatLng(marker.latitude, marker.longitude)) },
                title = marker.name,
                snippet = marker.location,
                icon = dotIcon,
                anchor = Offset(0.5f, 0.5f),
                onClick = {
                    onMarkerTap?.invoke(marker.name)
                    false
                },
            )
        }
    }
}

private fun createDotBitmapDescriptor(
    color: Int,
    sizeDp: Float,
    borderDp: Float,
    density: Float,
): BitmapDescriptor {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val borderPx = borderDp * density
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    // White border circle
    val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
    canvas.drawCircle(center, center, sizePx / 2f, borderPaint)
    // Colored inner circle
    val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
    canvas.drawCircle(center, center, sizePx / 2f - borderPx, fillPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
