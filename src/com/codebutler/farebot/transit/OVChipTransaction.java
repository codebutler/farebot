/*
 * OVChipTransaction.java
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

import java.util.Comparator;

public class OVChipTransaction implements Parcelable {
    public static final Comparator<OVChipTransaction> ID_ORDER = new Comparator<OVChipTransaction>() {
        public int compare(OVChipTransaction t1, OVChipTransaction t2) {
            return (t1.getId() < t2.getId() ? -1 : (t1.getId() == t2.getId() ? 0 : 1));
        }
    };

    private final int mTransactionSlot;
    private final int mDate;
    private final int mTime;
    private final int mTransfer;
    private final int mCompany;
    private final int mId;
    private final int mStation;
    private final int mMachineId;
    private final int mVehicleId;
    private final int mProductId;
    private final int mAmount;
    private final int mSubscriptionId;
    private final int mValid;
    private final int mUnknownConstant;
    private final int mUnknownConstant2;
    private final String mErrorMessage;

    public OVChipTransaction (int transactionSlot, String errorMessage) {
        mTransactionSlot  = transactionSlot;
        mErrorMessage     = errorMessage;
        mDate             =  0;
        mTime             =  0;
        mTransfer         = -3; // Default: No-data
        mCompany          =  0;
        mId               =  0;
        mStation          =  0;
        mMachineId        =  0;
        mVehicleId        =  0;
        mProductId        =  0;
        mAmount           =  0;
        mSubscriptionId   = -1; // Default: No valid subscriptionId
        mValid            =  0;
        mUnknownConstant  =  0;
        mUnknownConstant2 =  0;
    }

    public OVChipTransaction (
            int        transactionSlot,
            int        date,
            int        time,
            int        transfer,
            int        company,
            int        id,
            int        station,
            int        machineId,
            int        vehicleId,
            int        productId,
            int        amount,
            int        subscriptionId,
            int        valid,
            int        unknownConstant,
            int        unknownConstant2
    ) {
        mTransactionSlot = transactionSlot;
        mErrorMessage = "";
        mDate = date;
        mTime = time;
        mTransfer = transfer;
        mCompany = company;
        mId = id;
        mStation = station;
        mMachineId = machineId;
        mVehicleId = vehicleId;
        mProductId = productId;
        mAmount = amount;
        mSubscriptionId = subscriptionId;
        mValid = valid;
        mUnknownConstant = unknownConstant;
        mUnknownConstant2 = unknownConstant2;
    }

    public OVChipTransaction (int transactionSlot, byte[] data) {
        if (data == null) {
            data = new byte[32];
        }

        int valid = 1;
        int date = 0;
        int time = 0;
        int unknownConstant = 0;
        int transfer = -3; // Default: No-data
        int company = 0;
        int id = 0;
        int station = 0;
        int machineId = 0;
        int vehicleId = 0;
        int productId = 0;
        int unknownConstant2 = 0;
        int amount = 0;
        int subscriptionId = -1; // Default: No valid subscriptionId
        String errorMessage = "";

        if (data[0] == (byte)0x00 && data[1] == (byte)0x00 && data[2] == (byte)0x00 && (data[3] & (byte)0xF0) == (byte)0x00) valid = 0;
        if ((data[3] & (byte)0x10) != (byte)0x00) valid = 0;
        if ((data[3] & (byte)0x80) != (byte)0x00) valid = 0;
        if ((data[2] & (byte)0x02) != (byte)0x00) valid = 0;
        if ((data[2] & (byte)0x08) != (byte)0x00) valid = 0;
        if ((data[2] & (byte)0x20) != (byte)0x00) valid = 0;
        if ((data[2] & (byte)0x80) != (byte)0x00) valid = 0;
        if ((data[1] & (byte)0x01) != (byte)0x00) valid = 0;
        if ((data[1] & (byte)0x02) != (byte)0x00) valid = 0;
        if ((data[1] & (byte)0x08) != (byte)0x00) valid = 0;
        if ((data[1] & (byte)0x20) != (byte)0x00) valid = 0;
        if ((data[1] & (byte)0x40) != (byte)0x00) valid = 0;
        if ((data[1] & (byte)0x80) != (byte)0x00) valid = 0;
        if ((data[0] & (byte)0x02) != (byte)0x00) valid = 0;
        if ((data[0] & (byte)0x04) != (byte)0x00) valid = 0;

        if (valid == 0) {
            errorMessage = "No transaction";
        } else {
            int iBitOffset = 53; // Ident, Date, Time

            date = (((char)data[3] & (char)0x0F) << 10) | (((char)data[4] & (char)0xFF) << 2) | (((char)data[5] >> 6) & (char)0x03);
            time = (((char)data[5] & (char)0x3F) << 5) | (((char)data[6] >> 3) & (char)0x1F);

            if ((data[3] & (byte)0x20) != (byte)0x00) {
                unknownConstant = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((data[3] & (byte)0x40) != (byte)0x00) {
                transfer = Utils.getBitsFromBuffer(data, iBitOffset, 7);
                iBitOffset += 7;
            }

            if ((data[2] & (byte)0x01) != (byte)0x00) {
                company = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[2] & (byte)0x04) != (byte)0x00) {
                id = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((data[2] & (byte)0x10) != (byte)0x00) {
                station = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[2] & (byte)0x40) != (byte)0x00) {
                machineId = Utils.getBitsFromBuffer(data, iBitOffset, 24);
                iBitOffset += 24;
            }

            if ((data[1] & (byte)0x04) != (byte)0x00) {
                vehicleId = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[1] & (byte)0x10) != (byte)0x00) {
                productId = Utils.getBitsFromBuffer(data, iBitOffset, 5);
                iBitOffset += 5;
            }

            if ((data[0] & (byte)0x01) != (byte)0x00) {
                unknownConstant2 = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[0] & (byte)0x08) != (byte)0x00) {
                amount = Utils.getBitsFromBuffer(data, iBitOffset, 16);
                iBitOffset += 16;
            }

            if ((data[1] & (byte)0x10) == (byte)0x00) {
                subscriptionId = Utils.getBitsFromBuffer(data, iBitOffset, 13);
            }
        }

        mDate = date;
        mTime = time;
        mTransfer = transfer;
        mCompany = company;
        mId = id;
        mStation = station;
        mMachineId = machineId;
        mVehicleId = vehicleId;
        mProductId = productId;
        mAmount = amount;
        mSubscriptionId = subscriptionId;
        mValid = valid;
        mUnknownConstant = unknownConstant;
        mUnknownConstant2 = unknownConstant2;

        mTransactionSlot = transactionSlot;
        mErrorMessage = errorMessage;
    }

    public int getTransactionSlot() {
        return mTransactionSlot;
    }

    public int getDate() {
        return mDate;
    }

    public int getTime() {
        return mTime;
    }

    public int getTransfer() {
        return mTransfer;
    }

    public int getCompany() {
        return mCompany;
    }

    public int getId() {
        return mId;
    }

    public int getStation() {
        return mStation;
    }

    public int getMachineId() {
        return mMachineId;
    }

    public int getVehicleId() {
        return mVehicleId;
    }

    public int getProductId() {
        return mProductId;
    }

    public int getAmount() {
        return mAmount;
    }

    public int getSubscriptionId() {
        return mSubscriptionId;
    }

    public int getValid() {
        return mValid;
    }

    public int getUnknownConstant() {
        return mUnknownConstant;
    }

    public int getUnknownConstant2() {
        return mUnknownConstant2;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<OVChipTransaction> CREATOR = new Parcelable.Creator<OVChipTransaction>() {
        public OVChipTransaction createFromParcel(Parcel source) {
            int transactionSlot;
            int    date;
            int    time;
            int    transfer;
            int    company;
            int    id;
            int    station;
            int    machineId;
            int    vehicleId;
            int    productId;
            int    amount;
            int    subscriptionId;
            int    valid;
            int    unknownConstant;
            int    unknownConstant2;

            transactionSlot = source.readInt();
            valid = source.readInt();

            if (valid == 0) {
                return new OVChipTransaction(transactionSlot, source.readString());
            }

            date             = source.readInt();
            time             = source.readInt();
            transfer         = source.readInt();
            company          = source.readInt();
            id               = source.readInt();
            station          = source.readInt();
            machineId        = source.readInt();
            vehicleId        = source.readInt();
            productId        = source.readInt();
            amount           = source.readInt();
            subscriptionId   = source.readInt();
            unknownConstant  = source.readInt();
            unknownConstant2 = source.readInt();

            return new OVChipTransaction(transactionSlot,
                    date, time, transfer, company, id, station,
                    machineId, vehicleId, productId, amount,
                    subscriptionId, valid, unknownConstant,
                    unknownConstant2);
        }

        public OVChipTransaction[] newArray (int size) {
            return new OVChipTransaction[size];
        }
    };

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTransactionSlot);
        parcel.writeInt(mValid);
        if (mValid == 0) {
            parcel.writeString(mErrorMessage);
        } else {
            parcel.writeInt(mDate);
            parcel.writeInt(mTime);
            parcel.writeInt(mTransfer);
            parcel.writeInt(mCompany);
            parcel.writeInt(mId);
            parcel.writeInt(mStation);
            parcel.writeInt(mMachineId);
            parcel.writeInt(mVehicleId);
            parcel.writeInt(mProductId);
            parcel.writeInt(mAmount);
            parcel.writeInt(mSubscriptionId);
            parcel.writeInt(mUnknownConstant);
            parcel.writeInt(mUnknownConstant2);
        }
    }

    public boolean isSameTrip(OVChipTransaction nextTransaction) {
        /*
         * Information about checking in and out:
         * http://www.chipinfo.nl/inchecken/
         */

        if (mCompany == nextTransaction.getCompany() && mTransfer == OVChipTransitData.PROCESS_CHECKIN && nextTransaction.getTransfer() == OVChipTransitData.PROCESS_CHECKOUT) {
            if (mDate == nextTransaction.getDate()) {
                return true;
            } else if (mDate == nextTransaction.getDate() - 1) {
                // All NS trips get reset at 4 AM (except if it's a night train, but that's out of our scope).
                if (mCompany == OVChipTransitData.AGENCY_NS && nextTransaction.getTime() < 240) {
                    return true;
                }

                /*
                 * Some companies expect a checkout at the maximum of 15 minutes after the estimated arrival at the endstation of the line.
                 * But it's hard to determine the length of every single trip there is, so for now let's just assume a checkout at the next
                 * day is still from the same trip. Better solutions are always welcome ;)
                 */
                if (mCompany != OVChipTransitData.AGENCY_NS) {
                    return true;
                }
            }
        }

        return false;
    }
}