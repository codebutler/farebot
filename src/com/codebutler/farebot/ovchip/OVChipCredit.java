/*
 * OVChipCredit.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.ovchip;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.codebutler.farebot.Utils;

import android.os.Parcel;
import android.os.Parcelable;

public class OVChipCredit implements Parcelable {
	private final int mId;
	private final int mCreditId;
	private final int mCredit;
	private final int mBanbits;

	public OVChipCredit (
			int id,
			int creditId,
			int credit,
			int banbits
	) {
		mId = id;
		mCreditId = creditId;
		mCredit = credit;
		mBanbits = banbits;
	}

	public OVChipCredit (byte[] data) {
		if (data == null) {
			data = new byte[16];
		}

		int id;
		int creditId;
		int credit;
		int banbits;

		banbits = Utils.getBitsFromBuffer(data, 0, 9);
		id = Utils.getBitsFromBuffer(data, 9, 12);
		creditId = Utils.getBitsFromBuffer(data, 56, 12);
		credit = Utils.getBitsFromBuffer(data, 78, 15);	// Skipping the first bit (77)...

		if ((data[9] & (byte)0x04) != 4) {	// ...as the first bit is used to see if the credit is negative or not
			credit ^= (char)0x7FFF;
			credit = credit * -1;
		}

		mId = id;
		mCreditId = creditId;
		mCredit = credit;
		mBanbits = banbits;
	}

	public int getId() {
		return mId;
	}

	public int getCreditId() {
		return mCreditId;
	}

	public int getCredit() {
		return mCredit;
	}

	public int getBanbits() {
		return mBanbits;
	}

	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<OVChipCredit> CREATOR = new Parcelable.Creator<OVChipCredit>() {
        public OVChipCredit createFromParcel(Parcel source) {
        	int id = 0;
    		int creditId = 0;
    		int credit = 0;
    		int banbits = 0;

    		id = source.readInt();
    		creditId = source.readInt();
    		credit = source.readInt();
    		banbits = source.readInt();

            return new OVChipCredit(id, creditId,
        			credit, banbits);
        }

        public OVChipCredit[] newArray (int size) {
            return new OVChipCredit[size];
        }
    };

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(mId);
		parcel.writeInt(mCreditId);
		parcel.writeInt(mCredit);
		parcel.writeInt(mBanbits);
	}

	public static OVChipCredit fromXML (Element element) {
		int id = 0;
		int creditId = 0;
		int credit = 0;
		int banbits = 0;

		id = Integer.parseInt(element.getAttribute("id"));
		creditId = Integer.parseInt(element.getAttribute("creditid"));
		credit = Integer.parseInt(element.getAttribute("credit"));
		banbits = Integer.parseInt(element.getAttribute("banbits"));

		return new OVChipCredit(id, creditId,
    			credit, banbits);
    }

	public Element toXML (Document doc) throws Exception {
		Element credit = doc.createElement("credit");
		credit.setAttribute("id", Integer.toString(mId));
		credit.setAttribute("creditid", Integer.toString(mCreditId));
		credit.setAttribute("credit", Integer.toString(mCredit));
		credit.setAttribute("banbits", Integer.toString(mBanbits));

		return credit;
    }
}