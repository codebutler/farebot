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
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.transit.SuicaTransitData;
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Trip trip = (Trip) getListAdapter().getItem(position);

        if (!(Trip.hasLocation(trip.getStartStation())) && (!Trip.hasLocation(trip.getEndStation())))
            return;
        
        Intent intent = new Intent(this, TripMapActivity.class);
        intent.putExtra(TripMapActivity.TRIP_EXTRA, trip);
        startActivity(intent);
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

            Trip trip = getItem(position);
            
            boolean hasTime = (!(trip instanceof SuicaTransitData.SuicaTrip) || ((SuicaTransitData.SuicaTrip)trip).hasTime());

            Date date = new Date(trip.getTimestamp() * 1000);

            ImageView iconImageView   = (ImageView) convertView.findViewById(R.id.icon_image_view);
            TextView  dateTextView    = (TextView)  convertView.findViewById(R.id.date_text_view);
            TextView  timeTextView    = (TextView)  convertView.findViewById(R.id.time_text_view);
            TextView  routeTextView   = (TextView)  convertView.findViewById(R.id.route_text_view);
            TextView  fareTextView    = (TextView)  convertView.findViewById(R.id.fare_text_view);
            TextView  stationTextView = (TextView)  convertView.findViewById(R.id.station_text_view);

            if (trip.getMode() == Trip.Mode.BUS) {
                iconImageView.setImageResource(R.drawable.bus);
            } else if (trip.getMode() == Trip.Mode.TRAIN) {
                iconImageView.setImageResource(R.drawable.train);
            } else if (trip.getMode() == Trip.Mode.TRAM) {
                iconImageView.setImageResource(R.drawable.tram);
            } else if (trip.getMode() == Trip.Mode.METRO) {
                iconImageView.setImageResource(R.drawable.metro);
            } else if (trip.getMode() == Trip.Mode.FERRY) {
                iconImageView.setImageResource(R.drawable.ferry);
            } else {
                iconImageView.setImageDrawable(null); // FIXME
            }

            dateTextView.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(date));
            
            if (hasTime) {
                timeTextView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(date));
                timeTextView.setVisibility(View.VISIBLE);
            } else {
                timeTextView.setVisibility(View.GONE);
            }

            List<String> routeText = new ArrayList<String>();
            if (trip.getShortAgencyName() != null)
                routeText.add("<b>" + trip.getShortAgencyName() + "</b>");
            if (trip.getRouteName() != null)
                routeText.add(trip.getRouteName());
            
            if (routeText.size() > 0) {
                routeTextView.setText(Html.fromHtml(StringUtils.join(routeText, " ")));
                routeTextView.setVisibility(View.VISIBLE);
            } else {
                routeTextView.setVisibility(View.GONE);
            }

            if (trip.getFare() != 0) {
                fareTextView.setText(trip.getFareString());
            } else {
                fareTextView.setText("");
            }

            String stationText = Trip.formatStationNames(trip);
            if (stationText != null) {
                stationTextView.setText(stationText);
                stationTextView.setVisibility(View.VISIBLE);
            } else {
                stationTextView.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}