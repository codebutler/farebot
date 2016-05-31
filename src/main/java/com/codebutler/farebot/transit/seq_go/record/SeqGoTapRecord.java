/*
 * SeqGoTapRecord.java
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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.SeqGoData;
import com.codebutler.farebot.transit.seq_go.SeqGoUtil;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#tap-record-type
 */
public class SeqGoTapRecord extends SeqGoRecord implements Parcelable, Comparable<SeqGoTapRecord> {

    public static final Creator<SeqGoTapRecord> CREATOR = new Creator<SeqGoTapRecord>() {
        @Override
        public SeqGoTapRecord createFromParcel(Parcel source) {
            return new SeqGoTapRecord(source);
        }

        @Override
        public SeqGoTapRecord[] newArray(int size) {
            return new SeqGoTapRecord[size];
        }
    };

    private GregorianCalendar mTimestamp;
    private int mMode;
    private int mJourney;
    private int mStation;
    private int mChecksum;

    public static SeqGoTapRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x31) {
            throw new AssertionError("not a tap record");
        }

        SeqGoTapRecord record = new SeqGoTapRecord();

        record.mMode = Utils.byteArrayToInt(input, 1, 1);

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = SeqGoUtil.unpackDate(ts);

        byte[] journey = Utils.reverseBuffer(input, 5, 2);
        record.mJourney = Utils.byteArrayToInt(journey) >> 3;

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        return record;
    }

    private SeqGoTapRecord() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp.getTimeInMillis());
        parcel.writeInt(mMode);
        parcel.writeInt(mJourney);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
    }

    private SeqGoTapRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mMode = parcel.readInt();
        mJourney = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
    }

    public Trip.Mode getMode() {
        if (SeqGoData.VEHICLES.containsKey(mMode)) {
            return SeqGoData.VEHICLES.get(mMode);
        } else {
            return Trip.Mode.OTHER;
        }
    }

    public GregorianCalendar getTimestamp() {
        return mTimestamp;
    }

    public int getJourney() {
        return mJourney;
    }

    public int getStation() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }

    @Override
    public int compareTo(@NonNull SeqGoTapRecord rhs) {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.
        if (rhs.mJourney == this.mJourney) {
            return this.mTimestamp.compareTo(rhs.mTimestamp);
        } else {
            return integerCompare(this.mJourney, rhs.mJourney);
        }
    }

    private static int integerCompare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }
}
