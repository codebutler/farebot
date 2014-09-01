package com.codebutler.farebot.transit.suica;

import android.os.Parcel;

import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import net.kazzz.felica.lib.Util;

import org.apache.commons.lang3.ArrayUtils;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class SuicaTrip extends Trip {
    private final long mBalance;

    private final int mConsoleType;
    private final int mProcessType;

    private final boolean mIsProductSale;
    private final boolean mIsBus;

    private final boolean mIsCharge;

    private final long mFare;
    private final Date mTimestamp;
    private final int  mRegionCode;

    private int mRailEntranceLineCode;
    private int mRailEntranceStationCode;
    private int mRailExitLineCode;
    private int mRailExitStationCode;

    private int mBusLineCode;
    private int mBusStopCode;

    private Station mStartStation;
    private Station mEndStation;

    public SuicaTrip(FelicaBlock block, long previousBalance) {
        byte[] data = block.getData();

        // 00000080000000000000000000000000
        // 00 00 - console type
        // 01 00 - process type
        // 02 00 - ??
        // 03 80 - ??
        // 04 00 - date
        // 05 00 - date
        // 06 00 - enter line code
        // 07 00
        // 08 00
        // 09 00
        // 10 00
        // 11 00
        // 12 00
        // 13 00
        // 14 00
        // 15 00


        mConsoleType = data[0];
        mProcessType = data[1];

        mIsBus         = mConsoleType == (byte) 0x05;
        mIsProductSale = (mConsoleType == (byte) 0xc7 || mConsoleType == (byte) 0xc8);
        mIsCharge = (mProcessType == (byte) 0x02);

        mTimestamp = SuicaUtil.extractDate(mIsProductSale, data);
        mBalance   = (long) Util.toInt(data[11], data[10]);

        mRegionCode = data[15] & 0xFF;

        if (previousBalance >= 0) {
            mFare = (previousBalance - mBalance);
        } else {
            // Can't get amount for first record.
            mFare = 0;
        }

        // Unused block (new card)
        if (mTimestamp == null) {
            return;
        }

        if (!mIsProductSale && !mIsCharge) {
            if (mIsBus) {
                mBusLineCode  = Util.toInt(data[6], data[7]);
                mBusStopCode  = Util.toInt(data[8], data[9]);
                mStartStation = SuicaUtil.getBusStop(mRegionCode, mBusLineCode, mBusStopCode);

            } else {
                mRailEntranceLineCode    = data[6] & 0xFF;
                mRailEntranceStationCode = data[7] & 0xFF;
                mRailExitLineCode        = data[8] & 0xFF;
                mRailExitStationCode     = data[9] & 0xFF;
                mStartStation = SuicaUtil.getRailStation(mRegionCode, mRailEntranceLineCode, mRailEntranceStationCode);
                mEndStation   = SuicaUtil.getRailStation(mRegionCode, mRailExitLineCode, mRailExitStationCode);
            }
        }
    }

    public static Creator<SuicaTrip> CREATOR = new Creator<SuicaTrip>() {
        public SuicaTrip createFromParcel(Parcel parcel) {
            return new SuicaTrip(parcel);
        }

        public SuicaTrip[] newArray(int size) {
            return new SuicaTrip[size];
        }
    };

    public SuicaTrip(Parcel parcel) {
        mBalance = parcel.readLong();

        mConsoleType = parcel.readInt();
        mProcessType = parcel.readInt();

        mIsProductSale = (parcel.readInt() == 1);
        mIsBus         = (parcel.readInt() == 1);

        mIsCharge = (parcel.readInt() == 1);

        mFare       = parcel.readLong();
        mTimestamp  = new Date(parcel.readLong());
        mRegionCode = parcel.readInt();

        mRailEntranceLineCode    = parcel.readInt();
        mRailEntranceStationCode = parcel.readInt();
        mRailExitLineCode        = parcel.readInt();
        mRailExitStationCode     = parcel.readInt();

        mBusLineCode = parcel.readInt();
        mBusStopCode = parcel.readInt();

        if (parcel.readInt() == 1)
            mStartStation = parcel.readParcelable(Station.class.getClassLoader());
        if (parcel.readInt() == 1)
            mEndStation = parcel.readParcelable(Station.class.getClassLoader());
    }

    @Override public long getTimestamp() {
        if (mTimestamp != null)
            return mTimestamp.getTime() / 1000;
        else
            return 0;
    }

    @Override public long getExitTimestamp() {
        return 0;
    }

    public boolean hasTime() {
        return mIsProductSale;
    }

    @Override public String getRouteName() {
        return (mStartStation != null) ?  mStartStation.getLineName() : (getConsoleType() + " " + getProcessType());
    }

    @Override public String getAgencyName() {
        return (mStartStation != null) ? mStartStation.getCompanyName() : null;
    }

    @Override public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override public double getFare() {
        return mFare;
    }

    @Override public String getFareString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        if (mFare < 0) return "+" + format.format(-mFare);
        else return format.format(mFare);
    }

    public long getBalance() {
        return mBalance;
    }

    @Override public String getBalanceString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(mBalance);
    }

    @Override public String getStartStationName() {
        if (mIsProductSale || mIsCharge)
            return null;

        if (mStartStation != null) {
            return mStartStation.getShortStationName();
        }
        if (mIsBus) {
            return String.format("Bus Area 0x%s Line 0x%s Stop 0x%s", Integer.toHexString(mRegionCode), Integer.toHexString(mBusLineCode), Integer.toHexString(mBusStopCode));
        } else if (!(mRailEntranceLineCode == 0 && mRailEntranceStationCode == 0)) {
            return String.format("Line 0x%s Station 0x%s", Integer.toHexString(mRailEntranceLineCode), Integer.toHexString(mRailEntranceStationCode));
        } else {
            return null;
        }
    }

    @Override public Station getStartStation() {
        return mStartStation;
    }

    @Override public String getEndStationName() {
        if (mIsProductSale || mIsCharge || isTVM())
            return null;

        if (mEndStation != null) {
            return mEndStation.getShortStationName();
        }
        if (!mIsBus) {
            return String.format("Line 0x%s Station 0x%s", Integer.toHexString(mRailExitLineCode), Integer.toHexString(mRailExitStationCode));
        }
        return null;
    }

    @Override public Station getEndStation() {
        return mEndStation;
    }

    @Override public Mode getMode() {
        int consoleType = mConsoleType & 0xFF;
        if (isTVM()) {
            return Mode.TICKET_MACHINE;
        } else if (consoleType == 0xc8) {
            return Mode.VENDING_MACHINE;
        } else if (consoleType == 0xc7) {
            return Mode.POS;
        } else if (mIsBus) {
            return Mode.BUS;
        } else {
            return Mode.METRO;
        }
    }

    public String getConsoleType() {
        return SuicaUtil.getConsoleTypeName(mConsoleType);
    }

    public String getProcessType() {
        return SuicaUtil.getProcessTypeName(mProcessType);
    }

    /*
    public boolean isBus() {
        return mIsBus;
    }

    public boolean isProductSale() {
        return mIsProductSale;
    }

    public boolean isCharge() {
        return mIsCharge;
    }

    public int getRegionCode() {
        return mRegionCode;
    }

    public int getBusLineCode() {
        return mBusLineCode;
    }

    public int getBusStopCode() {
        return mBusStopCode;
    }

    public int getRailEntranceLineCode() {
        return mRailEntranceLineCode;
    }

    public int getRailEntranceStationCode() {
        return mRailEntranceStationCode;
    }

    public int getRailExitLineCode() {
        return mRailExitLineCode;
    }

    public int getRailExitStationCode() {
        return mRailExitStationCode;
    }
    */

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mBalance);

        parcel.writeInt(mConsoleType);
        parcel.writeInt(mProcessType);

        parcel.writeInt(mIsProductSale ? 1 : 0);
        parcel.writeInt(mIsBus ? 1 : 0);

        parcel.writeInt(mIsCharge ? 1 : 0);

        parcel.writeLong(mFare);
        parcel.writeLong(mTimestamp.getTime());
        parcel.writeInt(mRegionCode);

        parcel.writeInt(mRailEntranceLineCode);
        parcel.writeInt(mRailEntranceStationCode);
        parcel.writeInt(mRailExitLineCode);
        parcel.writeInt(mRailExitStationCode);

        parcel.writeInt(mBusLineCode);
        parcel.writeInt(mBusStopCode);

        if (mStartStation != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(mStartStation, flags);
        } else {
            parcel.writeInt(0);
        }

        if (mEndStation != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(mEndStation, flags);
        } else {
            parcel.writeInt(0);
        }
    }

    public int describeContents() {
        return 0;
    }

    private boolean isTVM() {
        int consoleType = mConsoleType & 0xFF;
        int[] tvmConsoleTypes = { 0x03, 0x07, 0x08, 0x12, 0x13, 0x14, 0x15 };
        return ArrayUtils.contains(tvmConsoleTypes, consoleType);
    }
}
