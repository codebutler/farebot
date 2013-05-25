/*
 * CardRefillsActivity.java
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activities.CardInfoActivity;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.TransitData;

import java.text.DateFormat;
import java.util.Date;

public class CardRefillsFragment extends SherlockListFragment {
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TransitData transitData = getArguments().getParcelable(CardInfoActivity.EXTRA_TRANSIT_DATA);
        setListAdapter(new RefillsListAdapter(getActivity(), transitData.getRefills()));
    }

    private static class RefillsListAdapter extends ArrayAdapter<Refill> {
        public RefillsListAdapter (Context context, Refill[] refills) {
            super(context, 0, refills);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            Activity activity = (Activity) getContext();
            LayoutInflater inflater = activity.getLayoutInflater();

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.refill_item, null);
            }

            Refill refill = (Refill) getItem(position);
            Date date = new Date(refill.getTimestamp() * 1000);

            TextView dateTimeTextView = (TextView) convertView.findViewById(R.id.datetime_text_view);
            TextView agencyTextView   = (TextView) convertView.findViewById(R.id.agency_text_view);
            TextView amountTextView   = (TextView) convertView.findViewById(R.id.amount_text_view);

            dateTimeTextView.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date));
            agencyTextView.setText(refill.getShortAgencyName());
            amountTextView.setText(refill.getAmountString());

            return convertView;
        }
    }
}