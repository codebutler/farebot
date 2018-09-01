/*
 * KMTTrip.java
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
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
 */

package com.codebutler.farebot.transit.kmt;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.kmt.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

@AutoValue
abstract class KMTTrip extends Trip {

    @NonNull
    static KMTTrip create(FelicaBlock block) {
        byte[] data = block.getData().bytes();
        int processType = data[12];
        int sequenceNumber = Util.toInt(data[13], data[14], data[15]);
        Date timestampData = KMTUtil.extractDate(data);
        int transactionAmount = Util.toInt(data[4], data[5], data[6], data[7]);
        int endStationData = Util.toInt(data[8], data[9]);
        return new AutoValue_KMTTrip(processType, sequenceNumber, timestampData, transactionAmount, endStationData);
    }

    @Override
    public Mode getMode() {
        switch (getProcessType()) {
            case 0:
                return Mode.TICKET_MACHINE;
            case 1:
                return Mode.TRAIN;
            case 2:
                return Mode.POS;
            default:
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
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat format = NumberFormat.getCurrencyInstance(localeID);
        int tripFare = getTransactionAmount();
        if (getProcessType() == 1) {
            tripFare *= -1;
        }
        return format.format(tripFare);
    }

    @Override
    public String getBalanceString() {
        return "-";
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return resources.getString(R.string.kmt_agency_short);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return resources.getString(R.string.kmt_agency);
    }

    @Override
    public boolean hasTime() {
        return getTimestampData() != null;
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        return resources.getString(R.string.kmt_defroute);
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        return null;
    }

    @Override
    public Station getStartStation() {
        return null;
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        if (KMTData.getStation(getEndStationData()) != null) {
            return KMTData.getStation(getEndStationData()).getStationName();
        } else {
            return String.format("Unknown (0x%x)", getEndStationData());
        }
    }

    @Override
    public Station getEndStation() {
        return KMTData.getStation(getEndStationData());
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    abstract int getProcessType();

    abstract int getSequenceNumber();

    abstract Date getTimestampData();

    abstract int getTransactionAmount();

    abstract int getEndStationData();

}
