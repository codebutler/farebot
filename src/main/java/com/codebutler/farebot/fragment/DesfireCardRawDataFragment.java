/*
 * DesfireCardRawDataFragment.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.activity.AdvancedCardInfoActivity;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.InvalidDesfireFile;
import com.codebutler.farebot.card.desfire.RecordDesfireFileSettings;
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings;
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile;
import com.codebutler.farebot.card.desfire.ValueDesfireFileSettings;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Serializer;

public class DesfireCardRawDataFragment extends ExpandableListFragment {
    private DesfireCard mCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card_raw_data, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Serializer serializer = FareBotApplication.getInstance().getSerializer();
        mCard = (DesfireCard) Card.fromXml(serializer, getArguments().getString(AdvancedCardInfoActivity.EXTRA_CARD));
        setListAdapter(new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                return mCard.getApplications().size();
            }

            @Override
            public int getChildrenCount(int groupPosition) {
                return mCard.getApplications().get(groupPosition).getFiles().size();
            }

            @Override
            public Object getGroup(int groupPosition) {
                return mCard.getApplications().get(groupPosition);
            }

            @Override
            public Object getChild(int groupPosition, int childPosition) {
                return mCard.getApplications().get(groupPosition).getFiles().get(childPosition);
            }

            @Override
            public long getGroupId(int groupPosition) {
                return mCard.getApplications().get(groupPosition).getId();
            }

            @Override
            public long getChildId(int groupPosition, int childPosition) {
                return mCard.getApplications().get(groupPosition).getFiles().get(childPosition).getId();
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

                TextView textView = (TextView) convertView.findViewById(android.R.id.text1);

                DesfireApplication app = mCard.getApplications().get(groupPosition);

                textView.setText("Application: 0x" + Integer.toHexString(app.getId()));

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

                DesfireApplication app = mCard.getApplications().get(groupPosition);
                DesfireFile file = app.getFiles().get(childPosition);

                textView1.setText("File: 0x" + Integer.toHexString(file.getId()));

                if (file instanceof InvalidDesfireFile) {
                    textView2.setText(((InvalidDesfireFile) file).getErrorMessage());
                } else if (file instanceof UnauthorizedDesfireFile) {
                    textView2.setText(((UnauthorizedDesfireFile) file).getErrorMessage());
                } else {
                    if (file.getFileSettings() instanceof StandardDesfireFileSettings) {
                        StandardDesfireFileSettings fileSettings = (StandardDesfireFileSettings) file.getFileSettings();
                        textView2.setText(String.format("Type: %s, Size: %s", fileSettings.getFileTypeName(),
                                String.valueOf(fileSettings.getFileSize())));
                    } else if (file.getFileSettings() instanceof RecordDesfireFileSettings) {
                        RecordDesfireFileSettings fileSettings = (RecordDesfireFileSettings) file.getFileSettings();
                        textView2.setText(String.format("Type: %s, Cur Records: %s, Max Records: %s, Record Size: %s",
                                fileSettings.getFileTypeName(),
                                String.valueOf(fileSettings.getCurRecords()),
                                String.valueOf(fileSettings.getMaxRecords()),
                                String.valueOf(fileSettings.getRecordSize())));
                    } else if (file.getFileSettings() instanceof ValueDesfireFileSettings) {
                        ValueDesfireFileSettings fileSettings = (ValueDesfireFileSettings) file.getFileSettings();

                        textView2.setText(String.format("Type: %s, Range: %s - %s, Limited Credit: %s (%s)",
                                fileSettings.getFileTypeName(),
                                fileSettings.getLowerLimit(),
                                fileSettings.getUpperLimit(),
                                fileSettings.getLimitedCreditValue(),
                                fileSettings.getLimitedCreditEnabled() ? "enabled" : "disabled"
                        ));
                    } else {
                        textView2.setText("Unknown file type");
                    }
                }

                return convertView;
            }
        });
    }

    @Override
    public boolean onListChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
                                    long id) {
        DesfireFile file = (DesfireFile) getExpandableListAdapter().getChild(groupPosition, childPosition);

        if (file instanceof InvalidDesfireFile) {
            return false;
        }

        String data = Utils.getHexString(file.getData(), "");

        new AlertDialog.Builder(getActivity())
                .setTitle("File Content")
                .setPositiveButton(android.R.string.ok, null)
                .setMessage(data)
                .show();

        return true;
    }
}
