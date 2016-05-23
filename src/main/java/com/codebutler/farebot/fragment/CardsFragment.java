/*
 * CardsFragment.java
 *
 * Copyright (C) 2012 Eric Butler
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

package com.codebutler.farebot.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.CardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.provider.CardDBHelper;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.util.ExportHelper;
import com.codebutler.farebot.util.Utils;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CardsFragment extends ListFragment {
    private static final int REQUEST_SELECT_FILE = 1;
    private static final String SD_EXPORT_PATH = Environment.getExternalStorageDirectory() + "/FareBot-Export.xml";

    private Map<String, TransitIdentity> mDataCache;

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
            return new CursorLoader(getActivity(), CardProvider.CONTENT_URI_CARD,
                CardDBHelper.PROJECTION,
                null,
                null,
                CardsTableColumns.SCANNED_AT + " DESC, " + CardsTableColumns._ID + " DESC");
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (getListAdapter() == null) {
                setListAdapter(new CardsAdapter());
                setListShown(true);
                setEmptyText(getString(R.string.no_scanned_cards));
            }

            ((CursorAdapter) getListAdapter()).swapCursor(cursor);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {}
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDataCache = new HashMap<>();
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        registerForContextMenu(getListView());

        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id);
        Intent intent = new Intent(getActivity(), CardInfoActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cards_menu, menu);
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.card_context_menu, menu);
    }

    @Override public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.delete_card) {
            long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
            Uri uri = ContentUris.withAppendedId(CardProvider.CONTENT_URI_CARD, id);
            getActivity().getContentResolver().delete(uri, null, null);
            return true;
        }
        return false;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
       try {
           if (item.getItemId() == R.id.import_clipboard) {
               @SuppressWarnings("deprecation")
               ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
               onCardsImported(ExportHelper.importCardsXml(getActivity(), clipboard.getText().toString()));
               return true;

           } else if (item.getItemId() == R.id.import_file) {
               Uri uri = Uri.fromFile(Environment.getExternalStorageDirectory());
               Intent i = new Intent(Intent.ACTION_GET_CONTENT);
               i.putExtra(Intent.EXTRA_STREAM, uri);
               i.setType("application/xml");
               startActivityForResult(Intent.createChooser(i, "Select File"), REQUEST_SELECT_FILE);
               return true;

           } else if (item.getItemId() == R.id.import_sd) {
               String xml = FileUtils.readFileToString(new File(SD_EXPORT_PATH));
               onCardsImported(ExportHelper.importCardsXml(getActivity(), xml));
               return true;

           } else if (item.getItemId() == R.id.copy_xml) {
               @SuppressWarnings("deprecation")
               ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
               clipboard.setText(ExportHelper.exportCardsXml(getActivity()));
               Toast.makeText(getActivity(), "Copied to clipboard.", Toast.LENGTH_SHORT).show();
               return true;

           } else if (item.getItemId() == R.id.share_xml) {
               Intent intent = new Intent(Intent.ACTION_SEND);
               intent.setType("text/plain");
               intent.putExtra(Intent.EXTRA_TEXT, ExportHelper.exportCardsXml(getActivity()));
               startActivity(intent);
               return true;

           } else if (item.getItemId() == R.id.save_xml) {
               String xml = ExportHelper.exportCardsXml(getActivity());
               File file = new File(SD_EXPORT_PATH);
               FileUtils.writeStringToFile(file, xml, "UTF-8");
               Toast.makeText(getActivity(), "Wrote FareBot-Export.xml to USB Storage.", Toast.LENGTH_SHORT).show();
               return true;
           }
       } catch (Exception ex) {
           Utils.showError(getActivity(), ex);
       }
       return false;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_FILE) {
                Uri uri = data.getData();
                String xml = org.apache.commons.io.FileUtils.readFileToString(new File(uri.getPath()));
                onCardsImported(ExportHelper.importCardsXml(getActivity(), xml));
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
    }

    private void onCardsImported(Uri[] uris) {
        ((CursorAdapter) ((ListView) getView().findViewById(android.R.id.list)).getAdapter()).notifyDataSetChanged();
        if (uris.length == 1) {
            Toast.makeText(getActivity(), "Card imported!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Intent.ACTION_VIEW, uris[0]));
        } else {
            Toast.makeText(getActivity(), "Cards Imported: " + uris.length, Toast.LENGTH_SHORT).show();
        }
    }

    private class CardsAdapter extends ResourceCursorAdapter {
       public CardsAdapter() {
           super(getActivity(), android.R.layout.simple_list_item_2, null, false);
       }

       @Override public void bindView(View view, Context context, Cursor cursor) {
           int type = cursor.getInt(cursor.getColumnIndex(CardsTableColumns.TYPE));
           String serial = cursor.getString(cursor.getColumnIndex(CardsTableColumns.TAG_SERIAL));
           Date scannedAt = new Date(cursor.getLong(cursor.getColumnIndex(CardsTableColumns.SCANNED_AT)));
           String nickname = cursor.getString(cursor.getColumnIndex(CardsTableColumns.NICKNAME));

           String cacheKey = serial + scannedAt.getTime();

           if (!mDataCache.containsKey(cacheKey)) {
               String data = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));
               try {
                   Serializer serializer = FareBotApplication.getInstance().getSerializer();
                   mDataCache.put(cacheKey, Card.fromXml(serializer, data).parseTransitIdentity());
               } catch (Exception ex) {
                   String error = String.format("Error: %s", Utils.getErrorMessage(ex));
                   mDataCache.put(cacheKey, new TransitIdentity(error, null));
               }
           }

           TransitIdentity identity = mDataCache.get(cacheKey);

           TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
           TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

           if (identity != null) {
               if (nickname != null){
                   textView1.setText(String.format("%s: %s", identity.getName(), nickname));
               } else if (identity.getSerialNumber() != null) {
                   textView1.setText(String.format("%s: %s", identity.getName(), identity.getSerialNumber()));
               } else {
                   // textView1.setText(identity.getName());
                   textView1.setText(String.format("%s: %s", identity.getName(), serial));
               }
               DateFormat timeInstance = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
               DateFormat dateInstance = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
               textView2.setText(getString(R.string.scanned_at_format, timeInstance.format(scannedAt),
                       dateInstance.format(scannedAt)));
           } else {
               textView1.setText(getString(R.string.unknown_card));
               textView2.setText(String.format("%s - %s", CardType.values()[type].toString(), serial));
           }
       }

       @Override protected void onContentChanged() {
           super.onContentChanged();
           mDataCache.clear();
       }
    }
}
