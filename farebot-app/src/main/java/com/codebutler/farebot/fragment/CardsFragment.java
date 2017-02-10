/*
 * CardsFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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
import android.content.AsyncTaskLoader;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.TransitFactoryRegistry;
import com.codebutler.farebot.activity.CardInfoActivity;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.serialize.CardSerializer;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.persist.model.SavedCard;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.util.ExportHelper;
import com.codebutler.farebot.util.Utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardsFragment extends ListFragment {

    private static final int REQUEST_SELECT_FILE = 1;
    private static final String FILENAME = "farebot-export.json";

    private final LoaderManager.LoaderCallbacks<List<SavedCard>> mLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<List<SavedCard>>() {
        @Override
        public Loader<List<SavedCard>> onCreateLoader(int id, Bundle bundle) {
            return new AsyncTaskLoader<List<SavedCard>>(getActivity()) {
                @Override
                public List<SavedCard> loadInBackground() {
                    return mCardPersister.getCards();
                }

                @Override
                protected void onStartLoading() {
                    forceLoad();
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<SavedCard>> loader, List<SavedCard> list) {
            setListAdapter(new CardsAdapter(list));
            setListShown(true);
            setEmptyText(getString(R.string.no_scanned_cards));
        }

        @Override
        public void onLoaderReset(Loader<List<SavedCard>> cursorLoader) { }
    };

    private ExportHelper mExportHelper;
    private CardPersister mCardPersister;
    private CardSerializer mCardSerializer;
    private TransitFactoryRegistry mTransitFactoryRegistry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        FareBotApplication application = (FareBotApplication) getActivity().getApplication();
        mExportHelper = application.getExportHelper();
        mCardPersister = application.getCardPersister();
        mCardSerializer = application.getCardSerializer();
        mTransitFactoryRegistry = application.getTransitFactoryRegistry();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        registerForContextMenu(getListView());

        getLoaderManager().initLoader(0, null, mLoaderCallbacks);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        startActivity(CardInfoActivity.newIntent(getActivity(), id));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cards_menu, menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.card_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.delete_card) {
            int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            SavedCard savedCard = (SavedCard) getListView().getItemAtPosition(position);
            mCardPersister.deleteCard(savedCard);
            getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ClipboardManager clipboardManager
                = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            int itemId = item.getItemId();
            switch (itemId) {
                case R.id.import_file:
                    Uri uri = Uri.fromFile(Environment.getExternalStorageDirectory());
                    Intent target = new Intent(Intent.ACTION_GET_CONTENT);
                    target.putExtra(Intent.EXTRA_STREAM, uri);
                    target.setType("application/json");
                    startActivityForResult(Intent.createChooser(target, getString(R.string.select_file)),
                            REQUEST_SELECT_FILE);
                    return true;
                case R.id.import_clipboard:
                    ClipData clip = clipboardManager.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        String text = clip.getItemAt(0).coerceToText(getActivity()).toString();
                        onCardsImported(mExportHelper.importCards(text));
                    }
                    return true;
                case R.id.copy:
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mExportHelper.exportCards()));
                    Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.share:
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, mExportHelper.exportCards());
                    startActivity(intent);
                    return true;
                case R.id.save:
                    exportToFile();
                    return true;
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_FILE) {
                Uri uri = data.getData();
                String json = FileUtils.readFileToString(new File(uri.getPath()));
                onCardsImported(mExportHelper.importCards(json));
            }
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
    }

    private void onCardsImported(@NonNull List<Long> cardIds) {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);

        String text = getResources().getQuantityString(R.plurals.cards_imported, cardIds.size());
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();

        if (cardIds.size() == 1) {
            startActivity(CardInfoActivity.newIntent(getActivity(), cardIds.get(0)));
        }
    }

    private void exportToFile() {
        try {
            FileUtils.writeStringToFile(new File(FILENAME), mExportHelper.exportCards(), "UTF-8");
            Toast.makeText(getActivity(), getString(R.string.saved_to_x, FILENAME), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Utils.showError(getActivity(), ex);
        }
    }

    private class CardsAdapter extends ArrayAdapter<SavedCard> {

        private final Map<String, TransitIdentity> mDataCache = new HashMap<>();

        CardsAdapter(List<SavedCard> savedCards) {
            super(getActivity(), 0, savedCards);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            SavedCard savedCard = getItem(position);

            CardType type = savedCard.type();
            String serial = savedCard.serial();
            Date scannedAt = savedCard.scanned_at();

            String cacheKey = serial + scannedAt.getTime();

            if (!mDataCache.containsKey(cacheKey)) {
                String data = savedCard.data();
                try {
                    RawCard rawCard = mCardSerializer.deserialize(data);
                    TransitIdentity transitIdentity = mTransitFactoryRegistry.parseTransitIdentity(rawCard.parse());
                    mDataCache.put(cacheKey, transitIdentity);
                } catch (Exception ex) {
                    Log.e("CardsFragment", "Parse error", ex);
                    String error = String.format("Error: %s", Utils.getErrorMessage(ex));
                    mDataCache.put(cacheKey, TransitIdentity.create(error, null));
                }
            }

            TextView textView1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) convertView.findViewById(android.R.id.text2);

            TransitIdentity identity = mDataCache.get(cacheKey);
            if (identity != null) {
                if (identity.getSerialNumber() != null) {
                    textView1.setText(String.format("%s: %s", identity.getName(), identity.getSerialNumber()));
                } else {
                    textView1.setText(String.format("%s: %s", identity.getName(), serial));
                }
                DateFormat timeInstance = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
                DateFormat dateInstance = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
                textView2.setText(getString(R.string.scanned_at_format, timeInstance.format(scannedAt),
                        dateInstance.format(scannedAt)));
            } else {
                textView1.setText(getString(R.string.unknown_card));
                textView2.setText(String.format("%s - %s", type.toString(), serial));
            }

            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return getItem(position)._id();
        }
    }
}
