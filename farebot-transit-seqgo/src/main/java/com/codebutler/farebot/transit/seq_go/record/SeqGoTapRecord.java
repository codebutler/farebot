/*
 * SeqGoTapRecord.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.seq_go.record;

import androidx.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.SeqGoData;
import com.codebutler.farebot.transit.seq_go.SeqGoUtil;
import com.google.auto.value.AutoValue;

import java.util.GregorianCalendar;

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#tap-record-type
 */
@AutoValue
public abstract class SeqGoTapRecord extends SeqGoRecord implements Comparable<SeqGoTapRecord> {

    @NonNull
    public static SeqGoTapRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x31) {
            throw new AssertionError("not a tap record");
        }

        int mode = ByteUtils.byteArrayToInt(input, 1, 1);
        GregorianCalendar timestamp = SeqGoUtil.unpackDate(ByteUtils.reverseBuffer(input, 2, 4));
        int journey = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 5, 2)) >> 3;
        int station = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 12, 2));
        int checksum = ByteUtils.byteArrayToInt(ByteUtils.reverseBuffer(input, 14, 2));

        return new AutoValue_SeqGoTapRecord(mode, timestamp, journey, station, checksum);
    }

    @NonNull
    public Trip.Mode getMode() {
        if (SeqGoData.VEHICLES.containsKey(getModeData())) {
            return SeqGoData.VEHICLES.get(getModeData());
        } else {
            return Trip.Mode.OTHER;
        }
    }

    @Override
    public int compareTo(@NonNull SeqGoTapRecord rhs) {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.
        if (rhs.getJourney() == this.getJourney()) {
            return this.getTimestamp().compareTo(rhs.getTimestamp());
        } else {
            return integerCompare(this.getJourney(), rhs.getJourney());
        }
    }

    private static int integerCompare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    abstract int getModeData();

    public abstract GregorianCalendar getTimestamp();

    public abstract int getJourney();

    public abstract int getStation();

    public abstract int getChecksum();
}
