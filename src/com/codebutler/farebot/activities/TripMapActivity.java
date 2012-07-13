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
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import java.util.ArrayList;

public class TripMapActivity extends SherlockMapActivity {
    public static final String TRIP_EXTRA = "trip";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_map);

        MapView mapView = (MapView) findViewById(R.id.map_view);
        mapView.setBuiltInZoomControls(true);

        Trip trip = (Trip) getIntent().getParcelableExtra(TRIP_EXTRA);

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

        StationItemizedOverlay stationsOverlay = new StationItemizedOverlay(null);
        stationsOverlay.addStation(trip.getStartStation(), startMarkerId);
        stationsOverlay.addStation(trip.getEndStation(),   endMarkerId);

        mapView.getOverlays().add(stationsOverlay);

        if (stationsOverlay.getCenter() != null) {
            MapController controller = mapView.getController();
            centerAroundOverlayItems(controller, stationsOverlay);
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
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
            item.setMarker(boundCenterBottom(getResources().getDrawable(markerId)));

            mMapOverlays.add(item);
            populate();
        }
    }

    // https://gist.github.com/1385260 
    private static void centerAroundOverlayItems(MapController controller, StationItemizedOverlay itemizedOverlay) {
    	if (itemizedOverlay.size() == 0) return;

    	double hpadding = 0.1;
    	double vpadding = 0.15;

    	//start wide
    	int minLatitude = (int)(90 * 1E6);
    	int maxLatitude = (int)(-90 * 1E6);
    	int minLongitude = (int)(180 * 1E6);
    	int maxLongitude = (int)(-180 * 1E6);

        for (int i = 0; i < itemizedOverlay.size(); i++) {
            OverlayItem item = itemizedOverlay.getItem(i);

    		int lat = item.getPoint().getLatitudeE6();
    		int lon = item.getPoint().getLongitudeE6();
    		//narrow down
    		maxLatitude = Math.max(lat, maxLatitude);
    		minLatitude = Math.min(lat, minLatitude);
    		maxLongitude = Math.max(lon, maxLongitude);
    		minLongitude = Math.min(lon, minLongitude);
    	}

    	maxLatitude = maxLatitude + (int)((maxLatitude - minLatitude) * hpadding);
    	minLatitude = minLatitude - (int)((maxLatitude - minLatitude) * hpadding);

    	maxLongitude = maxLongitude + (int)((maxLongitude - minLongitude) * vpadding);
    	minLongitude = minLongitude - (int)((maxLongitude - minLongitude) * vpadding);

    	controller.zoomToSpan(maxLatitude - minLatitude, maxLongitude - minLongitude); //this will zoom to nearest zoom level
    	controller.animateTo( //go to the center of the span
            new GeoPoint(
                (maxLatitude + minLatitude) / 2,
                (maxLongitude + minLongitude) / 2
            ));
    }
}