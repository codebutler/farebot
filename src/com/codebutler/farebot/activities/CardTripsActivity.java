/*
 * CardTripsActivity.java
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

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardTripsActivity extends ListActivity
{
    private Card mCard;

    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_trips);
        registerForContextMenu(findViewById(android.R.id.list));

        mCard = (Card) getIntent().getParcelableExtra(AdvancedCardInfoActivity.EXTRA_CARD);

        TransitData transitData = mCard.parseTransitData();

        if (transitData.getTrips() != null)
            setListAdapter(new UseLogListAdapter(this, transitData.getTrips()));
        else {
            findViewById(android.R.id.list).setVisibility(View.GONE);
            findViewById(R.id.error_text).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        int position = ((AdapterContextMenuInfo) menuInfo).position;
        Trip trip = (Trip) getListAdapter().getItem(position);

        if (trip.getStartStation() != null)
            menu.add(Menu.NONE, 1, 1, String.format("Map of %s", trip.getStartStation().getStationName()));

        if (trip.getEndStation() != null)
            menu.add(Menu.NONE, 2, 2, String.format("Map of %s", trip.getEndStation().getStationName()));
    }

    @Override
    public boolean onContextItemSelected (MenuItem item)
    {
        int position = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
        Trip trip = (Trip) getListAdapter().getItem(position);

        Station station = (item.getItemId() == 1) ? trip.getStartStation() : trip.getEndStation();

        Uri uri = Uri.parse(String.format("geo:0,0?q=%s,%s (%s)", station.getLatitude(), station.getLongitude(), station.getStationName()));
        startActivity(new Intent(Intent.ACTION_VIEW, uri));

        return true;
    }

    private static class UseLogListAdapter extends ArrayAdapter<Trip>
    {
        public UseLogListAdapter (Context context, Trip[] items)
        {
            super(context, 0, items);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent)
        {
            Activity activity = (Activity) getContext();
            LayoutInflater inflater = activity.getLayoutInflater();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.trip_item, null);
            }

            Trip trip = (Trip) getItem(position);

            Date date = new Date(trip.getTimestamp() * 1000);

            TextView dateTextView    = (TextView) convertView.findViewById(R.id.date_text_view);
            TextView timeTextView    = (TextView) convertView.findViewById(R.id.time_text_view);
            TextView routeTextView   = (TextView) convertView.findViewById(R.id.route_text_view);
            TextView fareTextView    = (TextView) convertView.findViewById(R.id.fare_text_view);
            TextView stationTextView = (TextView) convertView.findViewById(R.id.station_text_view);

            dateTextView.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(date));
            timeTextView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(date));

            List<String> routeText = new ArrayList<String>();
            if (trip.getShortAgencyName() != null)
                routeText.add(trip.getShortAgencyName());
            if (trip.getRouteName() != null)
                routeText.add(trip.getRouteName());
            
            if (routeText.size() > 0) {
                routeTextView.setText(StringUtils.join(routeText, " "));
                routeTextView.setVisibility(View.VISIBLE);
            } else {
                routeTextView.setVisibility(View.GONE);
            }

            if (trip.getFare() != 0) {
                fareTextView.setText(trip.getFareString());
            } else {
                fareTextView.setText("");
            }

            List<String> stationText = new ArrayList<String>();
            if (trip.getStartStationName() != null)
                stationText.add(trip.getStartStationName());
            if (trip.getEndStationName() != null && (!trip.getEndStationName().equals(trip.getStartStationName())))
                stationText.add(trip.getEndStationName());

            if (stationText.size() > 0) {
                stationTextView.setText(StringUtils.join(stationText, " → "));
                stationTextView.setVisibility(View.VISIBLE);
            } else {
                stationTextView.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}