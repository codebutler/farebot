/*
 * CardInfoActivity.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.core.Constants;
import com.codebutler.farebot.core.UnsupportedCardException;
import com.codebutler.farebot.card.serialize.CardSerializer;
import com.codebutler.farebot.fragment.CardBalanceFragment;
import com.codebutler.farebot.fragment.CardInfoFragment;
import com.codebutler.farebot.fragment.CardSubscriptionsFragment;
import com.codebutler.farebot.fragment.CardTripsFragment;
import com.codebutler.farebot.fragment.UnauthorizedCardFragment;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.TransitFactoryRegistry;
import com.codebutler.farebot.transit.stub.UnauthorizedClassicTransitInfo;
import com.codebutler.farebot.ui.TabPagerAdapter;
import com.codebutler.farebot.util.Utils;

public class CardInfoActivity extends Activity {

    public static final String EXTRA_TRANSIT_INFO = "transit_info";

    static final String SPEAK_BALANCE_EXTRA = "com.codebutler.farebot.speak_balance";

    private static final String KEY_SELECTED_TAB = "selected_tab";

    private RawCard mRawCard;
    private Card mCard;
    private TransitInfo mTransitInfo;
    private TabPagerAdapter mTabsAdapter;
    private TextToSpeech mTTS;
    private CardPersister mCardPersister;
    private CardSerializer mCardSerializer;
    private TransitFactoryRegistry mTransitFactoryRegistry;

    private OnInitListener mTTSInitListener = new OnInitListener() {
        @Override
        public void onInit(int status) {
            String balance = mTransitInfo.getBalanceString(getResources());
            if (status == TextToSpeech.SUCCESS && balance != null) {
                mTTS.speak(getString(R.string.balance_speech,
                        mTransitInfo.getBalanceString(getResources())),
                        TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FareBotApplication app = (FareBotApplication) getApplication();
        mCardPersister = app.getCardPersister();
        mCardSerializer = app.getCardSerializer();
        mTransitFactoryRegistry = app.getTransitFactoryRegistry();

        setContentView(R.layout.activity_card_info);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.loading);
        }

        new AsyncTask<Void, Void, Void>() {

            private boolean mSpeakBalanceEnabled;

            private Exception mException;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Uri uri = getIntent().getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    startManagingCursor(cursor);
                    cursor.moveToFirst();

                    mRawCard = mCardPersister.readCard(cursor);
                    mCard = mRawCard.parse();
                    mTransitInfo = mTransitFactoryRegistry.parseTransitInfo(mCard);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CardInfoActivity.this);
                    mSpeakBalanceEnabled = prefs.getBoolean("pref_key_speak_balance", false);
                } catch (Exception ex) {
                    Log.e("CardInfoActivity", "Failed to read", ex);
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

                if (mTransitInfo == null) {
                    showAdvancedInfo(new UnsupportedCardException());
                    finish();
                    return;
                }

                String titleSerial = (mTransitInfo.getSerialNumber() != null)
                        ? mTransitInfo.getSerialNumber()
                        : mCard.getTagId().hex();
                actionBar.setTitle(mTransitInfo.getCardName(getResources()) + " " + titleSerial);

                Bundle args = new Bundle();
                args.putParcelable(Constants.EXTRA_CARD, mCard);
                args.putParcelable(EXTRA_TRANSIT_INFO, mTransitInfo);

                if (mTransitInfo instanceof UnauthorizedClassicTransitInfo) {
                    mTabsAdapter.addTab(actionBar.newTab(), UnauthorizedCardFragment.class, args);
                    return;
                }

                if (mTransitInfo.getBalanceString(getResources()) != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.balance), CardBalanceFragment.class, args);
                }

                if (mTransitInfo.getTrips() != null || mTransitInfo.getRefills() != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.history), CardTripsFragment.class, args);
                }

                if (mTransitInfo.getSubscriptions() != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.subscriptions),
                            CardSubscriptionsFragment.class,
                            args);
                }

                if (mTransitInfo.getInfo(getApplicationContext()) != null) {
                    mTabsAdapter.addTab(actionBar.newTab().setText(R.string.info), CardInfoFragment.class, args);
                }

                if (mTabsAdapter.getCount() > 1) {
                    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                }

                if (mTransitInfo.hasUnknownStations()) {
                    findViewById(R.id.need_stations).setVisibility(View.VISIBLE);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    private void showAdvancedInfo(@Nullable Exception ex) {
        Intent intent = new Intent(this, AdvancedCardInfoActivity.class);
        intent.putExtra(Constants.EXTRA_CARD, mCard);
        intent.putExtra(Constants.EXTRA_RAW_CARD, mCardSerializer.serialize(mRawCard));
        if (ex != null) {
            intent.putExtra(Constants.EXTRA_ERROR, ex);
        }
        startActivity(intent);
    }
}
