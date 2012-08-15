/*
 * OVChipSubscription.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 *
 * Based on code from http://http://www.huuf.info/OV/
 * by Huuf. See project URL for complete author information.
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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class OVChipSubscription implements Parcelable {
	private final int mSubscriptionSlot;
	private final int mValid;
	private final int mId;
	private final int mCompany;
	private final int mSubscription;
	private final int mValidFrom;
	private final int mValidTo;
	private final int mMachineId;
	private final int mType1;
	private final int mType2;
	private final int mUsed;
	private final int mRest;
	private final String mErrorMessage;

	public OVChipSubscription (int subscriptionSlot, String errorMessage) {
		mSubscriptionSlot = subscriptionSlot;
		mValid = 0;
		mErrorMessage = errorMessage;
		mId = 0;
		mCompany = 0;
		mSubscription = 0;
		mValidFrom = 0;
		mValidTo = 0;
		mMachineId = 0;
		mType1 = 0;
		mType2 = 0;
		mUsed = 0;
		mRest = 0;
	}

	public OVChipSubscription (
			int subscriptionSlot,
			int valid,
			int id,
			int company,
			int subscription,
			int validFrom,
			int validTo,
			int machineId,
			int type1,
			int type2,
			int used,
			int rest
	) {
		mSubscriptionSlot = subscriptionSlot;
		mValid = valid;
		mId = id;
		mCompany = company;
		mSubscription = subscription;
		mValidFrom = validFrom;
		mValidTo = validTo;
		mMachineId = machineId;
		mType1 = type1;
		mType2 = type2;
		mUsed = used;
		mRest = rest;
		mErrorMessage = "";
	}

	public OVChipSubscription (int subscriptionSlot, byte[] data, int type1, int type2, int used, int rest) {
		if (data == null) {
			data = new byte[48];
		}

		int valid = 1;
		int id = 0;
		int company = 0;
		int subscription = 0;
		int validFrom = 0;
		int validTo = 0;
		int machineId = 0;
		String errorMessage = "";

		if (data[0] == (byte)0x0A && data[1] == (byte)0x00 && data[2] == (byte)0xE0 && ((data[3] & (byte)0xF0) == (byte)0x00)) {
			id = ((data[9] & (byte)0xFF) << 4) | ((data[10] >> 4) & (byte)0x0F);
			company = ((data[4] >> 4) & (byte)0x0F);
			subscription = (((char)data[4] & (char)0x0F) << 12) | (((char)data[5] & (char)0xFF) << 4) | (((char)data[6] >> 4) & (char)0x0F);
			validFrom = (((char)data[11] & (char)0x07) << 11) | (((char)data[12] & (char)0xFF) << 3) | (((char)data[13] >> 5) & (char)0x07);
			validTo = (((char)data[13] & (char)0x1F) << 9) | (((char)data[14] & (char)0xFF) << 1) | (((char)data[15] >> 7) & (char)0x01);
			machineId = (((char)data[21] & (char)0x03) << 22) | (((char)data[22] & (char)0xFF) << 14) | (((char)data[23] & (char)0xFF) << 6) | (((char)data[24] >> 2) & (char)0x3F);	
		} else if (data[0] == (byte)0x0A && data[1] == (byte)0x02 && data[2] == (byte)0xE0 && ((data[3] & (byte)0xF0) == (byte)0x00)) {
			id = ((data[9] & (byte)0xFF) << 4) | ((data[10] >> 4) & (byte)0x0F);
			company = ((data[4] >> 4) & (byte)0x0F);
			subscription = (((char)data[4] & (char)0x0F) << 12) | (((char)data[5] & (char)0xFF) << 4) | (((char)data[6] >> 4) & (char)0x0F);
			validFrom = ((char)(data[12] & (char)0x01) << 13) | (((char)data[13] & (char)0xFF) << 5) | (((char)data[14] >> 3) & (char)0x1F);

			if ((((data[11] & (byte)0x1F) << 7) | ((data[12] >> 1) & (byte)0x7F)) == 31) {
				validTo = (((char)data[16] & (char)0xFF) << 6) | (((char)data[17] >> 2) & (char)0x3F);
				machineId = (((char)data[25] & (char)0x03) << 22) | (((char)data[26] & (char)0xFF) << 14) | (((char)data[27] & (char)0xFF) << 6) | (((char)data[28] >> 2) & (char)0x3F);
			}

			if ((((data[11] & (byte)0x1F) << 7) | ((data[12] >> 1) & (byte)0x7F)) == 21) {
				validTo = (((char)data[14] & (char)0x07) << 11) | (((char)data[15] & (char)0xFF) << 3) | (((char)data[16] >> 5) & (char)0x07);
				machineId = (((char)data[23] & (char)0xFF) << 16) | (((char)data[24] & (char)0xFF) << 8) | (((char)data[25] & (char)0xFF));
			}
		} else {
			valid = 0;
			errorMessage = "No subscription";
		}

		mSubscriptionSlot = subscriptionSlot;
		mValid = valid;
		mId = id;
		mCompany = company;
		mSubscription = subscription;
		mValidFrom = validFrom;
		mValidTo = validTo;
		mMachineId = machineId;
		mType1 = type1;
		mType2 = type2;
		mUsed = used;
		mRest = rest;
		mErrorMessage = errorMessage;
	}

	public int getSubscriptionSlot() {
		return mSubscriptionSlot;
	}

	public int getValid() {
		return mValid;
	}

	public int getId() {
		return mId;
	}

	public int getCompany() {
		return mCompany;
	}

	public int getSubscription() {
		return mSubscription;
	}

	public int getValidFrom() {
		return mValidFrom;
	}

	public int getValidTo() {
		return mValidTo;
	}

	public int getMachineId() {
		return mMachineId;
	}

	public int getType1() {
		return mType1;
	}

	public int getType2() {
		return mType2;
	}

	public int getUsed() {
		return mUsed;
	}

	public int getRest() {
		return mRest;
	}

	public String getErrorMessage() {
		return mErrorMessage;
	}

	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<OVChipSubscription> CREATOR = new Parcelable.Creator<OVChipSubscription>() {
        public OVChipSubscription createFromParcel(Parcel source) {
        	int subscriptionSlot;
			int valid;
			int id;
			int company;
			int subscription;
			int validFrom;
			int validTo;
			int machineId;
			int type1;
			int type2;
			int used;
			int rest;

			subscriptionSlot = source.readInt();
			valid = source.readInt();

			if (valid == 0) {
                return new OVChipSubscription(subscriptionSlot, source.readString());
            }

			id = source.readInt();
			company = source.readInt();
			subscription = source.readInt();
			validFrom = source.readInt();
			validTo = source.readInt();
			machineId = source.readInt();
			type1 = source.readInt();
			type2 = source.readInt();
			used = source.readInt();
			rest = source.readInt();

            return new OVChipSubscription(subscriptionSlot,
        			valid, id, company, subscription,
        			validFrom, validTo, machineId, type1,
        			type2, used, rest);
        }

        public OVChipSubscription[] newArray (int size) {
            return new OVChipSubscription[size];
        }
    };

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(mSubscriptionSlot);
		parcel.writeInt(mValid);
		if (mValid == 0) {
			parcel.writeString(mErrorMessage);
		} else {
			parcel.writeInt(mId);
			parcel.writeInt(mCompany);
			parcel.writeInt(mSubscription);
			parcel.writeInt(mValidFrom);
			parcel.writeInt(mValidTo);
			parcel.writeInt(mMachineId);
			parcel.writeInt(mType1);
			parcel.writeInt(mType2);
			parcel.writeInt(mUsed);
			parcel.writeInt(mRest);
		}
	}

	public static OVChipSubscription fromXML (Element element) {
		int subscriptionSlot;
		int valid;
		int id;
		int company;
		int subscription;
		int validFrom;
		int validTo;
		int machineId;
		int type1;
		int type2;
		int used;
		int rest;

		subscriptionSlot = Integer.parseInt(element.getAttribute("slot"));
		if (element.getAttribute("valid").equals("0"))
			return new OVChipSubscription(subscriptionSlot, element.getAttribute("error"));

		valid = Integer.parseInt(element.getAttribute("valid"));
		id = Integer.parseInt(element.getAttribute("id"));
		company = Integer.parseInt(element.getAttribute("company"));
		subscription = Integer.parseInt(element.getAttribute("subscription"));
		validFrom = Integer.parseInt(element.getAttribute("validfrom"));
		validTo = Integer.parseInt(element.getAttribute("validto"));
		machineId = Integer.parseInt(element.getAttribute("machineid"));
		type1 = Integer.parseInt(element.getAttribute("type1"));
		type2 = Integer.parseInt(element.getAttribute("type2"));
		used = Integer.parseInt(element.getAttribute("used"));
		rest = Integer.parseInt(element.getAttribute("rest"));

		return new OVChipSubscription(subscriptionSlot,
    			valid, id, company, subscription,
    			validFrom, validTo, machineId, type1,
    			type2, used, rest);
    }

	public Element toXML (Document doc) throws Exception {
		Element subscription = doc.createElement("subscription");
		if (mValid == 0) {
			subscription.setAttribute("slot", Integer.toString(mSubscriptionSlot));
			subscription.setAttribute("valid", Integer.toString(mValid));
			subscription.setAttribute("error", getErrorMessage());
		} else {
			subscription.setAttribute("slot", Integer.toString(mSubscriptionSlot));
			subscription.setAttribute("valid", Integer.toString(mValid));
			subscription.setAttribute("id", Integer.toString(mId));
			subscription.setAttribute("company", Integer.toString(mCompany));
			subscription.setAttribute("subscription", Integer.toString(mSubscription));
			subscription.setAttribute("validfrom", Integer.toString(mValidFrom));
			subscription.setAttribute("validto", Integer.toString(mValidTo));
			subscription.setAttribute("machineid", Integer.toString(mMachineId));
			subscription.setAttribute("type1", Integer.toString(mType1));
			subscription.setAttribute("type2", Integer.toString(mType2));
			subscription.setAttribute("used", Integer.toString(mUsed));
			subscription.setAttribute("rest", Integer.toString(mRest));
		}

		return subscription;
    }
}