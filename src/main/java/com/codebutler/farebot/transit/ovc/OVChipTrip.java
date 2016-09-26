/*
 * OVChipTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.util.Date;

@AutoValue
abstract class OVChipTrip extends Trip {

    static final java.util.Comparator<? super OVChipTrip> ID_ORDER = new java.util.Comparator<OVChipTrip>() {
        @Override
        public int compare(OVChipTrip t1, OVChipTrip t2) {
            return Integer.valueOf(t1.getId()).compareTo(t2.getId());
        }
    };

    @NonNull
    static OVChipTrip create(OVChipTransaction transaction) {
        return create(transaction, null);
    }

    @NonNull
    static OVChipTrip create(OVChipTransaction inTransaction, OVChipTransaction outTransaction) {
        int id = inTransaction.getId();

        int processType = inTransaction.getTransfer();
        int agency = inTransaction.getCompany();

        Date timestamp = OVChipTransitData.convertDate(inTransaction.getDate(), inTransaction.getTime());

        int startStationId = inTransaction.getStation();
        Station startStation = getStation(agency, startStationId);

        int endStationId;
        Station endStation;
        Date exitTimestamp;
        int fare;

        if (outTransaction != null) {
            endStationId = outTransaction.getStation();
            if (getStation(agency, outTransaction.getStation()) != null) {
                endStation = getStation(agency, outTransaction.getStation());
            } else {
                endStation = Station.builder()
                        .stationName(String.format("Unknown (%s)", endStationId))
                        .build();
            }
            exitTimestamp = OVChipTransitData.convertDate(outTransaction.getDate(), outTransaction.getTime());
            fare = outTransaction.getAmount();
        } else {
            endStation = null;
            endStationId = 0;
            exitTimestamp = null;
            fare = inTransaction.getAmount();
        }

        boolean isTrain = (agency == OVChipTransitData.AGENCY_NS)
                || ((agency == OVChipTransitData.AGENCY_ARRIVA) && (startStationId < 800));

        // TODO: Needs verification!
        boolean isMetro = (agency == OVChipTransitData.AGENCY_GVB && startStationId < 3000)
                || (agency == OVChipTransitData.AGENCY_RET && startStationId < 3000);

        boolean isOther = agency == OVChipTransitData.AGENCY_TLS || agency == OVChipTransitData.AGENCY_DUO
                || agency == OVChipTransitData.AGENCY_STORE;

        // TODO: Needs verification!
        boolean isFerry = agency == OVChipTransitData.AGENCY_ARRIVA && (startStationId > 4600 && startStationId < 4700);

        // FIXME: Clean this up
        //mIsBusOrTram = (agency == AGENCY_GVB || agency == AGENCY_HTM || agency == AGENCY_RET && (!isMetro));
        //mIsBusOrTrain = agency == AGENCY_VEOLIA || agency == AGENCY_SYNTUS;

        // Everything else will be a bus, although this is not correct.
        // The only way to determine them would be to collect every single 'ovcid' out there :(
        boolean isBus = (!isTrain && !isMetro && !isOther && !isFerry);

        boolean isCharge = (processType == OVChipTransitData.PROCESS_CREDIT)
                || (processType == OVChipTransitData.PROCESS_TRANSFER);

        // Not 100% sure about what NODATA is, but looks alright so far
        boolean isPurchase = (processType == OVChipTransitData.PROCESS_PURCHASE)
                || (processType == OVChipTransitData.PROCESS_NODATA);

        boolean isBanned = processType == OVChipTransitData.PROCESS_BANNED;

        return new AutoValue_OVChipTrip.Builder()
                .id(id)
                .processType(processType)
                .agency(agency)
                .isBus(isBus)
                .isTrain(isTrain)
                .isMetro(isMetro)
                .isFerry(isFerry)
                .isOther(isOther)
                .isCharge(isCharge)
                .isPurchase(isPurchase)
                .isBanned(isBanned)
                .timestampData(timestamp)
                .fare(fare)
                .exitTimestampData(exitTimestamp)
                .startStation(startStation)
                .endStation(endStation)
                .startStationId(startStationId)
                .endStationId(endStationId)
                .build();
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        return OVChipTransitData.getShortAgencyName(getAgency());    // Nobody uses most of the long names
    }

    @Override
    public String getShortAgencyName() {
        return OVChipTransitData.getShortAgencyName(getAgency());
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        Station startStation = getStartStation();
        if (startStation != null && !TextUtils.isEmpty(startStation.getStationName())) {
            return startStation.getStationName();
        } else {
            return String.format("Unknown (%s)", getStartStationId());
        }
    }

    @Override
    public String getEndStationName() {
        Station endStation = getEndStation();
        if (endStation != null && !TextUtils.isEmpty(endStation.getStationName())) {
            return endStation.getStationName();
        } else {
            return String.format("Unknown (%s)", getEndStationId());
        }
    }

    @Override
    public Mode getMode() {
        if (getIsBanned()) {
            return Mode.BANNED;
        } else if (getIsCharge()) {
            return Mode.TICKET_MACHINE;
        } else if (getIsPurchase()) {
            return Mode.VENDING_MACHINE;
        } else if (getIsTrain()) {
            return Mode.TRAIN;
        } else if (getIsBus()) {
            return Mode.BUS;
        } else if (getIsMetro()) {
            return Mode.METRO;
        } else if (getIsFerry()) {
            return Mode.FERRY;
        } else if (getIsOther()) {
            return Mode.OTHER;
        } else {
            return Mode.OTHER;
        }
    }

    @Override
    public long getTimestamp() {
        if (getTimestampData() != null) {
            return getTimestampData().getTime() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public long getExitTimestamp() {
        if (getExitTimestampData() != null) {
            return getExitTimestampData().getTime() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasTime() {
        return (getTimestampData() != null);
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getFareString() {
        return OVChipTransitData.convertAmount((int) getFare());
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

            return Station.builder()
                    .stationName(stationName)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        } catch (Exception e) {
            Log.e("OVChipStationProvider", "Error in getStation", e);
            return null;
        }
    }

    abstract int getId();

    abstract int getProcessType();

    abstract int getAgency();

    abstract boolean getIsBus();

    abstract boolean getIsTrain();

    abstract boolean getIsMetro();

    abstract boolean getIsFerry();

    abstract boolean getIsOther();

    abstract boolean getIsCharge();

    abstract boolean getIsPurchase();

    abstract boolean getIsBanned();

    abstract Date getTimestampData();

    abstract long getFare();

    abstract Date getExitTimestampData();

    public abstract Station getStartStation();

    public abstract Station getEndStation();

    abstract int getStartStationId();

    abstract int getEndStationId();

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder id(int id);

        abstract Builder processType(int processType);

        abstract Builder agency(int agency);

        abstract Builder isBus(boolean isBus);

        abstract Builder isTrain(boolean isTrain);

        abstract Builder isMetro(boolean isMetro);

        abstract Builder isFerry(boolean isFerry);

        abstract Builder isOther(boolean isOther);

        abstract Builder isCharge(boolean isCharge);

        abstract Builder isPurchase(boolean isPurchase);

        abstract Builder isBanned(boolean isBanned);

        abstract Builder timestampData(Date timestamp);

        abstract Builder fare(long fare);

        abstract Builder exitTimestampData(Date exitTimestamp);

        abstract Builder startStation(Station startStation);

        abstract Builder endStation(Station endStation);

        abstract Builder startStationId(int startStationId);

        abstract Builder endStationId(int endStationId);

        abstract OVChipTrip build();
    }
}
