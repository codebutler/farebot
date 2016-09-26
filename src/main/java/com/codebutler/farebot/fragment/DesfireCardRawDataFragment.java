/*
 * DesfireCardRawDataFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.AdvancedCardInfoActivity;
import com.codebutler.farebot.card.desfire.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.RecordDesfireFileSettings;
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings;
import com.codebutler.farebot.card.desfire.ValueDesfireFileSettings;
import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication;
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard;
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile;
import com.codebutler.farebot.serialize.CardSerializer;

public class DesfireCardRawDataFragment extends ExpandableListFragment {

    private RawDesfireCard mRawCard;

    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_raw_data, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CardSerializer cardSerializer = ((FareBotApplication) getActivity().getApplication()).getCardSerializer();
        String serializedCard = getArguments().getString(AdvancedCardInfoActivity.EXTRA_RAW_CARD);
        mRawCard = (RawDesfireCard) cardSerializer.deserialize(serializedCard);

        setListAdapter(new DesfireCardRawDataAdapter());
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        RawDesfireFile file = getExpandableListAdapter().getChild(groupPosition, childPosition);

        ByteArray data = file.fileData();
        if (data == null) {
            return false;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle("File Content")
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data.hex())
                .show();

        return true;
    }

    @Override
    DesfireCardRawDataAdapter getExpandableListAdapter() {
        return (DesfireCardRawDataAdapter) super.getExpandableListAdapter();
    }

    private class DesfireCardRawDataAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return mRawCard.applications().size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mRawCard.applications().get(groupPosition).files().size();
        }

        @Override
        public RawDesfireApplication getGroup(int groupPosition) {
            return mRawCard.applications().get(groupPosition);
        }

        @Override
        public RawDesfireFile getChild(int groupPosition, int childPosition) {
            return mRawCard.applications().get(groupPosition).files().get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return mRawCard.applications().get(groupPosition).appId();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return mRawCard.applications().get(groupPosition).files().get(childPosition).fileId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }

            RawDesfireApplication app = mRawCard.applications().get(groupPosition);

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(String.format("Application: 0x%s", Integer.toHexString(app.appId())));

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                convertView = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }

            TextView textView1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) convertView.findViewById(android.R.id.text2);

            RawDesfireApplication app = mRawCard.applications().get(groupPosition);
            RawDesfireFile file = app.files().get(childPosition);

            textView1.setText(String.format("File: 0x%s", Integer.toHexString(file.fileId())));

            RawDesfireFile.Error error = file.error();
            if (error != null) {
                textView2.setText(error.message());
            } else {
                // FIXME: Avoid calling parse() every time
                DesfireFileSettings fileSettings = file.fileSettings().parse();
                if (fileSettings instanceof StandardDesfireFileSettings) {
                    StandardDesfireFileSettings standardFileSettings = (StandardDesfireFileSettings) fileSettings;
                    textView2.setText(String.format("Type: %s, Size: %s", standardFileSettings.getFileTypeName(),
                            String.valueOf(standardFileSettings.getFileSize())));
                } else if (fileSettings instanceof RecordDesfireFileSettings) {
                    RecordDesfireFileSettings recordFileSettings = (RecordDesfireFileSettings) fileSettings;
                    textView2.setText(String.format("Type: %s, Cur Records: %s, Max Records: %s, Record Size: %s",
                            recordFileSettings.getFileTypeName(),
                            String.valueOf(recordFileSettings.getCurRecords()),
                            String.valueOf(recordFileSettings.getMaxRecords()),
                            String.valueOf(recordFileSettings.getRecordSize())));
                } else if (fileSettings instanceof ValueDesfireFileSettings) {
                    ValueDesfireFileSettings valueFileSettings = (ValueDesfireFileSettings) fileSettings;
                    textView2.setText(String.format("Type: %s, Range: %s - %s, Limited Credit: %s (%s)",
                            valueFileSettings.getFileTypeName(),
                            valueFileSettings.getLowerLimit(),
                            valueFileSettings.getUpperLimit(),
                            valueFileSettings.getLimitedCreditValue(),
                            valueFileSettings.getLimitedCreditEnabled() ? "enabled" : "disabled"
                    ));
                } else {
                    textView2.setText("Unknown file type");
                }
            }

            return convertView;
        }
    }
}
