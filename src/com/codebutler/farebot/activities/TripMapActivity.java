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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.android.maps.*;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TripMapActivity extends MapActivity {
    public static final String TRIP_EXTRA = "trip";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_map);

        MapView mapView = (MapView) findViewById(R.id.map_view);
        mapView.setBuiltInZoomControls(true);

        Trip trip = (Trip) getIntent().getParcelableExtra(TRIP_EXTRA);
        Date date = new Date(trip.getTimestamp() * 1000);


        ((TextView) findViewById(R.id.route_text_view)).setText(trip.getAgencyName()); // FIXME + " " + trip.getRouteName());
        ((TextView) findViewById(R.id.stations_text_view)).setText(Trip.formatStationNames(trip));
        ((TextView) findViewById(R.id.datetime_text_view)).setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date));
        ((TextView) findViewById(R.id.fare_text_view)).setText(trip.getFareString());

        int startMarkerId = R.drawable.marker_start;
        int endMarkerId   = R.drawable.marker_end;

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

        StationItemizedOverlay stationsOverlay = new StationItemizedOverlay(null);
        stationsOverlay.addStation(trip.getStartStation(), startMarkerId);
        stationsOverlay.addStation(trip.getEndStation(),   endMarkerId);

        mapView.getOverlays().add(stationsOverlay);

        if (stationsOverlay.getCenter() != null) {
            MapController controller = mapView.getController();
            controller.setCenter(stationsOverlay.getCenter());
            controller.zoomToSpan(stationsOverlay.getLatSpanE6(), stationsOverlay.getLatSpanE6());
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private class StationItemizedOverlay extends ItemizedOverlay<OverlayItem> {
        private ArrayList<OverlayItem> mMapOverlays = new ArrayList<OverlayItem>();

        public StationItemizedOverlay(Drawable drawable) {
            super(null);
            populate();
        }

        @Override
        protected OverlayItem createItem(int i) {
            return mMapOverlays.get(i);
        }

        @Override
        public int size() {
            return mMapOverlays.size();
        }

        private void addStation(Station station, int markerId) {
            if (station == null || station.getLatitude() == null || station.getLongitude() == null)
                return;

            GeoPoint geoPoint = new GeoPoint((int) (Double.valueOf(station.getLatitude()) * 1e6), (int) (Double.valueOf(station.getLongitude()) * 1e6));

            Log.d("TripMapActivity", "Adding point: " + geoPoint.toString());

            OverlayItem item = new OverlayItem(geoPoint, station.getStationName(), station.getCompanyName());
            Drawable drawable = getResources().getDrawable(markerId);
            // drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable = boundCenterBottom(drawable);
            item.setMarker(drawable);

            mMapOverlays.add(item);
            populate();
        }
    }
}