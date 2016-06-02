/*
 * OVChipTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.util.Date;

class OVChipTrip extends Trip {

    public static final Creator<OVChipTrip> CREATOR = new Creator<OVChipTrip>() {
        @Override
        public OVChipTrip createFromParcel(Parcel parcel) {
            return new OVChipTrip(parcel);
        }

        @Override
        public OVChipTrip[] newArray(int size) {
            return new OVChipTrip[size];
        }
    };

    static final java.util.Comparator<? super OVChipTrip> ID_ORDER = new java.util.Comparator<OVChipTrip>() {
        @Override
        public int compare(OVChipTrip t1, OVChipTrip t2) {
            return Integer.valueOf(t1.getId()).compareTo(t2.getId());
        }
    };

    private final int mId;
    private final int mProcessType;
    private final int mAgency;
    private final boolean mIsBus;
    private final boolean mIsTrain;
    private final boolean mIsMetro;
    private final boolean mIsFerry;
    private final boolean mIsOther;
    private final boolean mIsCharge;
    private final boolean mIsPurchase;
    private final boolean mIsBanned;
    private final Date mTimestamp;
    private final long mFare;
    private final Date mExitTimestamp;
    private final Station mStartStation;
    private final Station mEndStation;
    private final int mStartStationId;
    private final int mEndStationId;

    OVChipTrip(OVChipTransaction transaction) {
        this(transaction, null);
    }

    OVChipTrip(OVChipTransaction inTransaction, OVChipTransaction outTransaction) {
        mId = inTransaction.getId();

        mProcessType = inTransaction.getTransfer();
        mAgency = inTransaction.getCompany();

        mTimestamp = OVChipTransitData.convertDate(inTransaction.getDate(), inTransaction.getTime());

        mStartStationId = inTransaction.getStation();
        mStartStation = getStation(mAgency, mStartStationId);

        if (outTransaction != null) {
            mEndStationId = outTransaction.getStation();
            if (getStation(mAgency, outTransaction.getStation()) != null) {
                mEndStation = getStation(mAgency, outTransaction.getStation());
            } else {
                mEndStation = new Station(String.format("Unknown (%s)", mEndStationId), null, null);
            }
            mExitTimestamp = OVChipTransitData.convertDate(outTransaction.getDate(), outTransaction.getTime());
            mFare = outTransaction.getAmount();
        } else {
            mEndStation = null;
            mEndStationId = 0;
            mExitTimestamp = null;
            mFare = inTransaction.getAmount();
        }

        mIsTrain = (mAgency == OVChipTransitData.AGENCY_NS)
                || ((mAgency == OVChipTransitData.AGENCY_ARRIVA) && (mStartStationId < 800));

        // TODO: Needs verification!
        mIsMetro = (mAgency == OVChipTransitData.AGENCY_GVB && mStartStationId < 3000)
                || (mAgency == OVChipTransitData.AGENCY_RET && mStartStationId < 3000);

        mIsOther = mAgency == OVChipTransitData.AGENCY_TLS || mAgency == OVChipTransitData.AGENCY_DUO
                || mAgency == OVChipTransitData.AGENCY_STORE;

        // TODO: Needs verification!
        mIsFerry = mAgency == OVChipTransitData.AGENCY_ARRIVA && (mStartStationId > 4600 && mStartStationId < 4700);

        // FIXME: Clean this up
        //mIsBusOrTram = (mAgency == AGENCY_GVB || mAgency == AGENCY_HTM || mAgency == AGENCY_RET && (!mIsMetro));
        //mIsBusOrTrain = mAgency == AGENCY_VEOLIA || mAgency == AGENCY_SYNTUS;

        // Everything else will be a bus, although this is not correct.
        // The only way to determine them would be to collect every single 'ovcid' out there :(
        mIsBus = (!mIsTrain && !mIsMetro && !mIsOther && !mIsFerry);

        mIsCharge = (mProcessType == OVChipTransitData.PROCESS_CREDIT)
                || (mProcessType == OVChipTransitData.PROCESS_TRANSFER);

        // Not 100% sure about what NODATA is, but looks alright so far
        mIsPurchase = (mProcessType == OVChipTransitData.PROCESS_PURCHASE)
                || (mProcessType == OVChipTransitData.PROCESS_NODATA);

        mIsBanned = mProcessType == OVChipTransitData.PROCESS_BANNED;
    }

    private OVChipTrip(Parcel parcel) {
        mId = parcel.readInt();

        // mSame = (parcel.readInt() == 1);

        mProcessType = parcel.readInt();
        mAgency = parcel.readInt();

        mIsBus = (parcel.readInt() == 1);
        mIsTrain = (parcel.readInt() == 1);
        mIsMetro = (parcel.readInt() == 1);
        mIsFerry = (parcel.readInt() == 1);
        mIsOther = (parcel.readInt() == 1);
        mIsCharge = (parcel.readInt() == 1);
        mIsPurchase = (parcel.readInt() == 1);
        mIsBanned = (parcel.readInt() == 1);

        mFare = parcel.readLong();
        mTimestamp = new Date(parcel.readLong());

        if (parcel.readInt() == 1) {
            mExitTimestamp = new Date(parcel.readLong());
        } else {
            mExitTimestamp = null;
        }

        mStartStationId = parcel.readInt();
        if (parcel.readInt() == 1) {
            mStartStation = parcel.readParcelable(Station.class.getClassLoader());
        } else {
            mStartStation = null;
        }

        mEndStationId = parcel.readInt();
        if (parcel.readInt() == 1) {
            mEndStation = parcel.readParcelable(Station.class.getClassLoader());
        } else {
            mEndStation = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mId);

        // parcel.writeInt(mSame ? 1 : 0);

        parcel.writeInt(mProcessType);
        parcel.writeInt(mAgency);

        parcel.writeInt(mIsBus ? 1 : 0);
        parcel.writeInt(mIsTrain ? 1 : 0);
        parcel.writeInt(mIsMetro ? 1 : 0);
        parcel.writeInt(mIsFerry ? 1 : 0);
        parcel.writeInt(mIsOther ? 1 : 0);
        parcel.writeInt(mIsCharge ? 1 : 0);
        parcel.writeInt(mIsPurchase ? 1 : 0);
        parcel.writeInt(mIsBanned ? 1 : 0);

        parcel.writeLong(mFare);
        parcel.writeLong(mTimestamp.getTime());

        if (mExitTimestamp != null) {
            parcel.writeInt(1);
            parcel.writeLong(mExitTimestamp.getTime());
        } else {
            parcel.writeInt(0);
        }

        parcel.writeInt(mStartStationId);
        if (mStartStation != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(mStartStation, flags);
        } else {
            parcel.writeInt(0);
        }

        parcel.writeInt(mEndStationId);
        if (mEndStation != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(mEndStation, flags);
        } else {
            parcel.writeInt(0);
        }
    }

    private int getId() {
        return mId;
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        return OVChipTransitData.getShortAgencyName(mAgency);    // Nobody uses most of the long names
    }

    @Override
    public String getShortAgencyName() {
        return OVChipTransitData.getShortAgencyName(mAgency);
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        if (mStartStation != null && !TextUtils.isEmpty(mStartStation.getStationName())) {
            return mStartStation.getStationName();
        } else {
            return String.format("Unknown (%s)", mStartStationId);
        }
    }

    @Override
    public Station getStartStation() {
        return mStartStation;
    }

    @Override
    public String getEndStationName() {
        if (mEndStation != null && !TextUtils.isEmpty(mEndStation.getStationName())) {
            return mEndStation.getStationName();
        }
        return null;
    }

    @Override
    public Station getEndStation() {
        return mEndStation;
    }

    @Override
    public Mode getMode() {
        if (mIsBanned) {
            return Mode.BANNED;
        } else if (mIsCharge) {
            return Mode.TICKET_MACHINE;
        } else if (mIsPurchase) {
            return Mode.VENDING_MACHINE;
        } else if (mIsTrain) {
            return Mode.TRAIN;
        } else if (mIsBus) {
            return Mode.BUS;
        } else if (mIsMetro) {
            return Mode.METRO;
        } else if (mIsFerry) {
            return Mode.FERRY;
        } else if (mIsOther) {
            return Mode.OTHER;
        } else {
            return Mode.OTHER;
        }
    }

    @Override
    public long getTimestamp() {
        if (mTimestamp != null) {
            return mTimestamp.getTime() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public long getExitTimestamp() {
        if (mExitTimestamp != null) {
            return mExitTimestamp.getTime() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasTime() {
        return (mTimestamp != null);
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getFareString() {
        return OVChipTransitData.convertAmount((int) mFare);
    }

    private static Station getStation(int companyCode, int stationCode) {
        try {
            SQLiteDatabase db = FareBotApplication.getInstance().getOVChipDBUtil().openDatabase();
            Cursor cursor = db.query(
                    OVChipDBUtil.TABLE_NAME,
                    OVChipDBUtil.COLUMNS_STATIONDATA,
                    String.format("%s = ? AND %s = ?", OVChipDBUtil.COLUMN_ROW_COMPANY, OVChipDBUtil.COLUMN_ROW_OVCID),
                    new String[]{
                            String.valueOf(companyCode),
                            String.valueOf(stationCode)
                    },
                    null,
                    null,
                    OVChipDBUtil.COLUMN_ROW_OVCID);

            if (!cursor.moveToFirst()) {
                Log.w("OVChipTransitData", String.format("FAILED get rail company: c: 0x%s s: 0x%s",
                        Integer.toHexString(companyCode),
                        Integer.toHexString(stationCode)));

                return null;
            }

            String cityName = cursor.getString(cursor.getColumnIndex(OVChipDBUtil.COLUMN_ROW_CITY));
            String stationName = cursor.getString(cursor.getColumnIndex(OVChipDBUtil.COLUMN_ROW_NAME));
            String latitude = cursor.getString(cursor.getColumnIndex(OVChipDBUtil.COLUMN_ROW_LAT));
            String longitude = cursor.getString(cursor.getColumnIndex(OVChipDBUtil.COLUMN_ROW_LON));

            if (cityName != null) {
                stationName = cityName.concat(", " + stationName);
            }

            return new Station(stationName, latitude, longitude);
        } catch (Exception e) {
            Log.e("OVChipStationProvider", "Error in getStation", e);
            return null;
        }
    }
}
