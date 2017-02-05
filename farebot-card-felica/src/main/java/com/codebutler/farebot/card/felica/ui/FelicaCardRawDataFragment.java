/*
 * FelicaCardRawDataFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.felica.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardUiDependencies;
import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.card.felica.FelicaSystem;
import com.codebutler.farebot.card.felica.FelicaUtils;
import com.codebutler.farebot.card.felica.R;
import com.codebutler.farebot.card.felica.raw.RawFelicaCard;
import com.codebutler.farebot.card.serialize.CardSerializer;
import com.codebutler.farebot.core.Constants;
import com.codebutler.farebot.core.ui.ExpandableListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

@CardRawDataFragmentClass(FelicaCardRawDataFragment.class)
public class FelicaCardRawDataFragment extends ExpandableListFragment {

    private RawFelicaCard mRawCard;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        CardSerializer cardSerializer = ((CardUiDependencies) getActivity().getApplication()).getCardSerializer();
        String serializedCard = getArguments().getString(Constants.EXTRA_RAW_CARD);
        mRawCard = (RawFelicaCard) cardSerializer.deserialize(serializedCard);

        setListAdapter(new FelicaRawDataAdapter(getActivity(), mRawCard));
    }

    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_raw_data, null);
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        FelicaService service = (FelicaService) getExpandableListAdapter().getChild(groupPosition, childPosition);

        List<String> items = new ArrayList<>();
        for (FelicaBlock block : service.getBlocks()) {
            items.add(String.format(Locale.US, "%02d: %s", block.getAddress(), block.getData().hex()));
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.monospace_list_item, items);
        new AlertDialog.Builder(getActivity())
                .setTitle(String.format("Service 0x%s", Integer.toHexString(service.getServiceCode())))
                .setPositiveButton(android.R.string.ok, null)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        @SuppressWarnings("deprecation")
                        ClipboardManager clipboard
                                = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                        clipboard.setText(adapter.getItem(position));
                        Toast.makeText(getActivity(), "Copied!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();

        return true;
    }

    private static class FelicaRawDataAdapter extends BaseExpandableListAdapter {
        private Activity mActivity;
        private RawFelicaCard mRawCard;

        private FelicaRawDataAdapter(Activity activity, RawFelicaCard rawCard) {
            mActivity = activity;
            mRawCard = rawCard;
        }

        @Override
        public int getGroupCount() {
            return mRawCard.systems().size();
        }

        @Override
        public FelicaSystem getGroup(int groupPosition) {
            return mRawCard.systems().get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return mRawCard.systems().get(groupPosition).getCode();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mRawCard.systems().get(groupPosition).getServices().size();
        }

        @Override
        public FelicaService getChild(int groupPosition, int childPosition) {
            return mRawCard.systems().get(groupPosition).getServices().get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return mRawCard.systems().get(groupPosition).getServices().get(childPosition).getServiceCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_1, null);
                view.setLayoutParams(new AbsListView.LayoutParams(MATCH_PARENT, 80));
            }

            FelicaSystem system = mRawCard.systems().get(groupPosition);

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(String.format("System: 0x%s (%s)", Integer.toHexString(system.getCode()),
                    FelicaUtils.getFriendlySystemName(system.getCode())));

            return view;
        }

        @Override
        public View getChildView(
                int groupPosition,
                int childPosition,
                boolean isExpanded,
                View convertView,
                ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, null);
                view.setLayoutParams(new AbsListView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            }

            TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

            FelicaSystem system = mRawCard.systems().get(groupPosition);
            FelicaService service = system.getServices().get(childPosition);

            textView1.setText(String.format("Service: 0x%s (%s)", Integer.toHexString(service.getServiceCode()),
                    FelicaUtils.getFriendlyServiceName(system.getCode(), service.getServiceCode())));
            textView2.setText(mActivity.getResources().getQuantityString(
                    R.plurals.block_count,
                    service.getBlocks().size(),
                    service.getBlocks().size()));

            return view;
        }
    }
}
