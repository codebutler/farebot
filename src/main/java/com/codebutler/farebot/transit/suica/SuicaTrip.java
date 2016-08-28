/*
 * SuicaTrip.java
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Thanks to these resources for providing additional information about the Suica format:
 * http://www.denno.net/SFCardFan/
 * http://jennychan.web.fc2.com/format/suica.html
 * http://d.hatena.ne.jp/baroqueworksdev/20110206/1297001722
 * http://handasse.blogspot.com/2008/04/python-pasorisuica.html
 * http://sourceforge.jp/projects/felicalib/wiki/suica
 *
 * Some of these resources have been translated into English at:
 * https://github.com/micolous/metrodroid/wiki/Suica
 */

package com.codebutler.farebot.transit.suica;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import net.kazzz.felica.lib.Util;

import org.apache.commons.lang3.ArrayUtils;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

@AutoValue
abstract class SuicaTrip extends Trip {

    @NonNull
    static SuicaTrip create(@NonNull FelicaBlock block, long previousBalance) {
        byte[] data = block.getData().bytes();

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

        int consoleType = data[0];
        int processType = data[1];

        boolean isBus = consoleType == (byte) 0x05;
        boolean isProductSale = (consoleType == (byte) 0xc7 || consoleType == (byte) 0xc8);
        boolean isCharge = (processType == (byte) 0x02);

        Date timestamp = SuicaUtil.extractDate(isProductSale, data);
        long balance = (long) Util.toInt(data[11], data[10]);

        int regionCode = data[15] & 0xFF;

        long fare;
        if (previousBalance >= 0) {
            fare = (previousBalance - balance);
        } else {
            // Can't get amount for first record.
            fare = 0;
        }

        int busLineCode = 0;
        int busStopCode = 0;
        int railEntranceLineCode = 0;
        int railEntranceStationCode = 0;
        int railExitLineCode = 0;
        int railExitStationCode = 0;
        Station startStation = null;
        Station endStation = null;

        if (timestamp == null) {
            // Unused block (new card)
        } else {
            if (!isProductSale && !isCharge) {
                if (isBus) {
                    busLineCode = Util.toInt(data[6], data[7]);
                    busStopCode = Util.toInt(data[8], data[9]);
                    startStation = SuicaUtil.getBusStop(regionCode, busLineCode, busStopCode);
                } else {
                    railEntranceLineCode = data[6] & 0xFF;
                    railEntranceStationCode = data[7] & 0xFF;
                    railExitLineCode = data[8] & 0xFF;
                    railExitStationCode = data[9] & 0xFF;
                    startStation = SuicaUtil.getRailStation(regionCode, railEntranceLineCode, railEntranceStationCode);
                    endStation = SuicaUtil.getRailStation(regionCode, railExitLineCode, railExitStationCode);
                }
            }
        }

        return new AutoValue_SuicaTrip.Builder()
                .balance(balance)
                .consoleType(consoleType)
                .processType(processType)
                .isProductSale(isProductSale)
                .isBus(isBus)
                .isCharge(isCharge)
                .fare(fare)
                .timestampData(timestamp)
                .regionCode(regionCode)
                .railEntranceLineCode(railEntranceLineCode)
                .railEntranceStationCode(railEntranceStationCode)
                .railExitLineCode(railExitLineCode)
                .railExitStationCode(railExitStationCode)
                .busLineCode(busLineCode)
                .busStopCode(busStopCode)
                .startStation(startStation)
                .endStation(endStation)
                .build();
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
        return 0;
    }

    @Override
    public boolean hasTime() {
        return getIsProductSale();
    }

    @Override
    public String getRouteName() {
        return (getStartStation() != null)
                ? getStartStation().getLineName() : (getConsoleTypeName() + " " + getProcessTypeName());
    }

