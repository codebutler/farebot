/*
 * UltralightCardRawDataFragment.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.AdvancedCardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.ultralight.UltralightCard;
import com.codebutler.farebot.card.ultralight.UltralightPage;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Serializer;

/**
 * Shows raw data of the Mifare Ultralight / Ultralight C
 */
public class UltralightCardRawDataFragment extends ExpandableListFragment {

    private UltralightCard mCard;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Serializer serializer = FareBotApplication.getInstance().getSerializer();
        mCard = (UltralightCard) Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        setListAdapter(new UltralightRawDataAdapter(getActivity(), mCard));
    }

    private static class UltralightRawDataAdapter extends BaseExpandableListAdapter {
        private Activity mActivity;
        private UltralightCard mCard;

        private UltralightRawDataAdapter(Activity mActivity, UltralightCard mCard) {
            this.mActivity = mActivity;
            this.mCard = mCard;
        }

        @Override
        public int getGroupCount() {
            return mCard.getPages().length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mCard.getPage(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mCard.getPage(groupPosition).getData();
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

            UltralightPage sector = (UltralightPage) getGroup(groupPosition);
            String sectorIndexString = Integer.toHexString(sector.getIndex());

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(mActivity.getString(R.string.page_title_format, sectorIndexString));

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mActivity.getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }

            byte[] block = (byte[]) getChild(groupPosition, childPosition);

            //((TextView) view.findViewById(android.R.id.text1)).setText(mActivity.getString(R.string.block_title_format, block.getIndex()));
            ((TextView) view.findViewById(android.R.id.text2)).setText(Utils.getHexString(block));

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }


}
