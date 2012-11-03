/*
 * CardKeysFragment.java
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

package com.codebutler.farebot.fragments;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.codebutler.farebot.BetterAsyncTask;
import com.codebutler.farebot.R;
import com.codebutler.farebot.provider.CardKeyProvider;
import com.codebutler.farebot.provider.KeysTableColumns;

public class KeysFragment extends SherlockListFragment implements AdapterView.OnItemLongClickListener {
    private ActionMode mActionMode;
    private int mActionKeyId;

    private com.actionbarsherlock.view.ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.keys_contextual, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete_key) {
                new BetterAsyncTask<Void>(getActivity(), false, false) {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Uri uri = ContentUris.withAppendedId(CardKeyProvider.CONTENT_URI, mActionKeyId);
                        getActivity().getContentResolver().delete(uri, null, null);
                        return null;
                    }

                    @Override
                    protected void onResult(Void unused) {
                        mActionMode.finish();
                        ((KeysAdapter) getListAdapter()).notifyDataSetChanged();
                    }
                }.execute();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionKeyId = 0;
            mActionMode  = null;
        }
    };

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<android.database.Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getActivity(), CardKeyProvider.CONTENT_URI,
                null,
                null,
                null,
                KeysTableColumns.CREATED_AT + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            ((CursorAdapter) getListView().getAdapter()).swapCursor(cursor);
            setListShown(true);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getString(R.string.no_keys));
        getListView().setOnItemLongClickListener(this);
        setListAdapter(new KeysAdapter());
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) getListAdapter().getItem(position);

        mActionKeyId = cursor.getInt(cursor.getColumnIndex(KeysTableColumns._ID));
        mActionMode  = ((SherlockFragmentActivity)getActivity()).startActionMode(mActionModeCallback);

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_keys_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_key) {
            new AlertDialog.Builder(getActivity())
                .setMessage(R.string.add_key_directions)
                .setPositiveButton(android.R.string.ok, null)
                .show();
            return true;
        }
        return false;
    }

    private class KeysAdapter extends ResourceCursorAdapter {
        public KeysAdapter() {
            super(getActivity(), android.R.layout.simple_list_item_2, null, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String id   = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_ID));
            String type = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE));

            TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

            textView1.setText(id);
            textView2.setText(type);
        }
    }
}
