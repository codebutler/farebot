/*
 * OVChipPreamble.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.transit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.Utils;

public class OVChipPreamble implements Parcelable {
    private final String mId;
    private final int mCheckbit;
    private final String mManufacturer;
    private final String mPublisher;
    private final String mUnknownConstant1;
    private final int mExpdate;
    private final String mUnknownConstant2;
    private final int mType;

    public OVChipPreamble (
            String id,
            int checkbit,
            String manufacturer,
            String publisher,
            String unknownConstant1,
            int expdate,
            String unknownConstant2,
            int type
    ) {
        mId = id;
        mCheckbit = checkbit;
        mManufacturer = manufacturer;
        mPublisher = publisher;
        mUnknownConstant1 = unknownConstant1;
        mExpdate = expdate;
        mUnknownConstant2 = unknownConstant2;
        mType = type;
    }

    public OVChipPreamble (byte[] data) {
        if (data == null) {
            data = new byte[48];
        }

        String id = "";
        int checkbit = 0;
        String manufacturer = "";
        String publisher = "";
        String unknownConstant1 = "";
        int expdate = 0;
        String unknownConstant2 = "";
        int type = 0;

        String hex = Utils.getHexString(data, null);

        id = hex.substring(0, 8);
        checkbit = Utils.getBitsFromBuffer(data, 32, 8);
        manufacturer = hex.substring(10, 20);
        publisher = hex.substring(20, 32);
        unknownConstant1 = hex.substring(32, 54);
        expdate = Utils.getBitsFromBuffer(data, 216, 20);
        unknownConstant2 = hex.substring(59, 68);
        type = Utils.getBitsFromBuffer(data, 276, 4);

        mId = id;
        mCheckbit = checkbit;
        mManufacturer = manufacturer;
        mPublisher = publisher;
        mUnknownConstant1 = unknownConstant1;
        mExpdate = expdate;
        mUnknownConstant2 = unknownConstant2;
        mType = type;
    }

    public String getId() {
        return mId;
    }

    public int getCheckbit() {
        return mCheckbit;
    }

    public String getManufacturer() {
        return mManufacturer;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public String getUnknownConstant1() {
        return mUnknownConstant1;
    }

    public int getExpdate() {
        return mExpdate;
    }

    public String getUnknownConstant2() {
        return mUnknownConstant2;
    }

    public int getType() {
        return mType;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<OVChipPreamble> CREATOR = new Parcelable.Creator<OVChipPreamble>() {
        public OVChipPreamble createFromParcel(Parcel source) {
            String id = "";
            int checkbit = 0;
            String manufacturer = "";
            String publisher = "";
            String unknownConstant1 = "";
            int expdate = 0;
            String unknownConstant2 = "";
            int type = 0;

            id = source.readString();
            checkbit = source.readInt();
            manufacturer = source.readString();
            publisher = source.readString();
            unknownConstant1 = source.readString();
            expdate = source.readInt();
            unknownConstant2 = source.readString();
            type = source.readInt();

            return new OVChipPreamble(id, checkbit,
                    manufacturer, publisher,
                    unknownConstant1, expdate,
                    unknownConstant2, type);
        }

        public OVChipPreamble[] newArray (int size) {
            return new OVChipPreamble[size];
        }
    };

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mId);
        parcel.writeInt(mCheckbit);
        parcel.writeString(mManufacturer);
        parcel.writeString(mPublisher);
        parcel.writeString(mUnknownConstant1);
        parcel.writeInt(mExpdate);
        parcel.writeString(mUnknownConstant2);
        parcel.writeInt(mType);
    }

    public static OVChipPreamble fromXML (Element element) {
        String id;
        int checkbit;
        String manufacturer;
        String publisher;
        String unknownConstant1;
        int expdate;
        String unknownConstant2;
        int type;

        id = element.getAttribute("id");
        checkbit = Integer.parseInt(element.getAttribute("checkbit"));
        manufacturer = element.getAttribute("manufacturer");
        publisher = element.getAttribute("publisher");
        unknownConstant1 = element.getAttribute("unknownconstant1");
        expdate = Integer.parseInt(element.getAttribute("expdate"));
        unknownConstant2 = element.getAttribute("unknownconstant2");
        type = Integer.parseInt(element.getAttribute("type"));

        return new OVChipPreamble(id, checkbit,
                manufacturer, publisher,
                unknownConstant1, expdate,
                unknownConstant2, type);
    }

    public Element toXML (Document doc) throws Exception {
        Element preamble = doc.createElement("preamble");
        preamble.setAttribute("id", getId());
        preamble.setAttribute("checkbit", Integer.toString(mCheckbit));
        preamble.setAttribute("manufacturer", getManufacturer());
        preamble.setAttribute("publisher", getPublisher());
        preamble.setAttribute("unknownconstant1", getUnknownConstant1());
        preamble.setAttribute("expdate", Integer.toString(mExpdate));
        preamble.setAttribute("unknownconstant2", getUnknownConstant2());
        preamble.setAttribute("type", Integer.toString(mType));

        return preamble;
    }
}