/*
 * ReadingTagActivity.java
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

package com.codebutler.farebot.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.codebutler.farebot.BuildConfig;
import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.UnsupportedTagException;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.util.Utils;

import java.util.Date;

public class ReadingTagActivity extends Activity {
    @Override public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_reading_tag);

        resolveIntent(getIntent());
    }

    @Override public void onNewIntent(Intent intent) {
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

                @Override protected Uri doInBackground(Void... params) {
                    try {
                        Card card = Card.dumpTag(tagId, tag);

                        String cardXml = card.toXml(FareBotApplication.getInstance().getSerializer());

                        if (BuildConfig.DEBUG) {
                            Log.d("ReadingTagActivity", "Got Card XML");
                            for (String line : cardXml.split("\n")) {
                                Log.d("ReadingTagActivity", "Got Card XML: " + line);
                            }
                        }
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ReadingTagActivity.this);
                        boolean overwriteLast = prefs.getBoolean("pref_overwrite_card_data", false);
                        boolean found = false;

                        String tagIdString = Utils.getHexString(card.getTagId());

                        ContentValues values = new ContentValues();
                        Uri oldUri = null;
                        Uri uri;
                        String selection = CardsTableColumns.TAG_SERIAL+"=?";
                        String[] selection_args = {tagIdString};
                        if(overwriteLast){
                            oldUri = CardProvider.CONTENT_URI_CARD;
                            Cursor cursor = getContentResolver().query(oldUri, null, selection, selection_args, null);
                            if(cursor.moveToFirst()) {
                                found = true;
                                values.put(CardsTableColumns.TYPE, cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE)));
                                values.put(CardsTableColumns.TAG_SERIAL, cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL)));
                                values.put(CardsTableColumns.DATA, cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA)));
                                values.put(CardsTableColumns.SCANNED_AT, cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT)));
                                oldUri = ContentUris.withAppendedId(oldUri, cursor.getLong(cursor.getColumnIndex(CardsTableColumns._ID)));
                            }
                            cursor.close();
                        }

                        values.put(CardsTableColumns.TYPE, card.getCardType().toInteger());
                        values.put(CardsTableColumns.TAG_SERIAL, tagIdString);
                        values.put(CardsTableColumns.DATA, cardXml);
                        values.put(CardsTableColumns.SCANNED_AT, card.getScannedAt().getTime());

                        if(overwriteLast && found){
                            getContentResolver().update(oldUri, values, null, null);
                            uri = oldUri;
                        } else {
                            uri = getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);
                        }

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(FareBotApplication.PREF_LAST_READ_ID, tagIdString);
                        editor.putLong(FareBotApplication.PREF_LAST_READ_AT, new Date().getTime());
                        editor.apply();

                        return uri;

                    } catch (Exception ex) {
                        mException = ex;
                        return null;
                    }
                }

                @Override protected void onPostExecute(Uri cardUri) {
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
