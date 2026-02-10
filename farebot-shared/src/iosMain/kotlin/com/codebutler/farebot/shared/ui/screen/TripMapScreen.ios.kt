package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPinAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformTripMap(uiState: TripMapUiState) {
    val startStation = uiState.startStation
    val endStation = uiState.endStation

    val startLat = startStation?.latitude?.toDouble()
    val startLng = startStation?.longitude?.toDouble()
    val endLat = endStation?.latitude?.toDouble()
    val endLng = endStation?.longitude?.toDouble()

    val hasStart = startLat != null && startLng != null
    val hasEnd = endLat != null && endLng != null

    if (!hasStart && !hasEnd) return

    UIKitView(
        factory = {
            MKMapView().apply {
                val startAnnotation = if (hasStart) {
                    MKPointAnnotation().apply {
                        setCoordinate(CLLocationCoordinate2DMake(startLat, startLng))
                        setTitle(startStation.stationName ?: "Start")
                        setSubtitle(startStation.companyName)
                    }
                } else null

                val endAnnotation = if (hasEnd) {
                    MKPointAnnotation().apply {
                        setCoordinate(CLLocationCoordinate2DMake(endLat, endLng))
                        setTitle(endStation.stationName ?: "End")
                        setSubtitle(endStation.companyName)
                    }
                } else null

                val annotations = listOfNotNull(startAnnotation, endAnnotation)
                addAnnotations(annotations)

                // Set the visible region
                if (hasStart && hasEnd) {
                    val centerLat = (startLat + endLat) / 2.0
                    val centerLng = (startLng + endLng) / 2.0
                    val latDelta = kotlin.math.abs(startLat - endLat)
                    val lngDelta = kotlin.math.abs(startLng - endLng)
                    val maxDelta = maxOf(latDelta, lngDelta)
                    // Convert degrees to meters (rough approximation) with padding
                    val distanceMeters = maxOf(maxDelta * 111_000 * 1.5, 1000.0)
                    val center = CLLocationCoordinate2DMake(centerLat, centerLng)
                    setRegion(
                        MKCoordinateRegionMakeWithDistance(center, distanceMeters, distanceMeters),
                        animated = false,
                    )
                } else {
                    val lat = startLat ?: endLat!!
                    val lng = startLng ?: endLng!!
                    val center = CLLocationCoordinate2DMake(lat, lng)
                    setRegion(
                        MKCoordinateRegionMakeWithDistance(center, 2000.0, 2000.0),
                        animated = false,
                    )
                }

                delegate = MapViewDelegate(startAnnotation, endAnnotation)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    )
}

@OptIn(ExperimentalForeignApi::class)
private class MapViewDelegate(
    private val startAnnotation: MKPointAnnotation?,
    private val endAnnotation: MKPointAnnotation?,
) : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: platform.MapKit.MKAnnotationProtocol,
    ): platform.MapKit.MKAnnotationView? {
        val identifier = "pin"
        val pinView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier) as? MKPinAnnotationView
            ?: MKPinAnnotationView(annotation = viewForAnnotation, reuseIdentifier = identifier)

        pinView.annotation = viewForAnnotation
        pinView.canShowCallout = true
        pinView.pinTintColor = when (viewForAnnotation) {
            startAnnotation -> platform.UIKit.UIColor.blueColor
            endAnnotation -> platform.UIKit.UIColor.redColor
            else -> platform.UIKit.UIColor.redColor
        }

        return pinView
    }
}
