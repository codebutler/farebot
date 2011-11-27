/*
 * SuicaTransitData.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 *   Eric Butler <eric@codebutler.com>
 *   Kazzz
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

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.util.Log;
import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.felica.DBUtil;
import com.codebutler.farebot.felica.FelicaBlock;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.felica.FelicaService;
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.*;

import static com.codebutler.farebot.felica.DBUtil.*;

public class SuicaTransitData extends TransitData {
    private SuicaTrip[] mTrips;

    public static boolean check(FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_SUICA) != null);
    }

    public static TransitIdentity parseTransitIdentity (FelicaCard card) {
        return new TransitIdentity("Suica Card", null); // FIXME
    }

    public SuicaTransitData(FelicaCard card) {
        FelicaService service = card.getSystem(FeliCaLib.SYSTEMCODE_SUICA).getService(FeliCaLib.SERVICE_SUICA_HISTORY);

        long previousBalance = -1;

        List<SuicaTrip> trips = new ArrayList<SuicaTrip>();

        // Read blocks oldest-to-newest to calculate fare.
        FelicaBlock[] blocks = service.getBlocks();
        for (int i = (blocks.length - 1); i >= 0; i--) {
            FelicaBlock block = blocks[i];

            SuicaTrip trip = new SuicaTrip(block, previousBalance);
            previousBalance = trip.getBalance();

            if (trip.getTimestamp() == 0) {
                continue;
            }

            trips.add(trip);
        }
        
        Log.d("SuicaTransitData", "------------------------------------------------------");

        // Return trips in descending order.
        Collections.reverse(trips);
        
        mTrips = trips.toArray(new SuicaTrip[trips.size()]);
    }

    @Override
    public String getBalanceString() {
        if (mTrips.length > 0)
            return mTrips[0].getBalanceString();
            // FIXME: return mTrips[mTrips.length - 1].getBalanceString();
        return null;
    }

    @Override
    public String getSerialNumber() {
        // FIXME: Find where this is on the card.
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public String getCardName() {
        return "Suica Card"; // FIXME: Could be ICOCA, etc.
    }

    public static class SuicaTrip extends Trip {
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

            mTimestamp = extractDate(mIsProductSale, data);
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
                    mStartStation = getBusStop(mBusLineCode, mBusStopCode);

                } else {
                    mRailEntranceLineCode    = data[6] & 0xFF;
                    mRailEntranceStationCode = data[7] & 0xFF;
                    mRailExitLineCode        = data[8] & 0xFF;
                    mRailExitStationCode     = data[9] & 0xFF;
                    mStartStation = getRailStation(mRegionCode, getRailArea(), mRailEntranceLineCode, mRailEntranceStationCode);
                    mEndStation   = getRailStation(mRegionCode, getRailArea(), mRailExitLineCode, mRailExitStationCode);
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

        public SuicaTrip (Parcel parcel) {
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

        @Override
        public long getTimestamp() {
            if (mTimestamp != null)
                return mTimestamp.getTime() / 1000;
            else
                return 0;
        }

        public boolean hasTime() {
            return mIsProductSale;
        }

        @Override
        public String getRouteName() {
            return (mStartStation != null) ?  mStartStation.getLineName() : getProcessType();
        }

        @Override
        public String getAgencyName() {
            return (mStartStation != null) ? mStartStation.getCompanyName() : null;
        }

        @Override
        public String getShortAgencyName() {
            return getAgencyName();
        }

        @Override
        public double getFare() {
            return mFare;
        }

        @Override
        public String getFareString() {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
            format.setMaximumFractionDigits(0);
            if (mFare < 0) return "+" + format.format(-mFare);
            else return format.format(mFare);
        }

        public long getBalance() {
            return mBalance;
        }

        @Override
        public String getBalanceString() {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
            format.setMaximumFractionDigits(0);
            return format.format(mBalance);
        }

        @Override
        public String getStartStationName() {
            
//            if (mIsBus)
//                Log.d("SuicaTransitData",
//                    String.format("START BUS STATION NAME: %s REGION: 0x%s LINE: 0x%s STOP: 0x%s",
//                    ((mStartStation != null) ? mStartStation.getShortStationName(): null),
//                    Integer.toHexString(mRegionCode),
//                    Integer.toHexString(mBusLineCode),
//                    Integer.toHexString(mBusStopCode)));
//            else
//                Log.d("SuicaTransitData",
//                    String.format("START RAIL STATION NAME: %s REGION: 0x%s LINE: 0x%s STOP: 0x%s",
//                    ((mStartStation != null) ? mStartStation.getShortStationName(): null),
//                    Integer.toHexString(mRegionCode),
//                    Integer.toHexString(mRailEntranceLineCode),
//                    Integer.toHexString(mRailEntranceStationCode)));

            if (mIsProductSale || mIsCharge)
                return null;

            if (mStartStation != null) {
                return mStartStation.getShortStationName();
            }
            if (mIsBus) {
                return String.format("Bus Area 0x%x Line 0x%x Stop 0x%x", (byte) mRegionCode, (byte) mBusLineCode, (byte) mBusStopCode);
            } else {
                return String.format("Line 0x%x Station 0x%x", (byte) mRailEntranceLineCode, (byte) mRailEntranceStationCode);
            }
        }

        @Override
        public Station getStartStation() {
            return mStartStation;
        }

        @Override
        public String getEndStationName() {
            if (mIsProductSale || mIsCharge)
                return null;

            if (mEndStation != null) {
                return mEndStation.getShortStationName();
            }
            if (!mIsBus) {
                return String.format("Line %x Station %x", mRailExitLineCode, mRailExitStationCode);
            } 
            return null;
        }

        @Override
        public Station getEndStation() {
            return mEndStation;
        }

        @Override
        public Mode getMode() {
            if (mIsProductSale)
                return Mode.PRODUCTS;
            else if (mIsBus)
                return Mode.BUS;
            else
                return Mode.METRO;
        }

        public String getConsoleType() {
            return SuicaTransitData.getConsoleTypeName(mConsoleType);
        }
   
        public String getProcessType() {
            return SuicaTransitData.getProcessTypeName(mProcessType);
        }

        // Based on code from http://handasse.blogspot.com/2008/04/python-pasorisuica.html
        // This page has notes http://jennychan.web.fc2.com/format/suica.html#008B
        private int getRailArea () {
            if (mIsProductSale || mIsCharge || mIsBus)
                return -1;
            if (mRegionCode == 0) {
                if (mRailEntranceLineCode < 0x80)
                    return 0; // line JR.
                else
                    return 1; // Kanto public private railways
            } else {
                return 2; // Kansai Private Railways public
            }
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
    }

    private static Date extractDate(boolean isProductSale, byte[] data) {
        int date = Util.toInt(data[4], data[5]);
        if (date == 0)
            return null;
        int yy = date >> 9;
        int mm = (date >> 5) & 0xf;
        int dd = date & 0x1f;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2000 + yy);
        c.set(Calendar.MONTH, mm-1);
        c.set(Calendar.DAY_OF_MONTH, dd);

        // Product sales have time, too.
        // 物販だったら時間もセット
        if (isProductSale) {
            int time = Util.toInt(data[6], data[7]);
            int hh = time >> 11;
            int min = (time >> 5) & 0x3f;
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, min);
        } else {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
        }
        return c.getTime();
    }

    /**
     * 機器種別を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     * @param cType コンソールタイプをセット
     * @return String 機器タイプが文字列で戻ります
     */
    private static String getConsoleTypeName(int cType) {
        Application app = FareBotApplication.getInstance();
        switch (cType & 0xff) {
            case 0x03: return app.getString(R.string.felica_terminal_fare_adjustment);
            case 0x04: return app.getString(R.string.felica_terminal_portable);
            case 0x05: return app.getString(R.string.felica_terminal_vehicle); // bus
            case 0x07: return app.getString(R.string.felica_terminal_ticket);
            case 0x08: return app.getString(R.string.felica_terminal_ticket);
            case 0x09: return app.getString(R.string.felica_terminal_deposit_quick_charge);
            case 0x12: return app.getString(R.string.felica_terminal_tvm_tokyo_monorail);
            case 0x13: return app.getString(R.string.felica_terminal_tvm_etc);
            case 0x14: return app.getString(R.string.felica_terminal_tvm_etc);
            case 0x15: return app.getString(R.string.felica_terminal_tvm_etc);
            case 0x16: return app.getString(R.string.felica_terminal_ticket_gate);
            case 0x17: return app.getString(R.string.felica_terminal_simple_ticket_gate);
            case 0x18: return app.getString(R.string.felica_terminal_booth);
            case 0x19: return app.getString(R.string.felica_terminal_booth_green);
            case 0x1a: return app.getString(R.string.felica_terminal_ticket_gate_terminal);
            case 0x1b: return app.getString(R.string.felica_terminal_mobile_phone);
            case 0x1c: return app.getString(R.string.felica_terminal_connection_adjustment);
            case 0x1d: return app.getString(R.string.felica_terminal_transfer_adjustment);
            case 0x1f: return app.getString(R.string.felica_terminal_simple_deposit);
            case 0x46: return "VIEW ALTTE";
            case 0x48: return "VIEW ALTTE";
            case 0xc7: return app.getString(R.string.felica_terminal_pos);  // sales
            case 0xc8: return app.getString(R.string.felica_terminal_vending);   // sales
            default:
                return String.format("Console 0x%x", cType);
        }
    }
    
    /**
     * 処理種別を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     * @param proc 処理タイプをセット
     * @return String 処理タイプが文字列で戻ります
     */
    private static String getProcessTypeName(int proc) {
        Application app = FareBotApplication.getInstance();
        switch (proc & 0xff) {
            case 0x01: return app.getString(R.string.felica_process_fare_exit_gate);
            case 0x02: return app.getString(R.string.felica_process_charge);
            case 0x03: return app.getString(R.string.felica_process_purchase_magnetic);
            case 0x04: return app.getString(R.string.felica_process_fare_adjustment); // FIXME: Just "Payment" ?
            case 0x05: return app.getString(R.string.felica_process_admission_payment);
            case 0x06: return app.getString(R.string.felica_process_booth_exit);
            case 0x07: return app.getString(R.string.felica_process_issue_new);
            case 0x08: return app.getString(R.string.felica_process_booth_deduction);
            case 0x0d: return app.getString(R.string.felica_process_bus_pitapa);                    // Bus
            case 0x0f: return app.getString(R.string.felica_process_bus_iruca);                     // Bus
            case 0x11: return app.getString(R.string.felica_process_reissue);
            case 0x13: return app.getString(R.string.felica_process_payment_shinkansen);
            case 0x14: return app.getString(R.string.felica_process_entry_a_autocharge);
            case 0x15: return app.getString(R.string.felica_process_exit_a_autocharge);
            case 0x1f: return app.getString(R.string.felica_process_deposit_bus);                   // Bus
            case 0x23: return app.getString(R.string.felica_process_purchase_special_ticket);       // Bus
            case 0x46: return app.getString(R.string.felica_process_merchandise_purchase);          // Sales
            case 0x48: return app.getString(R.string.felica_process_bonus_charge);
            case 0x49: return app.getString(R.string.felica_process_register_deposit);              // Sales
            case 0x4a: return app.getString(R.string.felica_process_merchandise_cancel);            // Sales
            case 0x4b: return app.getString(R.string.felica_process_merchandise_admission);         // Sales
            case 0xc6: return app.getString(R.string.felica_process_merchandise_purchase_cash);     // Sales
            case 0xcb: return app.getString(R.string.felica_process_merchandise_admission_cash);    // Sales
            case 0x84: return app.getString(R.string.felica_process_payment_thirdparty);
            case 0x85: return app.getString(R.string.felica_process_admission_thirdparty);
            default:
                return String.format("Process0x%x", proc);
        }
    }
    
    /**
     * パス停留所を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     * @param lineCode 線区コードをセット
     * @param stationCode 駅順コードをセット
     * @return 取得できた場合、序数0に会社名、1停留所名が戻ります
     */
    private static Station getBusStop(int lineCode, int stationCode) {
        DBUtil util = new DBUtil(FareBotApplication.getInstance());
        try {
            SQLiteDatabase db = util.openDataBase();
            Cursor cursor = db.query(TABLE_IRUCA_STATIONCODE,
                COLUMNS_IRUCA_STATIONCODE,
                String.format("%s = ? AND %s = ?", COLUMN_LINECODE, COLUMN_STATIONCODE),
                new String[] { String.valueOf(lineCode), String.valueOf(stationCode) },
                null,
                null,
                COLUMN_ID);

            if (!cursor.moveToFirst()) {
                return null;
            }

            // FIXME: Figure out a better way to deal with i18n.
            boolean isJa = Locale.getDefault().getLanguage().equals("ja");
            String companyName = cursor.getString(cursor.getColumnIndex(isJa ? COLUMN_COMPANYNAME : COLUMN_COMPANYNAME_EN));
            String stationName = cursor.getString(cursor.getColumnIndex(isJa ? COLUMN_STATIONNAME : COLUMN_STATIONNAME_EN));
            return new Station(companyName, null, stationName, null, null, null);

        } catch (Exception e) {
            Log.e("SuicaStationProvider", "getBusStop() error", e);
            return null;
        } finally {
            util.close();
        }
    }

    /**
     *  地区コード、線区コード、駅順コードから駅名を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param regionCode 地区コードをセット
     * @param lineCode 線区コードをセット
     * @param stationCode 駅順コードをセット
     * @return 取得できた場合、序数0に会社名、1に路線名、2に駅名が戻ります
     */
    private static Station getRailStation(int regionCode, int areaCode, int lineCode, int stationCode) {
        areaCode = (regionCode >> 6);

        DBUtil util = new DBUtil(FareBotApplication.getInstance());
        try {
            SQLiteDatabase db = util.openDataBase();
            Cursor cursor = db.query(
                 TABLE_STATIONCODE,
                 COLUMNS_STATIONCODE,
                 String.format("%s = ? AND %s = ? AND %s = ?", COLUMN_AREACODE, COLUMN_LINECODE, COLUMN_STATIONCODE),
                 new String[] {
                     String.valueOf(areaCode & 0xFF),
                     String.valueOf(lineCode & 0xFF),
                     String.valueOf(stationCode & 0xFF)
                 },
                 null,
                 null,
                 COLUMN_ID);

            if (!cursor.moveToFirst()) {
                Log.d("SuicaTransitData", String.format("FAILED get rail company: r: 0x%s a: 0x%s l: 0x%s s: 0x%s",
                    Integer.toHexString(regionCode),
                    Integer.toHexString(areaCode),
                    Integer.toHexString(lineCode),
                    Integer.toHexString(stationCode)));

                return null;
            }
            
            Log.d("SuicaTransitData", String.format("Got rail company: %s %s, line: %s %s, station: %s %s [ r: 0x%s a: 0x%s l: 0x%s s: 0x%s ]",
                cursor.getString(cursor.getColumnIndex(COLUMN_COMPANYNAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_COMPANYNAME_EN)),

                cursor.getString(cursor.getColumnIndex(COLUMN_LINENAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_LINENAME_EN)),

                cursor.getString(cursor.getColumnIndex(COLUMN_STATIONNAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_STATIONNAME_EN)),

                Integer.toHexString(regionCode),
                Integer.toHexString(areaCode),
                Integer.toHexString(lineCode),
                Integer.toHexString(stationCode)));

            // FIXME: Figure out a better way to deal with i18n.
            boolean isJa = Locale.getDefault().getLanguage().equals("ja");
            String companyName = cursor.getString(cursor.getColumnIndex(isJa ? COLUMN_COMPANYNAME : COLUMN_COMPANYNAME_EN));
            String lineName    = cursor.getString(cursor.getColumnIndex(isJa ? COLUMN_LINENAME    : COLUMN_LINENAME_EN));
            String stationName = cursor.getString(cursor.getColumnIndex(isJa ? COLUMN_STATIONNAME : COLUMN_STATIONNAME_EN));
            String latitude    = cursor.getString(cursor.getColumnIndex(COLUMN_LATITUDE));
            String longitude   = cursor.getString(cursor.getColumnIndex(COLUMN_LONGITUDE));
            return new Station(companyName, lineName, stationName, null, latitude, longitude);

        } catch (Exception e) {
            Log.e("SuicaStationProvider", "Error in getRailStation", e);
            return null;
        } finally {
            util.close();
        }
    }
}
