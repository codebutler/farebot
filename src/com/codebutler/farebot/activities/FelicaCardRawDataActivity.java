/*
 * FelicaCardRawDataActivity.java
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

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.felica.FelicaBlock;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.felica.FelicaService;
import com.codebutler.farebot.felica.FelicaSystem;

import java.util.ArrayList;
import java.util.List;

public class FelicaCardRawDataActivity extends ExpandableListActivity {
    private FelicaCard mCard;

    public void onCreate (Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_card_raw_data);

        mCard = (FelicaCard) getIntent().getParcelableExtra(AdvancedCardInfoActivity.EXTRA_CARD);

        setListAdapter(new BaseExpandableListAdapter() {
            public int getGroupCount() {
                return mCard.getSystems().length;
            }

            public Object getGroup(int groupPosition) {
                return mCard.getSystems()[groupPosition];
            }

            public long getGroupId(int groupPosition) {
                return mCard.getSystems()[groupPosition].getCode();
            }

            public int getChildrenCount(int groupPosition) {
                return mCard.getSystems()[groupPosition].getServices().length;
            }

            public Object getChild(int groupPosition, int childPosition) {
                return mCard.getSystems()[groupPosition].getServices()[childPosition];
            }

            public long getChildId(int groupPosition, int childPosition) {
                return mCard.getSystems()[groupPosition].getServices()[childPosition].getServiceCode();
            }

            public boolean hasStableIds() {
                return true;
            }

            public boolean isChildSelectable(int i, int i1) {
                return true;
            }

            public View getGroupView (int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_1, null);
                    view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, 80));
                }

                FelicaSystem system = mCard.getSystems()[groupPosition];

                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(String.format("System: 0x%s", Integer.toHexString(system.getCode())));

                return view;
            }

            public View getChildView(int groupPosition, int childPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, null);
                    view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
                }

                TextView textView1 = (TextView) view.findViewById(android.R.id.text1);
                TextView textView2 = (TextView) view.findViewById(android.R.id.text2);

                FelicaSystem system   = mCard.getSystems()[groupPosition];
                FelicaService service = system.getServices()[childPosition];

                textView1.setText(String.format("Service: 0x%s", Integer.toHexString(service.getServiceCode())));
                textView2.setText(String.format("%s block(s)", service.getBlocks().length));

                return view;
            }
        });

        getExpandableListView().setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                FelicaService service = (FelicaService) getExpandableListAdapter().getChild(groupPosition, childPosition);

                List<String> items = new ArrayList<String>();
                for (FelicaBlock block : service.getBlocks()) {
                    items.add(String.format("%02d: %s", block.getAddress(), Utils.getHexString(block.getData(), "<ERR>")));
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter(FelicaCardRawDataActivity.this, R.layout.monospace_list_item, items);

                new AlertDialog.Builder(FelicaCardRawDataActivity.this)
                    .setTitle(String.format("Service 0x%s", Integer.toHexString(service.getServiceCode())))
                    .setPositiveButton(android.R.string.ok, null)
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int position) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            clipboard.setText(adapter.getItem(position));
                            Toast.makeText(FelicaCardRawDataActivity.this, "Copied!" , 5).show();
                        }
                    })
                    .show();

                return true;
            }
        });
    }
}
