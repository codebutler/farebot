/*
 * CardInfoActivity.java
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;

import java.text.DateFormat;
import java.util.Date;

public class CardInfoActivity extends ListActivity
{
    private MifareCard mCard;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_info);

        Uri uri = getIntent().getData();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        startManagingCursor(cursor);
        cursor.moveToFirst();

        int    type   = cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE));
        String serial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL));
        String data   = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));

        try {
            mCard = MifareCard.fromXml(data);
        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
            return;
        }

        TransitData transitData = mCard.parseTransitData();
        if (transitData == null) {
            showAdvancedInfo();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.card_name_text_view)).setText(transitData.getCardName());
        ((TextView) findViewById(R.id.serial_text_view)).setText(String.valueOf(transitData.getSerialNumber()));
        ((TextView) findViewById(R.id.balance_text_view)).setText(transitData.getBalanceString());

        if (transitData.getTrips() != null)
            setListAdapter(new UseLogListAdapter(this, transitData.getTrips()));
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        Trip trip = (Trip) getListAdapter().getItem(position);
        Station station = trip.getStation();
        if (station != null) {
            Uri uri = Uri.parse(String.format("geo:0,0?q=%s,%s (%s)", station.getLatitude(), station.getLongitude(), station.getName()));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        getMenuInflater().inflate(R.menu.card_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        if (item.getItemId() == R.id.advanced_info) {
            showAdvancedInfo();
            return true;
        }
        return false;
    }

    private void showAdvancedInfo ()
    {
        Intent intent = new Intent(this, AdvancedCardInfoActivity.class);
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD, mCard);
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

            Trip trip = (Trip) getItem(position);

            Date date = new Date(trip.getTimestamp() * 1000);

            TextView dateTextView    = (TextView) convertView.findViewById(R.id.date_text_view);
            TextView timeTextView    = (TextView) convertView.findViewById(R.id.time_text_view);
            TextView routeTextView   = (TextView) convertView.findViewById(R.id.route_text_view);
            TextView fareTextView    = (TextView) convertView.findViewById(R.id.fare_text_view);
            TextView stationTextView = (TextView) convertView.findViewById(R.id.station_text_view);

            dateTextView.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(date));
            timeTextView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(date));
            routeTextView.setText(trip.getShortAgencyName() + " " + trip.getRouteName());

            if (trip.getFare() != 0) {
                fareTextView.setText(trip.getFareString());
            } else {
                fareTextView.setText("");
            }

            stationTextView.setText(trip.getStationName());

            return convertView;
        }
    }
}
