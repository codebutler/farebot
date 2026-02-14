package com.codebutler.farebot.shared.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPinAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformCardsMap(
    markers: List<CardsMapMarker>,
    modifier: Modifier,
    onMarkerTap: ((String) -> Unit)?,
    focusMarkers: List<CardsMapMarker>,
    topPadding: Dp,
) {
    if (markers.isEmpty()) return

    val delegate = remember { CardsMapDelegate() }
    delegate.onMarkerTap = onMarkerTap

    val annotations = remember(markers) {
        markers.map { marker ->
            MKPointAnnotation().apply {
                setCoordinate(CLLocationCoordinate2DMake(marker.latitude, marker.longitude))
                setTitle(marker.name)
                setSubtitle(marker.location)
            }
        }
    }

    val topInset = topPadding.value.toDouble()

    UIKitView(
        factory = {
            MKMapView().apply {
                layoutMargins = UIEdgeInsetsMake(topInset, 0.0, 0.0, 0.0)
                addAnnotations(annotations)
                showAnnotations(annotations, animated = false)
                this.delegate = delegate
            }
        },
        update = { mapView ->
            mapView.layoutMargins = UIEdgeInsetsMake(topInset, 0.0, 0.0, 0.0)
            if (focusMarkers.isNotEmpty()) {
                val focusNames = focusMarkers.map { it.name }.toSet()
                val focusAnnotations = annotations.filter { it.title in focusNames }
                if (focusAnnotations.isNotEmpty()) {
                    if (focusAnnotations.size == 1) {
                        val coord = focusAnnotations.first().coordinate
                        val span = platform.MapKit.MKCoordinateSpanMake(2.0, 2.0)
                        val region = platform.MapKit.MKCoordinateRegionMake(coord, span)
                        mapView.setRegion(region, animated = true)
                    } else {
                        var rect = platform.MapKit.MKMapPointForCoordinate(focusAnnotations.first().coordinate).useContents {
                            platform.MapKit.MKMapRectMake(x, y, 0.0, 0.0)
                        }
                        focusAnnotations.drop(1).forEach { ann ->
                            val pointRect = platform.MapKit.MKMapPointForCoordinate(ann.coordinate).useContents {
                                platform.MapKit.MKMapRectMake(x, y, 0.0, 0.0)
                            }
                            rect = platform.MapKit.MKMapRectUnion(rect, pointRect)
                        }
                        val padding = UIEdgeInsetsMake(20.0, 20.0, 20.0, 20.0)
                        mapView.setVisibleMapRect(rect, edgePadding = padding, animated = true)
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private class CardsMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    var onMarkerTap: ((String) -> Unit)? = null

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

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: platform.MapKit.MKAnnotationView) {
        val title = didSelectAnnotationView.annotation?.title ?: return
        onMarkerTap?.invoke(title)
    }
}
