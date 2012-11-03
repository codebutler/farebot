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

package com.codebutler.farebot.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.actionbarsherlock.app.SherlockActivity;
import com.codebutler.farebot.R;
import com.codebutler.farebot.UnsupportedTagException;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;

public class ReadingTagActivity extends SherlockActivity {
    @Override
    public void onCreate (Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_reading_tag);

        resolveIntent(getIntent());
    }

    @Override
    public void onNewIntent (Intent intent) {
        resolveIntent(intent);
    }

    private void resolveIntent (Intent intent) {
        try {
            final Tag tag      = (Tag) intent.getParcelableExtra("android.nfc.extra.TAG");
            final byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            
            new AsyncTask<Void, String, Card>() {
                Exception mException;
                
                @Override
                protected Card doInBackground (Void... params) {
                    try {
                        return Card.dumpTag(tagId, tag);
                    } catch (Exception ex) {
                        mException = ex;
                        return null;
                    }
                }

                @Override
                protected void onPostExecute (Card card) {
                    if (mException != null) {
                        if (mException instanceof UnsupportedTagException) {
                            UnsupportedTagException ex = (UnsupportedTagException) mException;
                            new AlertDialog.Builder(ReadingTagActivity.this)
                                .setTitle("Unsupported Tag")
                                .setMessage(ex.getMessage())
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick (DialogInterface arg0, int arg1) {
                                        finish();
                                    }
                                })
                                .show();
                        } else {
                            Utils.showErrorAndFinish(ReadingTagActivity.this, mException);
                        }
                        return;
                    }

                    try {
                        String cardXml = Utils.xmlNodeToString(card.toXML().getOwnerDocument());

                        Log.d("ReadingTagActivity", "Got Card XML");
                        for (String line : cardXml.split("\n")) {
                            Log.d("ReadingTagActivity", "Got Card XML: " + line);
                        }

                        ContentValues values = new ContentValues();
                        values.put(CardsTableColumns.TYPE, card.getCardType().toInteger());
                        values.put(CardsTableColumns.TAG_SERIAL, Utils.getHexString(card.getTagId()));
                        values.put(CardsTableColumns.DATA, cardXml);
                        values.put(CardsTableColumns.SCANNED_AT, card.getScannedAt().getTime());

                        Uri uri = getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);

                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(CardInfoActivity.SPEAK_BALANCE_EXTRA, true);
                        startActivity(intent);
                        finish();
                    } catch (Exception ex) {
                        Utils.showErrorAndFinish(ReadingTagActivity.this, ex);
                    }
                }
                
            }.execute();
            
        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
        }
    }
}
