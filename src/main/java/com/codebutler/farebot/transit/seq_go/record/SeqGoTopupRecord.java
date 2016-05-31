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

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.seq_go.SeqGoUtil;
import com.codebutler.farebot.util.Utils;

import java.util.GregorianCalendar;

/**
 * Top-up record type
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#top-up-record-type
 */
public class SeqGoTopupRecord extends SeqGoRecord implements Parcelable {

    public static final Creator<SeqGoTopupRecord> CREATOR = new Creator<SeqGoTopupRecord>() {
        @Override
        public SeqGoTopupRecord createFromParcel(Parcel source) {
            return new SeqGoTopupRecord(source);
        }

        @Override
        public SeqGoTopupRecord[] newArray(int size) {
            return new SeqGoTopupRecord[size];
        }
    };

    private GregorianCalendar mTimestamp;
    private int mCredit;
    private int mStation;
    private int mChecksum;
    private boolean mAutomatic;

    public static SeqGoTopupRecord recordFromBytes(byte[] input) {
        if ((input[0] != 0x01 && input[0] != 0x31) || input[1] != 0x01) {
            throw new AssertionError("Not a topup record");
        }

        SeqGoTopupRecord record = new SeqGoTopupRecord();

        byte[] ts = Utils.reverseBuffer(input, 2, 4);
        record.mTimestamp = SeqGoUtil.unpackDate(ts);

        byte[] credit = Utils.reverseBuffer(input, 6, 2);
        record.mCredit = Utils.byteArrayToInt(credit);

        byte[] station = Utils.reverseBuffer(input, 12, 2);
        record.mStation = Utils.byteArrayToInt(station);

        byte[] checksum = Utils.reverseBuffer(input, 14, 2);
        record.mChecksum = Utils.byteArrayToInt(checksum);

        record.mAutomatic = input[0] == 0x31;
        return record;
    }

    private SeqGoTopupRecord() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mTimestamp.getTimeInMillis());
        parcel.writeInt(mCredit);
        parcel.writeInt(mStation);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mAutomatic ? 1 : 0);
    }

    public SeqGoTopupRecord(Parcel parcel) {
        mTimestamp = new GregorianCalendar();
        mTimestamp.setTimeInMillis(parcel.readLong());
        mCredit = parcel.readInt();
        mStation = parcel.readInt();
        mChecksum = parcel.readInt();
        mAutomatic = parcel.readInt() == 1;
    }

    public GregorianCalendar getTimestamp() {
        return mTimestamp;
    }

    public int getCredit() {
        return mCredit;
    }

    public int getStation() {
        return mStation;
    }

    public int getChecksum() {
        return mChecksum;
    }

    public boolean getAutomatic() {
        return mAutomatic;
    }
}
