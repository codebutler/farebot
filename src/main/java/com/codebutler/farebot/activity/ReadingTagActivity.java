/*
 * ReadingTagActivity.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.TagReaderFactory;
import com.codebutler.farebot.card.UnsupportedTagException;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.util.Utils;

import java.util.Date;

public class ReadingTagActivity extends Activity {

    private CardPersister mCardPersister;
    private TagReaderFactory mTagReaderFactory;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_reading_tag);

        FareBotApplication app = (FareBotApplication) getApplication();
        mCardPersister = app.getCardPersister();
        mTagReaderFactory = app.getTagReaderFactory();

        resolveIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        try {
            final Tag tag = intent.getParcelableExtra("android.nfc.extra.TAG");
            final byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String lastReadId = prefs.getString(FareBotApplication.PREF_LAST_READ_ID, "");
            long lastReadAt = prefs.getLong(FareBotApplication.PREF_LAST_READ_AT, 0);

            // Prevent FareBot from reading the same card again right away.
            // This was especially a problem with FeliCa cards.
            if (Utils.getHexString(tagId).equals(lastReadId) && (new Date().getTime() - lastReadAt) < 5000) {
                finish();
                return;
            }

            new AsyncTask<Void, String, Uri>() {
                private Exception mException;

                @Override
                protected Uri doInBackground(Void... params) {
                    try {
                        TagReader tagReader = mTagReaderFactory.getTagReader(tagId, tag);

                        RawCard card = tagReader.readTag();

                        String tagIdString = card.tagId().hex();

                        SharedPreferences.Editor prefs
                                = PreferenceManager.getDefaultSharedPreferences(ReadingTagActivity.this).edit();
                        prefs.putString(FareBotApplication.PREF_LAST_READ_ID, tagIdString);
                        prefs.putLong(FareBotApplication.PREF_LAST_READ_AT, new Date().getTime());
                        prefs.apply();

                        return mCardPersister.saveCard(card);
                    } catch (Exception ex) {
                        mException = ex;
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Uri cardUri) {
                    if (mException == null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, cardUri);
                        intent.putExtra(CardInfoActivity.SPEAK_BALANCE_EXTRA, true);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    if (mException instanceof UnsupportedTagException) {
                        UnsupportedTagException ex = (UnsupportedTagException) mException;
                        new AlertDialog.Builder(ReadingTagActivity.this)
                                .setTitle("Unsupported Tag")
                                .setMessage(ex.getMessage())
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        finish();
                                    }
                                })
                                .show();
                    } else {
                        Utils.showErrorAndFinish(ReadingTagActivity.this, mException);
                    }
                }
            }.execute();

        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
        }
    }
}
