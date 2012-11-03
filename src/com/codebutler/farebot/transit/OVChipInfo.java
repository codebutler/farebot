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
	private final Date mBirthdate;
	private final int  mActive;
	private final int  mLimit;
	private final int  mCharge;
	private final int  mUnknown;

	public OVChipInfo (
			Date birthdate,
			int  active,
			int  limit,
			int  charge,
			int  unknown
	) {
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

    	Date birthdate = new Date();
    	int active = 0;
    	int limit = 0;
 		int charge = 0;
 		int unknown = 0;

   	 	if ((data[13] & (byte)0x02) == (byte)0x02) {	// Has date of birth, so it's a personal card (no autocharge on anonymous cards)
   	 		int year = (Utils.convertBCDtoInteger(data[14]) * 100) + Utils.convertBCDtoInteger(data[15]);
	   	 	int month = Utils.convertBCDtoInteger(data[16]);
	   	 	int day = Utils.convertBCDtoInteger(data[17]);

	   	 	Calendar calendar = Calendar.getInstance();
	        calendar.set(Calendar.YEAR, year);
	        calendar.set(Calendar.MONTH, month - 1);
	        calendar.set(Calendar.DAY_OF_MONTH, day);
	        birthdate = calendar.getTime();

 			active = (data[22] >> 4) & (byte)0x0F;
 			limit = (((char)data[22] & (char)0x0F) << 12) | (((char)data[23] & (char)0xFF) << 4) | (((char)data[24] >> 4) & (char)0x0F);
 			charge = (((char)data[24] & (char)0x0F) << 12) | (((char)data[25] & (char)0xFF) << 4) | (((char)data[26] >> 4) & (char)0x0F);
 			unknown = (((char)data[26] & (char)0x0F) << 12) | (((char)data[27] & (char)0xFF) << 4) | (((char)data[28] >> 4) & (char)0x0F);
   	 	}

   	 	mBirthdate = birthdate;
   	 	mActive = active;
		mLimit = limit;
		mCharge = charge;
		mUnknown = unknown;
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
        	Date birthdate = null;
        	int active = 0;
        	int limit = 0;
     		int charge = 0;
     		int unknown = 0;

     		birthdate = new Date(source.readLong());
     		active = source.readInt();
     		limit = source.readInt();
     		charge = source.readInt();
     		unknown = source.readInt();

            return new OVChipInfo(birthdate, active,
            		limit, charge, unknown);
        }

        public OVChipInfo[] newArray (int size) {
            return new OVChipInfo[size];
        }
    };

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeLong(mBirthdate.getTime());
		parcel.writeInt(mActive);
		parcel.writeInt(mLimit);
		parcel.writeInt(mCharge);
		parcel.writeInt(mUnknown);
	}

	public static OVChipInfo fromXML (Element element) {
		Date birthdate;
		int active;
    	int limit;
 		int charge;
 		int unknown;

 		birthdate = new Date(Long.valueOf(element.getAttribute("birthdate")));
 		active = Integer.parseInt(element.getAttribute("active"));
 		limit = Integer.parseInt(element.getAttribute("limit"));
 		charge = Integer.parseInt(element.getAttribute("charge"));
 		unknown = Integer.parseInt(element.getAttribute("unknown"));

		return new OVChipInfo(birthdate, active,
				limit, charge, unknown);
    }

	public Element toXML (Document doc) throws Exception {
		Element info = doc.createElement("info");
		info.setAttribute("birthdate", Long.toString(mBirthdate.getTime()));
		info.setAttribute("active", Integer.toString(mActive));
		info.setAttribute("limit", Integer.toString(mLimit));
		info.setAttribute("charge", Integer.toString(mCharge));
		info.setAttribute("unknown", Integer.toString(mUnknown));

		return info;
    }
}