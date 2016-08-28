/*
 * SeqGoTopupRecord.java
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

package com.codebutler.farebot.transit.seq_go.record;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.seq_go.SeqGoUtil;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

import java.util.GregorianCalendar;

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#top-up-record-type
 */
@AutoValue
public abstract class SeqGoTopupRecord extends SeqGoRecord implements Parcelable {

    @NonNull
    public static SeqGoTopupRecord recordFromBytes(byte[] input) {
        if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) {
            throw new AssertionError("Not a topup record");
        }

        GregorianCalendar timestamp = SeqGoUtil.unpackDate(Utils.reverseBuffer(input, 2, 4));
        int credit = Utils.byteArrayToInt(Utils.reverseBuffer(input, 6, 2));
        int station = Utils.byteArrayToInt(Utils.reverseBuffer(input, 12, 2));
        int checksum = Utils.byteArrayToInt(Utils.reverseBuffer(input, 14, 2));
        boolean automatic = input[0] == 0x31;
        return new AutoValue_SeqGoTopupRecord(timestamp, credit, station, checksum, automatic);
    }

    public abstract GregorianCalendar getTimestamp();

    public abstract int getCredit();

    public abstract int getStation();

    public abstract int getChecksum();

    public abstract boolean getAutomatic();
}
