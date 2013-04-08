/*
 * ClassicCardRawDataFragment.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import com.codebutler.farebot.ExpandableListFragment;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.activities.AdvancedCardInfoActivity;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.InvalidClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;

public class ClassicCardRawDataFragment extends ExpandableListFragment {
    private ClassicCard mCard;

    public void onCreate (Bundle bundle) {
        super.onCreate(bundle);
        mCard = (ClassicCard) getArguments().getParcelable(AdvancedCardInfoActivity.EXTRA_CARD);
        setListAdapter(new ClassicRawDataAdapter(getActivity(), mCard));
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        ClassicSector sector = mCard.getSector(groupPosition);
        ClassicBlock  block  = sector.getBlock(childPosition);

        String data = Utils.getHexString(block.getData(), "");

        new AlertDialog.Builder(getActivity())
            .setTitle(String.format("%s, %s", getString(R.string.sector_title_format, sector.getIndex()), getString(R.string.block_title_format, block.getIndex())))
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data)
                .show();

        return true;
    }

    private static class ClassicRawDataAdapter extends BaseExpandableListAdapter {
        private Activity mActivity;
        private ClassicCard mCard;

        private ClassicRawDataAdapter(Activity mActivity, ClassicCard mCard) {
            this.mActivity = mActivity;
            this.mCard = mCard;
        }

        @Override
        public int getGroupCount() {
            return mCard.getSectors().length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            ClassicSector sector = mCard.getSector(groupPosition);
            if (!(sector instanceof UnauthorizedClassicSector)) {
                ClassicBlock[] blocks = sector.getBlocks();
                return (blocks == null) ? 0 : blocks.length;
            } else {
                return 0;
            }
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mCard.getSector(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mCard.getSector(groupPosition).getBlocks()[childPosition];
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
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }

            ClassicSector sector = (ClassicSector) getGroup(groupPosition);
            String sectorIndexString = Integer.toHexString(sector.getIndex());

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            if (sector instanceof UnauthorizedClassicSector) {
                textView.setText(mActivity.getString(R.string.unauthorized_sector_title_format, sectorIndexString));
            } else if (sector instanceof InvalidClassicSector) {
                textView.setText(mActivity.getString(R.string.invalid_sector_title_format, sectorIndexString, ((InvalidClassicSector) sector).getError()));
            } else {
                textView.setText(mActivity.getString(R.string.sector_title_format, sectorIndexString));
            }

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }

            ClassicBlock block = (ClassicBlock) getChild(groupPosition, childPosition);

            ((TextView) view.findViewById(android.R.id.text1)).setText(mActivity.getString(R.string.block_title_format, block.getIndex()));
            ((TextView) view.findViewById(android.R.id.text2)).setText(block.getType());

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
