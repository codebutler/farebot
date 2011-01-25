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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.TransitData;

public class MainActivity extends ListActivity
{
    @Override
    protected void onCreate (Bundle bundle)
    {
        super.onCreate(bundle);

        Cursor cursor = getContentResolver().query(CardProvider.CONTENT_URI_CARD, new String[] { "type", "serial", "data" }, null, null, null);
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

                textView2.setText(String.format("%s — %s", MifareCard.CardType.values()[type].toString(), serial));
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
}
