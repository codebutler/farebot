/*
 * OVChipInfo.java
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

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Calendar;
import java.util.Date;

public class OVChipInfo implements Parcelable {
    private final int  mCompany;
    private final int  mExpdate;
    private final Date mBirthdate;
    private final int  mActive;
    private final int  mLimit;
    private final int  mCharge;
    private final int  mUnknown;

    public OVChipInfo (
    		int company,
    		int expdate,
            Date birthdate,
            int  active,
            int  limit,
            int  charge,
            int  unknown
    ) {
    	mCompany = company;
    	mExpdate = expdate;
        mBirthdate = birthdate;
        mActive = active;
        mLimit = limit;
        mCharge = charge;
        mUnknown = unknown;
    }

    public OVChipInfo (byte[] data) {
        if (data == null) {
            data = new byte[48];
        }

        int company = 0;
        int expdate = 0;
        Date birthdate = new Date();
        int active = 0;
        int limit = 0;
        int charge = 0;
        int unknown = 0;

        company = ((char)data[6] >> 3) & (char)0x1F; // Could be 4 bits though
        expdate = (((char)data[6] & (char)0x07) << 11) | (((char)data[7] & (char)0xFF) << 3) | (((char)data[8] >> 5) & (char)0x07);

        if ((data[13] & (byte)0x02) == (byte)0x02) {    // Has date of birth, so it's a personal card (no autocharge on anonymous cards)
            int year = (Utils.convertBCDtoInteger(data[14]) * 100) + Utils.convertBCDtoInteger(data[15]);
            int month = Utils.convertBCDtoInteger(data[16]);
            int day = Utils.convertBCDtoInteger(data[17]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            birthdate = calendar.getTime();

            active = (data[22] >> 5) & (byte)0x07;
            limit = (((char)data[22] & (char)0x1F) << 11) | (((char)data[23] & (char)0xFF) << 3) | (((char)data[24] >> 5) & (char)0x07);
            charge = (((char)data[24] & (char)0x1F) << 11) | (((char)data[25] & (char)0xFF) << 3) | (((char)data[26] >> 5) & (char)0x07);
            unknown = (((char)data[26] & (char)0x1F) << 11) | (((char)data[27] & (char)0xFF) << 3) | (((char)data[28] >> 5) & (char)0x07);
        }

        mCompany = company;
        mExpdate = expdate;
        mBirthdate = birthdate;
        mActive = active;
        mLimit = limit;
        mCharge = charge;
        mUnknown = unknown;
    }

    public int getCompany() {
        return mCompany;
    }

    public int getExpdate() {
        return mExpdate;
    }

    public Date getBirthdate() {
        return mBirthdate;
    }

    public int getActive() {
        return mActive;
    }

    public int getLimit() {
        return mLimit;
    }

    public int getCharge() {
        return mCharge;
    }

    public int getUnknown() {
        return mUnknown;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<OVChipInfo> CREATOR = new Parcelable.Creator<OVChipInfo>() {
        public OVChipInfo createFromParcel(Parcel source) {
        int company = 0;
        int expdate = 0;
        Date birthdate = null;
        int active = 0;
        int limit = 0;
        int charge = 0;
        int unknown = 0;

        company = source.readInt();
        expdate = source.readInt();
        birthdate = new Date(source.readLong());
        active = source.readInt();
        limit = source.readInt();
        charge = source.readInt();
        unknown = source.readInt();

        return new OVChipInfo(company, expdate, birthdate,
                    active, limit, charge, unknown);
        }

        public OVChipInfo[] newArray (int size) {
            return new OVChipInfo[size];
        }
    };

    public void writeToParcel(Parcel parcel, int flags) {
    	parcel.writeInt(mCompany);
        parcel.writeInt(mExpdate);
        parcel.writeLong(mBirthdate.getTime());
        parcel.writeInt(mActive);
        parcel.writeInt(mLimit);
        parcel.writeInt(mCharge);
        parcel.writeInt(mUnknown);
    }

    public static OVChipInfo fromXML (Element element) {
        int company;
        int expdate;
        Date birthdate;
        int active;
        int limit;
        int charge;
        int unknown;

        company = Integer.parseInt(element.getAttribute("company"));
        expdate = Integer.parseInt(element.getAttribute("expdate"));
        birthdate = new Date(Long.valueOf(element.getAttribute("birthdate")));
        active = Integer.parseInt(element.getAttribute("active"));
        limit = Integer.parseInt(element.getAttribute("limit"));
        charge = Integer.parseInt(element.getAttribute("charge"));
        unknown = Integer.parseInt(element.getAttribute("unknown"));

        return new OVChipInfo(company, expdate, birthdate,
                active, limit, charge, unknown);
    }

    public Element toXML (Document doc) throws Exception {
        Element info = doc.createElement("info");
        info.setAttribute("company", Integer.toString(mCompany));
        info.setAttribute("expdate", Integer.toString(mExpdate));
        info.setAttribute("birthdate", Long.toString(mBirthdate.getTime()));
        info.setAttribute("active", Integer.toString(mActive));
        info.setAttribute("limit", Integer.toString(mLimit));
        info.setAttribute("charge", Integer.toString(mCharge));
        info.setAttribute("unknown", Integer.toString(mUnknown));

        return info;
    }
}
