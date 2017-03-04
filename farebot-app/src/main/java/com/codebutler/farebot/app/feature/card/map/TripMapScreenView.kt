/*
 * TripMapScreenView.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.app.feature.card.map

import android.content.Context
import android.os.Bundle
import android.support.annotation.DrawableRes
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Trip
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.wealthfront.magellan.BaseScreenView
import java.util.ArrayList

class TripMapScreenView(
        context: Context,
        val trip : Trip)
    : BaseScreenView<TripMapScreen>(context) {

    private val mapView: MapView by bindView(R.id.map)

    init {
        inflate(context, R.layout.screen_trip_map, this)

        mapView.getMapAsync { map ->
            map.uiSettings.isZoomControlsEnabled = false
            populateMap(map, trip)
        }
    }

    fun onCreate(bundle: Bundle) {
        mapView.onCreate(bundle)
    }

    fun onDestroy() {
        mapView.onDestroy()
    }

    fun onPause() {
        mapView.onPause()
    }

    fun onResume() {
        mapView.onResume()
    }

    fun onStart() {
        mapView.onStart()
    }

    fun onStop() {
        mapView.onStop()
    }

    private fun populateMap(map: GoogleMap, trip: Trip) {
        val startMarkerId = R.drawable.marker_start
        val endMarkerId = R.drawable.marker_end

        val points = ArrayList<LatLng>()
        val builder = LatLngBounds.builder()

        val startStation = trip.startStation
        if (startStation != null) {
            val startStationLatLng = addStationMarker(map, startStation, startMarkerId)
            builder.include(startStationLatLng)
            points.add(startStationLatLng)
        }

        val endStation = trip.endStation
        if (endStation != null) {
            val endStationLatLng = addStationMarker(map, endStation, endMarkerId)
            builder.include(endStationLatLng)
            points.add(endStationLatLng)
        }

        if (points.isNotEmpty()) {
            val bounds = builder.build()
            if (points.size == 1) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(points[0], 17f))
            } else {
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                val padding = resources.getDimensionPixelSize(R.dimen.map_padding)
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding))
            }
        }
    }

    private fun addStationMarker(map: GoogleMap, station: Station, @DrawableRes iconId: Int): LatLng {
        val pos = LatLng(station.latitude?.toDoubleOrNull() ?: 0.0, station.longitude?.toDoubleOrNull() ?: 0.0)
        map.addMarker(MarkerOptions()
                .position(pos)
                .title(station.stationName)
                .snippet(station.companyName)
                .icon(BitmapDescriptorFactory.fromResource(iconId)))
        return pos
    }
}
