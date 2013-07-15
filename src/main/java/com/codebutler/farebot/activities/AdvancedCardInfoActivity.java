/*
 * AdvancedCardInfoActivity.java
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.CardHasManufacturingInfo;
import com.codebutler.farebot.CardRawDataFragmentClass;
import com.codebutler.farebot.R;
import com.codebutler.farebot.TabPagerAdapter;
import com.codebutler.farebot.UnauthorizedException;
import com.codebutler.farebot.UnsupportedCardException;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.fragments.CardHWDetailFragment;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdvancedCardInfoActivity extends SherlockFragmentActivity {
    public static String EXTRA_CARD  = "com.codebutler.farebot.EXTRA_CARD";
    public static String EXTRA_ERROR = "com.codebutler.farebot.EXTRA_ERROR";

    private TabPagerAdapter mTabsAdapter;
    private Card mCard;
    private Exception mError;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_card_info);

        findViewById(R.id.error_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportError();
            }
        });
        
        mCard = (Card) getIntent().getParcelableExtra(AdvancedCardInfoActivity.EXTRA_CARD);
        
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);
        
        Bundle args = new Bundle();
        args.putParcelable(AdvancedCardInfoActivity.EXTRA_CARD, mCard);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mCard.getCardType().toString() + " " + Utils.getHexString(mCard.getTagId(), "<error>"));

        if (mCard.getScannedAt().getTime() > 0) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(mCard.getScannedAt());
            String time = new SimpleDateFormat("h:m a", Locale.US).format(mCard.getScannedAt());
            actionBar.setSubtitle(String.format("Scanned on %s at %s.", date, time));
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
                        new AlertDialog.Builder(AdvancedCardInfoActivity.this)
                            .setMessage(R.string.add_key_directions)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    }
                });
            } else {
                findViewById(R.id.error).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.error_text)).setText(Utils.getErrorMessage(mError));
            }
        }

        CardHasManufacturingInfo infoAnnotation = mCard.getClass().getAnnotation(CardHasManufacturingInfo.class);
        if (infoAnnotation == null || infoAnnotation.value()) {
            mTabsAdapter.addTab(actionBar.newTab().setText(R.string.hw_detail), CardHWDetailFragment.class, args);
        }

        CardRawDataFragmentClass annotation = mCard.getClass().getAnnotation(CardRawDataFragmentClass.class);
        if (annotation != null) {
            Class rawDataFragmentClass = annotation.value();
            if (rawDataFragmentClass != null) {
                mTabsAdapter.addTab(actionBar.newTab().setText(R.string.data), rawDataFragmentClass, args);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getSupportMenuInflater().inflate(R.menu.card_advanced_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        try {
            String xml = Utils.xmlNodeToString(mCard.toXML().getOwnerDocument());
            if (item.getItemId() == R.id.copy_xml) {
                @SuppressWarnings("deprecation")
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(xml);
                Toast.makeText(this, "Copied to clipboard.", 5).show();
                return true;

            } else if (item.getItemId() == R.id.share_xml) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, xml);
                startActivity(intent);
                return true;

            } else if (item.getItemId() == android.R.id.home) {
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
                StringBuilder builder = new StringBuilder();
                builder.append(Utils.getDeviceInfoString());
                builder.append("\n\n");

                builder.append(mError.toString());
                builder.append("\n");
                builder.append(Utils.getErrorMessage(mError));
                builder.append("\n");
                for (StackTraceElement elem : mError.getStackTrace()) {
                    builder.append(elem.toString());
                    builder.append("\n");
                }

                builder.append("\n\n");

                try {
                    builder.append(Utils.xmlNodeToString(mCard.toXML().getOwnerDocument()));
                } catch (Exception ex) {
                    builder.append("Failed to generate XML: ");
                    builder.append(ex);
                }

                builder.append("\n\n");
                builder.append(getString(R.string.comments));
                builder.append(":\n\n");

                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:eric+farebot@codebutler.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "FareBot Bug Report");
                intent.putExtra(Intent.EXTRA_TEXT, builder.toString());
                startActivity(intent);

            }
        };
        new AlertDialog.Builder(this)
            .setTitle(R.string.report_error_privacy_title)
            .setMessage(R.string.report_error_privacy_message)
            .setPositiveButton(android.R.string.ok, listener)
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
}
