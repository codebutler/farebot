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
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.Card;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdvancedCardInfoActivity extends TabActivity
{
    public static String EXTRA_CARD    = "com.codebutler.farebot.EXTRA_CARD";
    public static String EXTRA_MESSAGE = "com.codebutler.farebot.EXTRA_MESSAGE";

    private Card mCard;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_card_info);

        mCard = (Card) getIntent().getParcelableExtra(AdvancedCardInfoActivity.EXTRA_CARD);

        ((TextView) findViewById(R.id.card_type_text_view)).setText(mCard.getCardType().toString());
        ((TextView) findViewById(R.id.card_serial_text_view)).setText(Utils.getHexString(mCard.getTagId(), "<error>"));

        if (mCard.getScannedAt().getTime() > 0) {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(mCard.getScannedAt());
            String time = new SimpleDateFormat("h:m a", Locale.US).format(mCard.getScannedAt());
            ((TextView) findViewById(R.id.scanned_at)).setText(String.format("Scanned on %s at %s.", date, time));
        } else
            findViewById(R.id.scanned_at).setVisibility(View.GONE);

        if (getIntent().hasExtra(EXTRA_MESSAGE)) {
            String message = getIntent().getStringExtra(EXTRA_MESSAGE);
            ((TextView) findViewById(R.id.error_text_view)).setText(message);
            findViewById(R.id.error_text_view).setVisibility(View.VISIBLE);
        }

        TabHost tabHost = getTabHost();

        TabHost.TabSpec spec;
        Intent intent;

        intent = new Intent(this, CardHWDetailActivity.class);
        intent.putExtras(getIntent().getExtras());

        spec = tabHost.newTabSpec("hw_detail");
        spec.setIndicator(getString(R.string.hw_detail));
        spec.setContent(intent);
        tabHost.addTab(spec);

        Class rawDataActivityClass = null;

        if (mCard instanceof DesfireCard) {
            rawDataActivityClass = DesfireCardRawDataActivity.class;
        } else if (mCard instanceof FelicaCard) {
            rawDataActivityClass = FelicaCardRawDataActivity.class;
        }

        if (rawDataActivityClass != null) {
            intent = new Intent(this, rawDataActivityClass);
            intent.putExtras(getIntent().getExtras());
            spec.setIndicator(getString(R.string.data));
            spec.setContent(intent);
            tabHost.addTab(spec);
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
            }
        } catch (Exception ex) {
            new AlertDialog.Builder(this)
                .setMessage(ex.toString())
                .show();
        }
        return false;
    }
}
