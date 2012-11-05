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

package com.codebutler.farebot.transit;

import android.os.Parcel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OVChipSubscription extends Subscription {
    private final int mId;
    private final long mValidFrom;
    private final long mValidTo;
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
        int validFrom = 0;
        int validTo = 0;
        int machineId = 0;

        if (data[0] == (byte)0x0A && data[1] == (byte)0x00 && data[2] == (byte)0xE0 && ((data[3] & (byte)0xF0) == (byte)0x00)) {
            id           = ((data[9] & (byte)0xFF) << 4) | ((data[10] >> 4) & (byte)0x0F);
            company      = ((data[4] >> 4) & (byte) 0x0F);
            subscription = (((char)data[4] & (char)0x0F) << 12) | (((char)data[5] & (char)0xFF) << 4) | (((char)data[6] >> 4) & (char)0x0F);
            validFrom    = (((char)data[11] & (char)0x07) << 11) | (((char)data[12] & (char)0xFF) << 3) | (((char)data[13] >> 5) & (char)0x07);
            validTo      = (((char)data[13] & (char)0x1F) << 9) | (((char)data[14] & (char)0xFF) << 1) | (((char)data[15] >> 7) & (char)0x01);
            machineId    = (((char)data[21] & (char)0x03) << 22) | (((char)data[22] & (char)0xFF) << 14) | (((char)data[23] & (char)0xFF) << 6) | (((char)data[24] >> 2) & (char)0x3F);
        } else if (data[0] == (byte)0x0A && data[1] == (byte)0x02 && data[2] == (byte)0xE0 && ((data[3] & (byte)0xF0) == (byte)0x00)) {
            id           = ((data[9] & (byte)0xFF) << 4) | ((data[10] >> 4) & (byte)0x0F);
            company      = ((data[4] >> 4) & (byte) 0x0F);
            subscription = (((char)data[4] & (char)0x0F) << 12) | (((char)data[5] & (char)0xFF) << 4) | (((char)data[6] >> 4) & (char)0x0F);
            validFrom    = ((char)(data[12] & (char)0x01) << 13) | (((char)data[13] & (char)0xFF) << 5) | (((char)data[14] >> 3) & (char)0x1F);

            if ((((data[11] & (byte)0x1F) << 7) | ((data[12] >> 1) & (byte)0x7F)) == 31) {
                validTo   = (((char)data[16] & (char)0xFF) << 6) | (((char)data[17] >> 2) & (char)0x3F);
                machineId = (((char)data[25] & (char)0x03) << 22) | (((char)data[26] & (char)0xFF) << 14) | (((char)data[27] & (char)0xFF) << 6) | (((char)data[28] >> 2) & (char)0x3F);
            }

            if ((((data[11] & (byte)0x1F) << 7) | ((data[12] >> 1) & (byte)0x7F)) == 21) {
                validTo   = (((char)data[14] & (char)0x07) << 11) | (((char)data[15] & (char)0xFF) << 3) | (((char)data[16] >> 5) & (char)0x07);
                machineId = (((char)data[23] & (char)0xFF) << 16) | (((char)data[24] & (char)0xFF) << 8) | (((char)data[25] & (char)0xFF));
            }
        } else {
            throw new IllegalArgumentException("Not valid");
        }

        mId           = id;
        mAgency       = company;
        mSubscription = subscription;
        mValidFrom    = validFrom;
        mValidTo      = validTo;
        mMachineId    = machineId;
    }


    public OVChipSubscription(Parcel parcel) {
        mId           = parcel.readInt();
        mValidFrom    = parcel.readLong();
        mValidTo      = parcel.readLong();
        mAgency       = parcel.readInt();
        mMachineId    = parcel.readInt();
        mSubscription = parcel.readInt();

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

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public Date getValidFrom() {
        return OVChipTransitData.convertDate((int) mValidFrom);
    }

    @Override
    public Date getValidTo() {
        return OVChipTransitData.convertDate((int) mValidTo);
    }

    @Override
    public String getSubscriptionName() {
        return getSubscriptionName(mSubscription);
    }

    @Override
    public int getMachineId () {
        return mMachineId;
    }

    @Override
    public String getAgencyName () {
        return OVChipTransitData.getShortAgencyName((int)mAgency);    // Nobody uses most of the long names
    }

    @Override
    public String getShortAgencyName () {
        return OVChipTransitData.getShortAgencyName((int)mAgency);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mId);
        parcel.writeLong(mValidFrom);
        parcel.writeLong(mValidTo);
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