/*
 * TripMapActivity.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.activities;

import android.os.Bundle;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class TripMapActivity extends SherlockFragmentActivity {
    public static final String TRIP_EXTRA = "trip";

    private GoogleMap mMap;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Trip trip = getIntent().getParcelableExtra(TRIP_EXTRA);
        if (trip == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_trip_map);

        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.getUiSettings().setZoomControlsEnabled(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(Trip.formatStationNames(trip));
        actionBar.setSubtitle((trip.getRouteName() == null) ? trip.getAgencyName() : String.format("%s %s", trip.getAgencyName(), trip.getRouteName()));

        int startMarkerId = R.drawable.marker_start;
        int endMarkerId   = R.drawable.marker_end;

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

        final List<LatLng> points = new ArrayList<LatLng>();
        LatLngBounds.Builder builder = LatLngBounds.builder();

        if (trip.getStartStation() != null) {
            LatLng startStationLatLng = addStationMarker(trip.getStartStation(), startMarkerId);
            builder.include(startStationLatLng);
            points.add(startStationLatLng);
        }

        if (trip.getEndStation() != null) {
            LatLng endStationLatLng = addStationMarker(trip.getEndStation(), endMarkerId);
            builder.include(endStationLatLng);
            points.add(endStationLatLng);
        }

        final LatLngBounds bounds = builder.build();
        findViewById(R.id.map).post(new Runnable() {
            @Override
            public void run() {
                if (points.size() == 1) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 17));
                } else {
                    int padding = getResources().getDimensionPixelSize(R.dimen.map_padding);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                }
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

    private LatLng addStationMarker(Station station, int iconId) {
        LatLng pos = new LatLng(Double.valueOf(station.getLatitude()), Double.valueOf(station.getLongitude()));
        mMap.addMarker(new MarkerOptions()
            .position(pos)
            .title(station.getStationName())
            .snippet(station.getCompanyName())
            .icon(BitmapDescriptorFactory.fromResource(iconId))
        );
        return pos;
    }
}