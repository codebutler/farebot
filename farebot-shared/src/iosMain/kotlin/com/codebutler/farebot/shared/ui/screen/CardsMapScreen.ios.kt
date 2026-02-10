package com.codebutler.farebot.shared.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPinAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformCardsMap(markers: List<CardsMapMarker>, modifier: Modifier) {
    if (markers.isEmpty()) return

    UIKitView(
        factory = {
            MKMapView().apply {
                val annotations = markers.map { marker ->
                    MKPointAnnotation().apply {
                        setCoordinate(CLLocationCoordinate2DMake(marker.latitude, marker.longitude))
                        setTitle(marker.name)
                        setSubtitle(marker.location)
                    }
                }
                addAnnotations(annotations)
                showAnnotations(annotations, animated = false)
                delegate = CardsMapDelegate()
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private class CardsMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: platform.MapKit.MKAnnotationProtocol,
    ): platform.MapKit.MKAnnotationView? {
        val identifier = "cardPin"
        val pinView = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier) as? MKPinAnnotationView
            ?: MKPinAnnotationView(annotation = viewForAnnotation, reuseIdentifier = identifier)

        pinView.annotation = viewForAnnotation
        pinView.canShowCallout = true
        pinView.pinTintColor = platform.UIKit.UIColor.redColor

        return pinView
    }
}
