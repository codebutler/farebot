/*
 * MainActivity.java
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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;

public class MainActivity extends SherlockFragmentActivity {
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private String[][] mTechLists = new String[][] {
        new String[] { IsoDep.class.getName() },
        new String[] { MifareClassic.class.getName() },
        new String[] { MifareUltralight.class.getName() },
        new String[] { NfcF.class.getName() }
    };

    @Override
    protected void onCreate (Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(false);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            Utils.checkNfcEnabled(this, mNfcAdapter);

            Intent intent = new Intent(this, ReadingTagActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        } else {
            ((TextView) findViewById(R.id.directions)).setText(R.string.nfc_unavailable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, mTechLists);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    public void onSupportedCardsClick(View view) {
        startActivity(new Intent(this, SupportedCardsActivity.class));
    }

    public void onHistoryClick(View view) {
        startActivity(new Intent(this, CardsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        if (item.getItemId() == R.id.about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://codebutler.github.com/farebot")));
            return true;
        } else if (item.getItemId() == R.id.prefs) {
            startActivity(new Intent(this, FareBotPreferenceActivity.class));
        } else if (item.getItemId() == R.id.keys) {
            startActivity(new Intent(this, KeysActivity.class));
        }

        return false;
    }
}
