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

import android.app.TabActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.TransitData;

public class CardInfoActivity extends TabActivity
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

        TransitData transitData = null;
        try {
            transitData = mCard.parseTransitData();
        } catch (Exception ex) {
            showAdvancedInfo(Utils.getErrorMessage(ex));
            finish();
            return;
        }

        if (transitData == null) {
            showAdvancedInfo("Unknown card data. Only ORCA and Clipper cards are currently supported.");
            finish();
            return;
        }

        ((TextView) findViewById(R.id.card_name_text_view)).setText(transitData.getCardName());
        ((TextView) findViewById(R.id.serial_text_view)).setText(String.valueOf(transitData.getSerialNumber()));
        ((TextView) findViewById(R.id.balance_text_view)).setText(transitData.getBalanceString());

        TabSpec tabSpec;
        Intent  intent;

        intent = new Intent(this, CardTripsActivity.class);
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD, mCard);

        tabSpec = getTabHost().newTabSpec("trips")
            .setContent(intent)
            .setIndicator(getString(R.string.recent_trips));
        getTabHost().addTab(tabSpec);

        intent = new Intent(this, CardRefillsActivity.class);
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD, mCard);
        
        tabSpec = getTabHost().newTabSpec("refills")
            .setContent(intent)
            .setIndicator(getString(R.string.recent_refills));
        getTabHost().addTab(tabSpec);

        if (transitData.getRefills() == null) {
            findViewById(R.id.recent_trips_header).setVisibility(View.VISIBLE);
            getTabHost().getTabWidget().setVisibility(View.GONE);
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
            showAdvancedInfo(null);
            return true;
        }
        return false;
    }

    private void showAdvancedInfo (String message)
    {
        Intent intent = new Intent(this, AdvancedCardInfoActivity.class);
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD, mCard);
        if (message != null)
            intent.putExtra(AdvancedCardInfoActivity.EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
