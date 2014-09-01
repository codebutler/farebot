/*
* OVCSubscription.java
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

package com.codebutler.farebot.transit.ovc;

import android.os.Parcel;

import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.util.Utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OVChipSubscription extends Subscription {
    private final int mId;
    private final int mUnknown1;
    private final long mValidFromDate;
    private final long mValidFromTime;
    private final long mValidToDate;
    private final long mValidToTime;
    private final int mUnknown2;
    private final int mAgency;
    private final int mMachineId;
    private final int mSubscription;
    private final int mSubscriptionAddress;
    private final int mType1;
    private final int mType2;
    private final int mUsed;
    private final int mRest;

    private static Map<Integer, String> sSubscriptions = new HashMap<Integer, String>() {
        /* It seems that all the IDs are unique, so why bother with the companies? */ {
            /* NS */
            put(0x0005, "OV-jaarkaart");
            put(0x0007, "OV-Bijkaart 1e klas");
            put(0x0011, "NS Businesscard");
            put(0x0019, "Voordeelurenabonnement (twee jaar)");
            put(0x00AF, "Studenten OV-chipkaart week (2009)");
            put(0x00B0, "Studenten OV-chipkaart weekend (2009)");
            put(0x00B1, "Studentenkaart korting week (2009)");
            put(0x00B2, "Studentenkaart korting weekend (2009)");
            put(0x00C9, "Reizen op saldo bij NS, 1e klasse");
            put(0x00CA, "Reizen op saldo bij NS, 2de klasse");
            put(0x00CE, "Voordeelurenabonnement reizen op saldo");
            put(0x00E5, "Reizen op saldo (tijdelijk eerste klas)");
            put(0x00E6, "Reizen op saldo (tijdelijk tweede klas)");
            put(0x00E7, "Reizen op saldo (tijdelijk eerste klas korting)");
            /* Arriva */
            put(0x059A, "Dalkorting");
            /* Veolia */
            put(0x0626, "DALU Dalkorting");
            /* Connexxion */
            put(0x0692, "Daluren Oost-Nederland");
            put(0x069C, "Daluren Oost-Nederland");
            /* DUO */
            put(0x09C6, "Student weekend-vrij");
            put(0x09C7, "Student week-korting");
            put(0x09C9, "Student week-vrij");
            put(0x09CA, "Student weekend-korting");
            /* GVB */
            put(0x0BBD, "Fietssupplement");
        }
    };

    public static Creator<OVChipSubscription> CREATOR = new Creator<OVChipSubscription>() {
        public OVChipSubscription createFromParcel(Parcel parcel) {
            return new OVChipSubscription(parcel);
        }

        public OVChipSubscription[] newArray(int size) {
            return new OVChipSubscription[size];
        }
    };

    public OVChipSubscription(int subscriptionAddress, byte[] data, int type1, int type2, int used, int rest) {
        mSubscriptionAddress = subscriptionAddress;
        mType1 = type1;
        mType2 = type2;
        mUsed = used;
        mRest = rest;

        if (data == null) {
            data = new byte[48];
        }

        int id = 0;
        int company = 0;
        int subscription = 0;
        int unknown1 = 0;
        int validFromDate = 0;
        int validFromTime = 0;
        int validToDate = 0;
        int validToTime = 0;
        int unknown2 = 0;
        int machineId = 0;

        int iBitOffset = 0;
        int fieldbits = Utils.getBitsFromBuffer(data, 0, 28);
        iBitOffset += 28;
        int subfieldbits = 0;

        if (fieldbits != 0x00) {
            if ((fieldbits & 0x0000200) != 0x00) {
                company = Utils.getBitsFromBuffer(data, iBitOffset, 8);
                iBitOffset += 8;
            }

            if ((fieldbits & 0x0000400) != 0x00) {
                subscription = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 24;	/* skipping the first 8 bits, as they are not used OR don't belong to subscriptiontype at all */
            }

            if ((fieldbits & 0x0000800) != 0x00) {
                id = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((fieldbits & 0x0002000) != 0x00) {
                unknown1 = Utils.getBitsFromBuffer(data, iBitOffset, 10);
                iBitOffset += 10;
            }

            if ((fieldbits & 0x0200000) != 0x00) {
                subfieldbits = Utils.getBitsFromBuffer(data, iBitOffset, 9);
                iBitOffset += 9;
            }

            if (subfieldbits != 0x00) {
                if ((subfieldbits & 0x0000001) != 0x00) {
                    validFromDate = Utils.getBitsFromBuffer(data, iBitOffset, 14);
                    iBitOffset += 14;
                }

                if ((subfieldbits & 0x0000002) != 0x00) {
                    validFromTime = Utils.getBitsFromBuffer(data, iBitOffset, 11);
                    iBitOffset += 11;
                }

                if ((subfieldbits & 0x0000004) != 0x00) {
                    validToDate = Utils.getBitsFromBuffer(data, iBitOffset, 14);
                    iBitOffset += 14;
                }

                if ((subfieldbits & 0x0000008) != 0x00) {
                    validToTime = Utils.getBitsFromBuffer(data, iBitOffset, 11);
                    iBitOffset += 11;
                }

                if ((subfieldbits & 0x0000010) != 0x00) {
                    unknown2 = Utils.getBitsFromBuffer(data, iBitOffset, 53);
                    iBitOffset += 53;
                }
            }

            if ((fieldbits & 0x0800000) != 0x00) {
                machineId = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }
        } else {
            throw new IllegalArgumentException("Not valid");
        }

        mId            = id;
        mAgency        = company;
        mSubscription  = subscription;
        mUnknown1      = unknown1;
        mValidFromDate = validFromDate;
        mValidFromTime = validFromTime;
        mValidToDate   = validToDate;
        mValidToTime   = validToTime;
        mUnknown2      = unknown2;
        mMachineId     = machineId;
    }

    public OVChipSubscription(Parcel parcel) {
        mId            = parcel.readInt();
        mUnknown1      = parcel.readInt();
        mValidFromDate = parcel.readLong();
        mValidFromTime = parcel.readLong();
        mValidToDate   = parcel.readLong();
        mValidToTime   = parcel.readLong();
        mUnknown2      = parcel.readInt();
        mAgency        = parcel.readInt();
        mMachineId     = parcel.readInt();
        mSubscription  = parcel.readInt();

        mSubscriptionAddress = parcel.readInt();
        mType1               = parcel.readInt();
        mType2               = parcel.readInt();
        mUsed                = parcel.readInt();
        mRest                = parcel.readInt();
    }

    public static String getSubscriptionName(int subscription) {
        if (sSubscriptions.containsKey(subscription)) {
            return sSubscriptions.get(subscription);
        }
        return "Unknown Subscription (0x" + Long.toString(subscription, 16) + ")";
    }

    @Override public int getId() {
        return mId;
    }

    @Override public Date getValidFrom() {
        if (mValidFromTime != 0)
            return OVChipTransitData.convertDate((int) mValidFromDate, (int) mValidFromTime);
        else
        	return OVChipTransitData.convertDate((int) mValidFromDate);
    }

    @Override public Date getValidTo() {
    	if (mValidToTime != 0)
            return OVChipTransitData.convertDate((int) mValidToDate, (int) mValidToTime);
        else
        	return OVChipTransitData.convertDate((int) mValidToDate);
    }

    @Override public String getSubscriptionName() {
        return getSubscriptionName(mSubscription);
    }

    @Override public String getActivation() {
        if (mType1 != 0) {
            return mUsed != 0 ? "Activated and used" : "Activated but not used";
        }
        return "Deactivated";
    }

    @Override public int getMachineId () {
        return mMachineId;
    }

    @Override public String getAgencyName () {
        return OVChipTransitData.getShortAgencyName(mAgency);    // Nobody uses most of the long names
    }

    @Override public String getShortAgencyName () {
        return OVChipTransitData.getShortAgencyName(mAgency);
    }

    public int getUnknown1 () {
        return mUnknown1;
    }

    public int getUnknown2 () {
        return mUnknown2;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mId);
        parcel.writeInt(mUnknown1);
        parcel.writeLong(mValidFromDate);
        parcel.writeLong(mValidFromTime);
        parcel.writeLong(mValidToDate);
        parcel.writeLong(mValidToTime);
        parcel.writeInt(mUnknown2);
        parcel.writeInt(mAgency);
        parcel.writeInt(mMachineId);
        parcel.writeInt(mSubscription);
        parcel.writeInt(mSubscriptionAddress);
        parcel.writeInt(mType1);
        parcel.writeInt(mType2);
        parcel.writeInt(mUsed);
        parcel.writeInt(mRest);
    }
}