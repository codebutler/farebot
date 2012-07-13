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

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.R;

public class MainActivity extends SherlockFragmentActivity {
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private String[][] mTechLists;

    @Override
    protected void onCreate (Bundle bundle)
    {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(false);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        checkNfcEnabled();

        Intent intent = new Intent(this, ReadingTagActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        
        mTechLists = new String[][] {
            new String[] { IsoDep.class.getName() },
            new String[] { MifareClassic.class.getName() },
            new String[] { MifareUltralight.class.getName() },
            new String[] { NfcF.class.getName() }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, mTechLists);
    }

    public void onSupportedCardsClick(View view) {
        startActivity(new Intent(this, SupportedCardsActivity.class));
    }

    public void onHistoryClick(View view) {
        startActivity(new Intent(this, CardsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        getSupportMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        if (item.getItemId() == R.id.about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://codebutler.github.com/farebot")));
            return true;

        } else if (item.getItemId() == R.id.prefs) {
            startActivity(new Intent(this, FareBotPreferenceActivity.class));
        }
        return false;
    }

	private void checkNfcEnabled()
    {
		if (mNfcAdapter.isEnabled()) {
            return;
        }
        new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.nfc_off_error)
            .setMessage(R.string.turn_on_nfc)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            })
            .setNeutralButton(R.string.wireless_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
            })
            .show();
	}
}