    @Override
    public String getAgencyName() {
        return (getStartStation() != null) ? getStartStation().getCompanyName() : null;
    }

    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getFareString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        if (getFare() < 0) {
            return "+" + format.format(-getFare());
        } else {
            return format.format(getFare());
        }
    }

    @Override
    public String getBalanceString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(getBalance());
    }

    @Override
    public String getStartStationName() {
        if (getIsProductSale() || getIsCharge()) {
            return null;
        }

        if (getStartStation() != null) {
            return getStartStation().getDisplayStationName();
        }
        if (getIsBus()) {
            return String.format("Bus Area 0x%s Line 0x%s Stop 0x%s", Integer.toHexString(getRegionCode()),
                    Integer.toHexString(getBusLineCode()), Integer.toHexString(getBusStopCode()));
        } else if (!(getRailEntranceLineCode() == 0 && getRailEntranceStationCode() == 0)) {
            return String.format("Line 0x%s Station 0x%s", Integer.toHexString(getRailEntranceLineCode()),
                    Integer.toHexString(getRailEntranceStationCode()));
        } else {
            return null;
        }
    }

    @Override
    public String getEndStationName() {
        if (getIsProductSale() || getIsCharge() || isTVM()) {
            return null;
        }

        if (getEndStation() != null) {
            return getEndStation().getDisplayStationName();
        }
        if (!getIsBus()) {
            return String.format("Line 0x%s Station 0x%s", Integer.toHexString(getRailExitLineCode()),
                    Integer.toHexString(getRailExitStationCode()));
        }
        return null;
    }

    @Override
    public Mode getMode() {
        int consoleType = getConsoleType() & 0xFF;
        if (isTVM()) {
            return Mode.TICKET_MACHINE;
        } else if (consoleType == 0xc8) {
            return Mode.VENDING_MACHINE;
        } else if (consoleType == 0xc7) {
            return Mode.POS;
        } else if (getIsBus()) {
            return Mode.BUS;
        } else {
            return Mode.METRO;
        }
    }

    private String getConsoleTypeName() {
        return SuicaUtil.getConsoleTypeName(getConsoleType());
    }

    private String getProcessTypeName() {
        return SuicaUtil.getProcessTypeName(getProcessType());
    }

    private boolean isTVM() {
        int consoleType = getConsoleType() & 0xFF;
        int[] tvmConsoleTypes = {0x03, 0x07, 0x08, 0x12, 0x13, 0x14, 0x15};
        return ArrayUtils.contains(tvmConsoleTypes, consoleType);
    }

    abstract long getBalance();

    abstract int getConsoleType();

    abstract int getProcessType();

    abstract boolean getIsProductSale();

    abstract boolean getIsBus();

    abstract boolean getIsCharge();

    abstract long getFare();

    abstract Date getTimestampData();

    abstract int getRegionCode();

    abstract int getRailEntranceLineCode();

    abstract int getRailEntranceStationCode();

    abstract int getRailExitLineCode();

    abstract int getRailExitStationCode();

    abstract int getBusLineCode();

    abstract int getBusStopCode();

    public abstract Station getStartStation();

    public abstract Station getEndStation();

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder balance(long balance);

        abstract Builder consoleType(int consoleType);

        abstract Builder processType(int processType);

        abstract Builder isProductSale(boolean isProductSale);

        abstract Builder isBus(boolean isBus);

        abstract Builder isCharge(boolean isCharge);

        abstract Builder fare(long fare);

        abstract Builder timestampData(Date timestamp);

        abstract Builder regionCode(int regionCode);

        abstract Builder railEntranceLineCode(int railEntranceLineCode);

        abstract Builder railEntranceStationCode(int railEntranceStationCode);

        abstract Builder railExitLineCode(int railExitLineCode);

        abstract Builder railExitStationCode(int railExitStationCode);

        abstract Builder busLineCode(int busLineCode);

        abstract Builder busStopCode(int busStopCode);

        abstract Builder startStation(Station startStation);

        abstract Builder endStation(Station endStation);

        abstract SuicaTrip build();
    }
}
