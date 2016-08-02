/*
 * AdvancedCardInfoActivity.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Chris Norden <thisiscnn@gmail.com>
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
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardHasManufacturingInfo;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.UnsupportedCardException;
import com.codebutler.farebot.fragment.CardHWDetailFragment;
import com.codebutler.farebot.ui.TabPagerAdapter;
import com.codebutler.farebot.util.Utils;
import com.crashlytics.android.Crashlytics;

public class AdvancedCardInfoActivity extends Activity {
    public static final String EXTRA_CARD = "com.codebutler.farebot.EXTRA_CARD";
    public static final String EXTRA_RAW_CARD = "com.codebutler.farebot.EXTRA_RAW_CARD";
    public static final String EXTRA_ERROR = "com.codebutler.farebot.EXTRA_ERROR";

    private TabPagerAdapter mTabsAdapter;
    private Card mCard;
    private Exception mError;
    private String mSerializedRawCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_card_info);

        findViewById(R.id.error_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportError();
            }
        });

        mCard = getIntent().getParcelableExtra(EXTRA_CARD);
        mSerializedRawCard = getIntent().getStringExtra(EXTRA_RAW_CARD);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mCard.getCardType().toString() + " " + mCard.getTagId().hex());

        if (mCard.getScannedAt().getTime() > 0) {
            String date = Utils.dateFormat(mCard.getScannedAt());
            String time = Utils.timeFormat(mCard.getScannedAt());
            actionBar.setSubtitle(Utils.localizeString(R.string.scanned_at_format, time, date));
        }

        if (getIntent().hasExtra(EXTRA_ERROR)) {
            mError = (Exception) getIntent().getSerializableExtra(EXTRA_ERROR);
            if (mError instanceof UnsupportedCardException) {
                findViewById(R.id.unknown_card).setVisibility(View.VISIBLE);
            } else if (mError instanceof UnauthorizedException) {
                findViewById(R.id.unauthorized_card).setVisibility(View.VISIBLE);
                findViewById(R.id.load_keys).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(AdvancedCardInfoActivity.this, AddKeyActivity.class));
                    }
                });
            } else {
                findViewById(R.id.error).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.error_text)).setText(Utils.getErrorMessage(mError));
            }
        }

        CardHasManufacturingInfo infoAnnotation = getSupermostClass(mCard.getClass())
                .getAnnotation(CardHasManufacturingInfo.class);
        if (infoAnnotation == null || infoAnnotation.value()) {
            mTabsAdapter.addTab(actionBar.newTab().setText(R.string.hw_detail), CardHWDetailFragment.class,
                    getIntent().getExtras());
        }

        CardRawDataFragmentClass annotation = getSupermostClass(mCard.getClass())
                .getAnnotation(CardRawDataFragmentClass.class);
        if (annotation != null) {
            Class rawDataFragmentClass = annotation.value();
            mTabsAdapter.addTab(actionBar.newTab().setText(R.string.data), rawDataFragmentClass,
                    getIntent().getExtras());
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card_advanced_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            switch (item.getItemId()) {
                case R.id.copy:
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mSerializedRawCard));
                    Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.share:
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, mSerializedRawCard);
                    startActivity(intent);
                    return true;
                case android.R.id.home:
                    finish();
                    return true;
            }
        } catch (Exception ex) {
            new AlertDialog.Builder(this)
                    .setMessage(ex.toString())
                    .show();
        }
        return false;
    }

    private void reportError() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Crashlytics.log(mSerializedRawCard);
                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                }
                Crashlytics.logException(mError);
                Toast.makeText(AdvancedCardInfoActivity.this, R.string.error_report_sent, Toast.LENGTH_SHORT).show();
            }
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.report_error_privacy_title)
                .setMessage(R.string.report_error_privacy_message)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @NonNull
    private Class<?> getSupermostClass(Class aClass) {
        while (aClass.getSuperclass() != Object.class) {
            aClass = aClass.getSuperclass();
        }
        return aClass;
    }
}
