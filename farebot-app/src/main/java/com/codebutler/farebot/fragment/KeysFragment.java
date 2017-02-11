/*
 * KeysFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.AddKeyActivity;
import com.codebutler.farebot.persist.CardKeysPersister;
import com.codebutler.farebot.persist.model.SavedKey;
import com.codebutler.farebot.util.BetterAsyncTask;

import java.util.List;

public class KeysFragment extends ListFragment implements AdapterView.OnItemLongClickListener {

    private CardKeysPersister mCardKeysPersister;

    private final LoaderManager.LoaderCallbacks<List<SavedKey>> mLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<SavedKey>>() {
        @Override
        public Loader<List<SavedKey>> onCreateLoader(int i, Bundle bundle) {
            return new AsyncTaskLoader<List<SavedKey>>(getActivity()) {
                @Override
                public List<SavedKey> loadInBackground() {
                    return mCardKeysPersister.getSavedKeys();
                }

                @Override
                protected void onStartLoading() {
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<SavedKey>> loader, List<SavedKey> savedKeys) {
            setListAdapter(new KeysAdapter(savedKeys));
            setListShown(true);
        }

        @Override
        public void onLoaderReset(Loader<List<SavedKey>> loader) { }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mCardKeysPersister = ((FareBotApplication) getActivity().getApplication()).getCardKeysPersister();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getString(R.string.no_keys));
        getListView().setOnItemLongClickListener(this);
        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        SavedKey savedKey = (SavedKey) getListView().getItemAtPosition(position);
        getActivity().startActionMode(new KeysActionModeCallback(savedKey));
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_keys_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_key) {
            startActivity(new Intent(getActivity(), AddKeyActivity.class));
            return true;
        }
        return false;
    }

    private class KeysAdapter extends ArrayAdapter<SavedKey> {

        KeysAdapter(@NonNull List<SavedKey> savedKeys) {
            super(getActivity(), android.R.layout.simple_list_item_2, savedKeys);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = super.getView(position, convertView, parent);
            }

            SavedKey savedKey = getItem(position);

            TextView textView1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) convertView.findViewById(android.R.id.text2);

            textView1.setText(savedKey.card_id());
            textView2.setText(savedKey.card_type().toString());

            return convertView;
        }
    }

    private class KeysActionModeCallback implements ActionMode.Callback {

        @NonNull private final SavedKey mSavedKey;

        private KeysActionModeCallback(@NonNull SavedKey savedKey) {
            mSavedKey = savedKey;
        }

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
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.delete_key) {
                new BetterAsyncTask<Void>(getActivity(), false, false) {
                    @Override
                    protected Void doInBackground() throws Exception {
                        mCardKeysPersister.delete(mSavedKey);
                        return null;
                    }

                    @Override
                    protected void onResult(Void unused) {
                        mode.finish();
                        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
                    }
                }.execute();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) { }
    }
}
