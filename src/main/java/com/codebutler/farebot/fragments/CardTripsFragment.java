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

package com.codebutler.farebot.fragments;

import android.app.Activity;
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
import com.actionbarsherlock.app.SherlockListFragment;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activities.AdvancedCardInfoActivity;
import com.codebutler.farebot.activities.CardInfoActivity;
import com.codebutler.farebot.activities.TripMapActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.OVChipTrip;
import com.codebutler.farebot.transit.OrcaTransitData;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardTripsFragment extends SherlockListFragment {
    private Card        mCard;
    private TransitData mTransitData;

    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCard        = (Card)        getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        mTransitData = (TransitData) getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_trips, null);

        if (mTransitData.getTrips() != null && mTransitData.getTrips().length > 0)
            setListAdapter(new UseLogListAdapter(getActivity(), mTransitData.getTrips()));
        else {
            view.findViewById(android.R.id.list).setVisibility(View.GONE);
            view.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Trip trip = (Trip) getListAdapter().getItem(position);

        if (!(Trip.hasLocation(trip.getStartStation())) && (!Trip.hasLocation(trip.getEndStation())))
            return;
        
        Intent intent = new Intent(getActivity(), TripMapActivity.class);
        intent.putExtra(TripMapActivity.TRIP_EXTRA, trip);
        startActivity(intent);
    }

    private static class UseLogListAdapter extends ArrayAdapter<Trip> {
        public UseLogListAdapter (Context context, Trip[] items) {
            super(context, 0, items);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            Activity activity = (Activity) getContext();
            LayoutInflater inflater = activity.getLayoutInflater();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.trip_item, parent, false);
            }

            Trip trip = getItem(position);

            Date date = new Date(trip.getTimestamp() * 1000);

            View listHeader = convertView.findViewById(R.id.list_header);
            if (isFirstInSection(position)) {
                listHeader.setVisibility(View.VISIBLE);
                ((TextView) listHeader.findViewById(android.R.id.text1)).setText(DateFormat.getDateInstance(DateFormat.LONG).format(date));
            } else {
                listHeader.setVisibility(View.GONE);
            }
            
            convertView.findViewById(R.id.list_divider).setVisibility(isLastInSection(position) ? View.INVISIBLE : View.VISIBLE);

            ImageView iconImageView   = (ImageView) convertView.findViewById(R.id.icon_image_view);
            TextView  timeTextView    = (TextView)  convertView.findViewById(R.id.time_text_view);
            TextView  routeTextView   = (TextView)  convertView.findViewById(R.id.route_text_view);
            TextView  fareTextView    = (TextView)  convertView.findViewById(R.id.fare_text_view);
            TextView  stationTextView = (TextView)  convertView.findViewById(R.id.station_text_view);

            if (trip.getMode() == Trip.Mode.BUS) {
                iconImageView.setImageResource(R.drawable.bus);
            } else if (trip.getMode() == Trip.Mode.TRAIN) {
                iconImageView.setImageResource(R.drawable.train);
            } else if (trip.getMode() == Trip.Mode.TRAM) {
                iconImageView.setImageResource( R.drawable.tram);
            } else if (trip.getMode() == Trip.Mode.METRO) {
                iconImageView.setImageResource(R.drawable.metro);
            } else if (trip.getMode() == Trip.Mode.FERRY) {
                iconImageView.setImageResource(R.drawable.ferry);
            } else if (trip.getMode() == Trip.Mode.TICKET_MACHINE) {
                iconImageView.setImageResource(R.drawable.tvm);
            } else if (trip.getMode() == Trip.Mode.VENDING_MACHINE) {
                iconImageView.setImageResource(R.drawable.vending_machine);
            } else if (trip.getMode() == Trip.Mode.POS) {
                iconImageView.setImageResource(R.drawable.cashier);
            } else if (trip.getMode() == Trip.Mode.BANNED) {
                iconImageView.setImageResource(R.drawable.banned);
        	} else {
                iconImageView.setImageResource(R.drawable.unknown);
            }

            if (trip.hasTime()) {
                timeTextView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(date));
                timeTextView.setVisibility(View.VISIBLE);
            } else {
                timeTextView.setVisibility(View.INVISIBLE);
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
                routeTextView.setVisibility(View.INVISIBLE);
            }

            if (trip.getFare() != 0) {
                fareTextView.setText(trip.getFareString());
            } else if (trip instanceof OrcaTransitData.OrcaTrip) {
                fareTextView.setText(R.string.pass_or_transfer);
            } else if (trip instanceof OVChipTrip) {
                fareTextView.setText(trip.getFareString());
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

        @Override
        public boolean isEnabled(int position) {
            Trip trip = getItem(position);
            return Trip.hasLocation(trip.getStartStation()) || Trip.hasLocation(trip.getEndStation());
        }

        private boolean isFirstInSection(int position) {
            if (position == 0) return true;
            
            Date date1 = new Date(getItem(position).getTimestamp() * 1000);
            Date date2 = new Date(getItem(position - 1).getTimestamp() * 1000);

            return ((date1.getYear() != date2.getYear()) || (date1.getMonth() != date2.getMonth()) || (date1.getDay() != date2.getDay()));
        }
        
        public boolean isLastInSection(int position) {
            if (position == getCount() - 1) return true;

            Date date1 = new Date(getItem(position).getTimestamp() * 1000);
            Date date2 = new Date(getItem(position + 1).getTimestamp() * 1000);

            return ((date1.getYear() != date2.getYear()) || (date1.getMonth() != date2.getMonth()) || (date1.getDay() != date2.getDay()));
        }
    }
}