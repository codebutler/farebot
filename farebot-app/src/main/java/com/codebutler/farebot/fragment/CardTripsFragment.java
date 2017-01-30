/*
 * CardTripsFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.AdvancedCardInfoActivity;
import com.codebutler.farebot.activity.CardInfoActivity;
import com.codebutler.farebot.activity.TripMapActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.RefillTrip;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.orca.OrcaTrip;
import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CardTripsFragment extends ListFragment {
    private Card mCard;
    private TransitData mTransitData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCard = getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        mTransitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_card_trips, null);

        List<Trip> trips = new ArrayList<>();
        if (mTransitData.getTrips() != null && mTransitData.getTrips().size() > 0) {
            for (Trip t : mTransitData.getTrips()) {
                trips.add(t);
            }
        }

        // This is for "legacy" implementations which have a separate list of refills.
        if (mTransitData.getRefills() != null && mTransitData.getRefills().size() > 0) {
            for (Refill r : mTransitData.getRefills()) {
                trips.add(RefillTrip.create(r));
            }
        }

        // Explicitly sort these events
        Collections.sort(trips, new Trip.Comparator());

        if (trips.size() > 0) {
            setListAdapter(new UseLogListAdapter(getActivity(), trips.toArray(new Trip[trips.size()])));
        } else {
            view.findViewById(android.R.id.list).setVisibility(View.GONE);
            view.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Trip trip = (Trip) getListAdapter().getItem(position);
        if (trip == null
                || !((trip.getStartStation() != null && trip.getStartStation().hasLocation())
                || (trip.getEndStation() != null && trip.getEndStation().hasLocation()))) {
            return;
        }

        Intent intent = new Intent(getActivity(), TripMapActivity.class);
        intent.putExtra(TripMapActivity.TRIP_EXTRA, trip);
        startActivity(intent);
    }

    private static class UseLogListAdapter extends ArrayAdapter<Trip> {
        UseLogListAdapter(Context context, Trip[] items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Activity activity = (Activity) getContext();
            LayoutInflater inflater = activity.getLayoutInflater();
            Resources resources = activity.getResources();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.trip_item, parent, false);
            }

            Trip trip = getItem(position);

            Date date = new Date(trip.getTimestamp() * 1000);

            View listHeader = convertView.findViewById(R.id.list_header);
            if (isFirstInSection(position)) {
                listHeader.setVisibility(View.VISIBLE);
                ((TextView) listHeader.findViewById(android.R.id.text1))
                        .setText(DateFormat.getLongDateFormat(getContext()).format(date));
            } else {
                listHeader.setVisibility(View.GONE);
            }

            convertView.findViewById(R.id.list_divider).setVisibility(isLastInSection(position)
                    ? View.INVISIBLE : View.VISIBLE);

            final ImageView iconImageView = (ImageView) convertView.findViewById(R.id.icon_image_view);
            final TextView timeTextView = (TextView) convertView.findViewById(R.id.time_text_view);
            final TextView routeTextView = (TextView) convertView.findViewById(R.id.route_text_view);
            final TextView fareTextView = (TextView) convertView.findViewById(R.id.fare_text_view);
            final TextView stationTextView = (TextView) convertView.findViewById(R.id.station_text_view);

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
                timeTextView.setText(Utils.timeFormat(getContext(), date));
                timeTextView.setVisibility(View.VISIBLE);
            } else {
                timeTextView.setVisibility(View.INVISIBLE);
            }

            List<String> routeText = new ArrayList<>();
            if (trip.getShortAgencyName(resources) != null) {
                routeText.add("<b>" + trip.getShortAgencyName(resources) + "</b>");
            }
            if (trip.getRouteName(resources) != null) {
                routeText.add(trip.getRouteName(resources));
            }

            if (routeText.size() > 0) {
                routeTextView.setText(Html.fromHtml(StringUtils.join(routeText, " ")));
                routeTextView.setVisibility(View.VISIBLE);
            } else {
                routeTextView.setVisibility(View.INVISIBLE);
            }

            fareTextView.setVisibility(View.VISIBLE);
            if (trip.hasFare()) {
                fareTextView.setText(trip.getFareString(resources));
            } else if (trip instanceof OrcaTrip) {
                fareTextView.setText(R.string.pass_or_transfer);
            } else {
                // Hide the text "Fare" for hasFare == false
                fareTextView.setVisibility(View.INVISIBLE);
            }

            String stationText = Trip.formatStationNames(activity, trip);
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
            if (trip == null) {
                return false;
            }
            return (trip.getStartStation() != null && trip.getStartStation().hasLocation())
                    || (trip.getEndStation() != null && trip.getEndStation().hasLocation());
        }

        private boolean isFirstInSection(int position) {
            if (position == 0) {
                return true;
            }

            Date date1 = new Date(getItem(position).getTimestamp() * 1000);
            Date date2 = new Date(getItem(position - 1).getTimestamp() * 1000);

            return ((date1.getYear() != date2.getYear()) || (date1.getMonth() != date2.getMonth())
                    || (date1.getDate() != date2.getDate()));
        }

        public boolean isLastInSection(int position) {
            if (position == getCount() - 1) {
                return true;
            }

            Date date1 = new Date(getItem(position).getTimestamp() * 1000);
            Date date2 = new Date(getItem(position + 1).getTimestamp() * 1000);

            return ((date1.getYear() != date2.getYear()) || (date1.getMonth() != date2.getMonth())
                    || (date1.getDate() != date2.getDate()));
        }
    }
}
