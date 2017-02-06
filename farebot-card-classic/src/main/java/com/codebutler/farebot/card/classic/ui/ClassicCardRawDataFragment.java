/*
 * ClassicCardRawDataFragment.java
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

package com.codebutler.farebot.card.classic.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.codebutler.farebot.card.CardUiDependencies;
import com.codebutler.farebot.card.classic.R;
import com.codebutler.farebot.card.classic.raw.RawClassicBlock;
import com.codebutler.farebot.card.classic.raw.RawClassicCard;
import com.codebutler.farebot.card.classic.raw.RawClassicSector;
import com.codebutler.farebot.card.serialize.CardSerializer;
import com.codebutler.farebot.core.Constants;
import com.codebutler.farebot.core.ui.ExpandableListFragment;

import java.util.List;

public class ClassicCardRawDataFragment extends ExpandableListFragment {

    private RawClassicCard mRawCard;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

         CardSerializer cardSerializer = ((CardUiDependencies) getActivity().getApplication()).getCardSerializer();
         String serializedCard = getArguments().getString(Constants.EXTRA_RAW_CARD);
         mRawCard = (RawClassicCard) cardSerializer.deserialize(serializedCard);
         setListAdapter(new ClassicRawDataAdapter(getActivity(), mRawCard));
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        RawClassicSector sector = mRawCard.sectors().get(groupPosition);
        List<RawClassicBlock> blocks = sector.blocks();
        if (blocks == null) {
            return false;
        }
        RawClassicBlock block = blocks.get(childPosition);

        String data = block.data().hex();

        String sectorTitle = getString(R.string.sector_title_format, String.valueOf(sector.index()));
        String blockTitle = getString(R.string.block_title_format, String.valueOf(block.index()));
        new AlertDialog.Builder(getActivity())
                .setTitle(String.format("%s, %s", sectorTitle, blockTitle))
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data)
                .show();

        return true;
    }

    private static class ClassicRawDataAdapter extends BaseExpandableListAdapter {
        private Activity mActivity;
        private RawClassicCard mRawCard;

        private ClassicRawDataAdapter(Activity activity, RawClassicCard rawClassicCard) {
            mActivity = activity;
            mRawCard = rawClassicCard;
        }

        @Override
        public int getGroupCount() {
            return mRawCard.sectors().size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            RawClassicSector sector = mRawCard.sectors().get(groupPosition);
            List<RawClassicBlock> blocks = sector.blocks();
            return blocks != null ? blocks.size() : 0;
        }

        @Override
        public RawClassicSector getGroup(int groupPosition) {
            return mRawCard.sectors().get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mRawCard.sectors().get(groupPosition).blocks().get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return groupPosition + childPosition + 100000;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater()
                        .inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }

            RawClassicSector sector = getGroup(groupPosition);
            String sectorIndexString = Integer.toHexString(sector.index());

            TextView textView = (TextView) view.findViewById(android.R.id.text1);

            switch (sector.type()) {
                case RawClassicSector.TYPE_UNAUTHORIZED:
                    textView.setText(mActivity.getString(R.string.unauthorized_sector_title_format, sectorIndexString));
                    break;
                case RawClassicSector.TYPE_INVALID:
                    textView.setText(mActivity.getString(R.string.invalid_sector_title_format, sectorIndexString,
                            sector.errorMessage()));
                    break;
                default:
                    textView.setText(mActivity.getString(R.string.sector_title_format, sectorIndexString));
                    break;
            }

            return view;
        }

        @Override
        public View getChildView(
                int groupPosition,
                int childPosition,
                boolean isLastChild,
                View convertView,
                ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater()
                        .inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }

            RawClassicBlock block = (RawClassicBlock) getChild(groupPosition, childPosition);

            ((TextView) view.findViewById(android.R.id.text1))
                    .setText(mActivity.getString(R.string.block_title_format, String.valueOf(block.index())));
            ((TextView) view.findViewById(android.R.id.text2)).setText(block.type());

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
