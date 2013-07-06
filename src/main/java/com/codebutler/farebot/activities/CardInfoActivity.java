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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.R;
import com.codebutler.farebot.TabPagerAdapter;
import com.codebutler.farebot.UnsupportedCardException;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.fragments.CardBalanceFragment;
import com.codebutler.farebot.fragments.CardInfoFragment;
import com.codebutler.farebot.fragments.CardRefillsFragment;
import com.codebutler.farebot.fragments.CardSubscriptionsFragment;
import com.codebutler.farebot.fragments.CardTripsFragment;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.SuicaTransitData;
import com.codebutler.farebot.transit.EdyTransitData;
import com.codebutler.farebot.transit.TransitData;

public class CardInfoActivity extends SherlockFragmentActivity {
    public static final String EXTRA_TRANSIT_DATA = "transit_data";
    public static final String SPEAK_BALANCE_EXTRA = "com.codebutler.farebot.speak_balance";

    private static final String KEY_SELECTED_TAB = "selected_tab";

    private Card            mCard;
    private TransitData     mTransitData;
    private TabPagerAdapter mTabsAdapter;
    private TextToSpeech    mTTS;

    private OnInitListener mTTSInitListener = new OnInitListener() {
        public void onInit (int status) {
            if (status == TextToSpeech.SUCCESS) {
                mTTS.speak(getString(R.string.balance_speech, mTransitData.getBalanceString()), TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_card_info);
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.loading);
        
        new AsyncTask<Void, Void, Void>() {
            private Exception mException;
            public boolean mSpeakBalanceEnabled;
            
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Uri uri = getIntent().getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    startManagingCursor(cursor);
                    cursor.moveToFirst();

                    String data = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));

                    mCard        = Card.fromXml(data);
                    mTransitData = mCard.parseTransitData();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CardInfoActivity.this);
                    mSpeakBalanceEnabled = prefs.getBoolean("pref_key_speak_balance", false);
                } catch (Exception ex) {
                    mException = ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                findViewById(R.id.loading).setVisibility(View.GONE);
                findViewById(R.id.pager).setVisibility(View.VISIBLE);

                if (mException != null) {
                    if (mCard == null) {
                        Utils.showErrorAndFinish(CardInfoActivity.this, mException);
                    } else {
                        Log.e("CardInfoActivity", "Error parsing transit data", mException);
                        showAdvancedInfo(mException);
                        finish();
                    }
                    return;
                }

                if (mTransitData == null) {
                    showAdvancedInfo(new UnsupportedCardException());
                    finish();
                    return;
                }

                String titleSerial = (mTransitData.getSerialNumber() != null) ? mTransitData.getSerialNumber() : Utils.getHexString(mCard.getTagId(), "");
                actionBar.setTitle(mTransitData.getCardName() + " " + titleSerial);

                Bundle args = new Bundle();
                args.putParcelable(AdvancedCardInfoActivity.EXTRA_CARD, mCard);
                args.putParcelable(EXTRA_TRANSIT_DATA, mTransitData);
                
                mTabsAdapter.addTab(actionBar.newTab().setText(R.string.balance), CardBalanceFragment.class, args);

                if (mTransitData.getTrips() != null) {
                    int textId = (mTransitData instanceof SuicaTransitData) || (mTransitData instanceof EdyTransitData) ? R.string.history : R.string.trips;
                    mTabsAdapter.addTab(actionBar.newTab().setText(textId), CardTripsFragment.class, args);
                }

                if (mTransitData.getRefills() != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.refills), CardRefillsFragment.class, args);
                }

                if (mTransitData.getSubscriptions() != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.subscriptions), CardSubscriptionsFragment.class, args);
                }

                if (mTransitData.getInfo() != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.info), CardInfoFragment.class, args);
                }
                
                if (mTabsAdapter.getCount() > 1) {
                    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                }

                boolean speakBalanceRequested = getIntent().getBooleanExtra(SPEAK_BALANCE_EXTRA, false);
                if (mSpeakBalanceEnabled && speakBalanceRequested) {
                    mTTS = new TextToSpeech(CardInfoActivity.this, mTTSInitListener);
                }

                if (savedInstanceState != null) {
                    viewPager.setCurrentItem(savedInstanceState.getInt(KEY_SELECTED_TAB, 0));
                }
            }
        }.execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putInt(KEY_SELECTED_TAB, ((ViewPager) findViewById(R.id.pager)).getCurrentItem());
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getSupportMenuInflater().inflate(R.menu.card_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, CardsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.advanced_info) {
            showAdvancedInfo(null);
            return true;
        }
        return false;
    }

    private void showAdvancedInfo (Exception ex) {
        Intent intent = new Intent(this, AdvancedCardInfoActivity.class);
        intent.putExtra(AdvancedCardInfoActivity.EXTRA_CARD, mCard);
        if (ex != null) {
            intent.putExtra(AdvancedCardInfoActivity.EXTRA_ERROR, ex);
        }
        startActivity(intent);
    }
}
