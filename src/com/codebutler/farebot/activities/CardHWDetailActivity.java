/*
 * CardHWDetailActivity.java
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
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.codebutler.farebot.R;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.DesfireManufacturingData;

import java.util.ArrayList;
import java.util.List;

public class CardHWDetailActivity extends ListActivity
{
    private DesfireCard mCard;

    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_hw_detail);

        mCard = (DesfireCard) getIntent().getParcelableExtra(AdvancedCardInfoActivity.EXTRA_CARD);

        List<ListItem> items = new ArrayList<ListItem>();

        DesfireManufacturingData data = mCard.getManufacturingData();
        items.add(new HeaderListItem("Hardware Information"));
        items.add(new ListItem("Vendor ID",     Integer.toString(data.hwVendorID)));
        items.add(new ListItem("Type",          Integer.toString(data.hwType)));
        items.add(new ListItem("Subtype",       Integer.toString(data.hwSubType)));
        items.add(new ListItem("Major Version", Integer.toString(data.hwMajorVersion)));
        items.add(new ListItem("Minor Version", Integer.toString(data.hwMinorVersion)));
        items.add(new ListItem("Storage Size",  Integer.toString(data.hwStorageSize)));
        items.add(new ListItem("Protocol",      Integer.toString(data.hwProtocol)));

        items.add(new HeaderListItem("Software Information"));
        items.add(new ListItem("Vendor ID",     Integer.toString(data.swVendorID)));
        items.add(new ListItem("Type",          Integer.toString(data.swType)));
        items.add(new ListItem("Subtype",       Integer.toString(data.swSubType)));
        items.add(new ListItem("Major Version", Integer.toString(data.swMajorVersion)));
        items.add(new ListItem("Minor Version", Integer.toString(data.swMinorVersion)));
        items.add(new ListItem("Storage Size",  Integer.toString(data.swStorageSize)));
        items.add(new ListItem("Protocol",      Integer.toString(data.swProtocol)));

        items.add(new HeaderListItem("General Information"));
        items.add(new ListItem("Serial Number",      Integer.toString(data.uid)));
        items.add(new ListItem("Batch Number",       Integer.toString(data.batchNo)));
        items.add(new ListItem("Week of Production", Integer.toString(data.weekProd)));
        items.add(new ListItem("Year of Production", Integer.toString(data.yearProd)));

        setListAdapter(new HWDetailListAdapter(this, items));
    }

    private class HWDetailListAdapter extends ArrayAdapter<ListItem>
    {
        private HWDetailListAdapter (Context context, List<ListItem> items)
        {
            super(context, 0, items);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent)
        {
            ListItem item = (ListItem) getListAdapter().getItem(position);
            if (convertView != null) {
                Log.i("CardHWDetailActivity", "ID: " + convertView.getId());
            }
            if (item instanceof HeaderListItem) {
                if (convertView == null || convertView.getId() != android.R.id.text1)
                    convertView = getLayoutInflater().inflate(R.layout.list_header, null);
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getText1());
            } else {
                if (convertView == null || convertView.getId() == android.R.id.text1)
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getText1());
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(item.getText2());
            }
            return convertView;
        }
    }

    private class ListItem
    {
        protected final String mText1;
        protected final String mText2;

        public ListItem (String name, String value)
        {
            mText1 = name;
            mText2 = value;
        }

        public String getText1 () {
            return mText1;
        }

        public String getText2 () {
            return mText2;
        }
    }

    private class HeaderListItem extends ListItem
    {
        public HeaderListItem (String title)
        {
            super(title, null);
        }
    }
}
