/*
 * TripMapActivity.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class TripMapActivity extends Activity {

    public static final String TRIP_EXTRA = "trip";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Trip trip = getIntent().getParcelableExtra(TRIP_EXTRA);
        if (trip == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_trip_map);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(Trip.formatStationNames(trip));
        actionBar.setSubtitle((trip.getRouteName() == null) ? trip.getAgencyName()
                : String.format("%s %s", trip.getAgencyName(), trip.getRouteName()));

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                populateMap(googleMap, trip);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    private void populateMap(@NonNull GoogleMap map, @NonNull Trip trip) {
        @DrawableRes int startMarkerId = R.drawable.marker_start;
        @DrawableRes int endMarkerId = R.drawable.marker_end;

        /* FIXME: Need icons...
        if (trip.getMode() == Trip.Mode.BUS) {
            startMarkerId = R.drawable.marker_bus_start;
            endMarkerId   = R.drawable.marker_bus_end;

        } else if (trip.getMode() == Trip.Mode.TRAIN) {
            startMarkerId = R.drawable.marker_train_start;
            endMarkerId   = R.drawable.marker_train_end;

        } else if (trip.getMode() == Trip.Mode.TRAM) {
            startMarkerId = R.drawable.marker_tram_start;
            endMarkerId   = R.drawable.marker_tram_end;

        } else if (trip.getMode() == Trip.Mode.METRO) {
            startMarkerId = R.drawable.marker_metro_start;
            endMarkerId   = R.drawable.marker_metro_end;

        } else if (trip.getMode() == Trip.Mode.FERRY) {
            startMarkerId = R.drawable.marker_ferry_start;
            endMarkerId   = R.drawable.marker_ferry_end;
        }
        */

        final List<LatLng> points = new ArrayList<>();
        LatLngBounds.Builder builder = LatLngBounds.builder();

        if (trip.getStartStation() != null) {
            LatLng startStationLatLng = addStationMarker(map, trip.getStartStation(), startMarkerId);
            builder.include(startStationLatLng);
            points.add(startStationLatLng);
        }

        if (trip.getEndStation() != null) {
            LatLng endStationLatLng = addStationMarker(map, trip.getEndStation(), endMarkerId);
            builder.include(endStationLatLng);
            points.add(endStationLatLng);
        }

        final LatLngBounds bounds = builder.build();
        if (points.size() == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 17));
        } else {
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = getResources().getDimensionPixelSize(R.dimen.map_padding);
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
        }
    }

    @NonNull
    private LatLng addStationMarker(@NonNull GoogleMap map, @NonNull Station station, @DrawableRes int iconId) {
        LatLng pos = new LatLng(Double.valueOf(station.getLatitude()), Double.valueOf(station.getLongitude()));
        map.addMarker(new MarkerOptions()
                .position(pos)
                .title(station.getStationName())
                .snippet(station.getCompanyName())
                .icon(BitmapDescriptorFactory.fromResource(iconId))
        );
        return pos;
    }
}
