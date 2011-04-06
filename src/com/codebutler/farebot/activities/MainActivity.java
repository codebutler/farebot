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

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.TransitData;

import java.io.File;

public class MainActivity extends ListActivity
{
    private static final int SELECT_FILE = 1;

    private static final String[] PROJECTION = new String[] {
        CardsTableColumns._ID,
        CardsTableColumns.TYPE,
        CardsTableColumns.TAG_SERIAL,
        CardsTableColumns.DATA 
    };

    @Override
    protected void onCreate (Bundle bundle)
    {
        super.onCreate(bundle);

        Cursor cursor = getContentResolver().query(CardProvider.CONTENT_URI_CARD,
            PROJECTION,
            null,
            null,
            CardsTableColumns._ID + " DESC");
        startManagingCursor(cursor);

        setListAdapter(new ResourceCursorAdapter(this, android.R.layout.simple_list_item_2, cursor) {
            @Override
            public void bindView (View view, Context context, Cursor cursor) {
                int    type   = cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE));
                String serial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL));
                String data   = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));

                TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
                TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

                try {
                    // This may end up being too slow.
                    MifareCard card = MifareCard.fromXml(data);
                    TransitData transitData = card.parseTransitData();
                    if (transitData != null) {
                        textView1.setText(String.format("%s: %s", transitData.getCardName(), transitData.getSerialNumber()));
                    } else {
                        textView1.setText("Unknown Card");
                    }
                } catch (Exception ex) {
                    textView1.setText("Error");
                }

                textView2.setText(String.format("%s - %s", MifareCard.CardType.values()[type].toString(), serial));
            }
        });

        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        getMenuInflater().inflate(R.menu.card_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected (MenuItem item)
    {
        if (item.getItemId() == R.id.delete_card) {
            long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
            Uri uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id);
            getContentResolver().delete(uri, null, null);
            return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id)
    {
        Uri uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        if (item.getItemId() == R.id.about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://codebutler.github.com/farebot")));
            return true;
        } else if (item.getItemId() == R.id.import_file) {
            Uri uri = Uri.fromFile(Environment.getExternalStorageDirectory());

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.setType("application/xml");
            startActivityForResult(Intent.createChooser(i, "Select File"), SELECT_FILE);
        } else if (item.getItemId() == R.id.import_clipboard) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            importXML(clipboard.getText().toString());
        }
        return false;
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        try {
            if (resultCode == RESULT_OK && requestCode == SELECT_FILE) {
                Uri uri = data.getData();
                String xml = org.apache.commons.io.FileUtils.readFileToString(new File(uri.getPath()));
                importXML(xml);
            }
        } catch (Exception ex) {
            Utils.showError(this, ex);
        }
    }

    private void importXML (String xml)
    {
        try {
            MifareCard card = MifareCard.fromXml(xml);

            ContentValues values = new ContentValues();
            values.put(CardsTableColumns.TYPE, card.getCardType().toInteger());
            values.put(CardsTableColumns.TAG_SERIAL, Utils.getHexString(card.getTagId()));
            values.put(CardsTableColumns.DATA, xml);

            Uri uri = getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);

            ((ResourceCursorAdapter) getListAdapter()).notifyDataSetChanged();

            Toast.makeText(this, "XML imported!", 5).show();

            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ex) {
            Utils.showError(this, ex);
        }
    }
}
