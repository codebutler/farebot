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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.TransitData;

public class CardInfoActivity extends TabActivity
{
    public static final String SPEAK_BALANCE_EXTRA = "com.codebutler.farebot.speak_balance";

    private Card mCard;
    private TransitData mTransitData;

    private TextToSpeech   mTTS = null;
    private OnInitListener mTTSInitListener = new OnInitListener() {
        public void onInit (int status) {
            if (status == TextToSpeech.SUCCESS) {
                mTTS.speak(getString(R.string.balance_speech, mTransitData.getBalanceString()), TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    };

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
            mCard = Card.fromXml(data);
        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
            return;
        }

        try {
            mTransitData = mCard.parseTransitData();
        } catch (Exception ex) {
            Log.e("CardInfoActivity", "Error parsing transit data", ex);
            showAdvancedInfo(Utils.getErrorMessage(ex));
            finish();
            return;
        }

        if (mTransitData == null) {
            showAdvancedInfo("Unsupported card data.");
            finish();
            return;
        }

        ((TextView) findViewById(R.id.card_name_text_view)).setText(mTransitData.getCardName());
        ((TextView) findViewById(R.id.serial_text_view)).setText(mTransitData.getSerialNumber());
        ((TextView) findViewById(R.id.balance_text_view)).setText(mTransitData.getBalanceString());

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

        if (mTransitData.getRefills() == null) {
            findViewById(R.id.recent_trips_header).setVisibility(View.VISIBLE);
            getTabHost().getTabWidget().setVisibility(View.GONE);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean speakBalanceEnabled   = prefs.getBoolean("pref_key_speak_balance", false);
        boolean speakBalanceRequested = getIntent().getBooleanExtra(SPEAK_BALANCE_EXTRA, false);
        if (speakBalanceEnabled && speakBalanceRequested) {
            mTTS = new TextToSpeech(this, mTTSInitListener);
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
