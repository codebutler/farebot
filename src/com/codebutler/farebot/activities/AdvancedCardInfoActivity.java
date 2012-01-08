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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.codebutler.farebot.R;
import com.codebutler.farebot.TabPagerAdapter;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.fragments.CardHWDetailFragment;
import com.codebutler.farebot.fragments.DesfireCardRawDataFragment;
import com.codebutler.farebot.fragments.FelicaCardRawDataFragment;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.mifare.DesfireCard;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdvancedCardInfoActivity extends Activity
{
    public static String EXTRA_CARD    = "com.codebutler.farebot.EXTRA_CARD";
    public static String EXTRA_MESSAGE = "com.codebutler.farebot.EXTRA_MESSAGE";

    private TabPagerAdapter mTabsAdapter;
    private Card mCard;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_card_info);
        
        mCard = (Card) getIntent().getParcelableExtra(AdvancedCardInfoActivity.EXTRA_CARD);
        
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabPagerAdapter(this, viewPager);
        
        Bundle args = new Bundle();
        args.putParcelable(AdvancedCardInfoActivity.EXTRA_CARD, mCard);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mCard.getCardType().toString() + " " + Utils.getHexString(mCard.getTagId(), "<error>"));

        if (mCard.getScannedAt().getTime() > 0) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(mCard.getScannedAt());
            String time = new SimpleDateFormat("h:m a", Locale.US).format(mCard.getScannedAt());
            actionBar.setSubtitle(String.format("Scanned on %s at %s.", date, time));
        }

        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.hw_detail), CardHWDetailFragment.class, args);

        if (getIntent().hasExtra(EXTRA_MESSAGE)) {
            String message = getIntent().getStringExtra(EXTRA_MESSAGE);
            ((TextView) findViewById(R.id.error_text_view)).setText(message);
            findViewById(R.id.error_text_view).setVisibility(View.VISIBLE);
//            findViewById(R.id.pager).setVisibility(View.GONE);
        }

        Class rawDataFragmentClass = null;
        if (mCard instanceof DesfireCard) {
            rawDataFragmentClass = DesfireCardRawDataFragment.class;
        } else if (mCard instanceof FelicaCard) {
            rawDataFragmentClass = FelicaCardRawDataFragment.class;
        }

        if (rawDataFragmentClass != null) {
            mTabsAdapter.addTab(actionBar.newTab().setText(R.string.data), rawDataFragmentClass, args);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        getMenuInflater().inflate(R.menu.card_advanced_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        try {
            String xml = Utils.xmlNodeToString(mCard.toXML().getOwnerDocument());
            if (item.getItemId() == R.id.copy_xml) {
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
}
