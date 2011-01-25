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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.nfc.INfcTag;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.nfc.NfcInternal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReadingTagActivity extends Activity
{
    @Override
    public void onCreate (Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.activity_reading_tag);

        resolveIntent(getIntent());
    }

    @Override
    public void onNewIntent (Intent intent)
    {
        resolveIntent(intent);
    }

    private void resolveIntent (Intent intent)
    {
        final TextView textView = (TextView) findViewById(R.id.textView);
        
        try {
            Bundle extras = intent.getExtras();
            
            final byte[] id = extras.getByteArray(NfcAdapter.EXTRA_ID);
            Log.d("MainActivity", "Found tag with id: " + Utils.getHexString(id));
            
            final Object tagObject = extras.getParcelable("android.nfc.extra.TAG");

            new AsyncTask<Object, String, MifareCard>() {
                Exception mException;
                
                @Override
                protected MifareCard doInBackground (Object... params) {
                    try {
                        Object tagObject = params[0];
                        String cardType = NfcInternal.getCardType(tagObject);

                        if (cardType.equals("Iso14443-4"))
                            return DesfireCard.dumpTag(id, tagObject);
                        else
                            throw new Exception("Unsupported card type: " + cardType);
                    } catch (Exception ex) {
                        mException = ex;
                        return null;
                    }
                }

                @Override
                protected void onPostExecute (MifareCard card) {
                    if (mException != null) {
                        Utils.showErrorAndFinish(ReadingTagActivity.this, mException);
                        return;
                    }

                    try {
                        String cardXml = Utils.xmlDocumentToString(card.toXML().getOwnerDocument());

                        ContentValues values = new ContentValues();
                        values.put(CardsTableColumns.TYPE, card.getCardType().toInteger());
                        values.put(CardsTableColumns.TAG_SERIAL, Utils.getHexString(card.getTagId()));
                        values.put(CardsTableColumns.DATA, cardXml);

                        Uri uri = getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        finish();
                    } catch (Exception ex) {
                        Utils.showErrorAndFinish(ReadingTagActivity.this, ex);
                    }
                }

                @Override
                protected void onProgressUpdate(String... values) {
                    textView.setText(values[0]);
                }
                
            }.execute(tagObject);
            
        } catch (Exception ex) {
            Utils.showErrorAndFinish(this, ex);
        }
    }
}
