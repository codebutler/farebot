/*
 * ManlyFastFerryMetadataRecord.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.manly_fast_ferry.record;

import com.codebutler.farebot.util.Utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryMetadataRecord extends ManlyFastFerryRegularRecord {

    private static final GregorianCalendar MANLY_BASE_EPOCH = new GregorianCalendar(2000, Calendar.JANUARY, 1);

    private String mCardSerial;
    private GregorianCalendar mEpochDate;

    private ManlyFastFerryMetadataRecord() { }

    public static ManlyFastFerryMetadataRecord recordFromBytes(byte[] input) {
        assert input[0] == 0x02;
        assert input[1] == 0x03;

        final int epochDays = Utils.byteArrayToInt(input, 5, 2);

        ManlyFastFerryMetadataRecord record = new ManlyFastFerryMetadataRecord();
        record.mCardSerial = Utils.getHexString(Arrays.copyOfRange(input, 7, 11));
        record.mEpochDate = new GregorianCalendar();
        record.mEpochDate.setTimeInMillis(MANLY_BASE_EPOCH.getTimeInMillis());
        record.mEpochDate.add(Calendar.DATE, epochDays);

        return record;
    }

    public String getCardSerial() {
        return mCardSerial;
    }

    public GregorianCalendar getEpochDate() {
        return mEpochDate;
    }
}
