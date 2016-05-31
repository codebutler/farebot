/*
 * SeqGoRefill.java
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

package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.seq_go.record.SeqGoTopupRecord;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Represents a top-up event on the Go card.
 */
public class SeqGoRefill extends Refill {
    private SeqGoTopupRecord mTopup;

    public SeqGoRefill(SeqGoTopupRecord topup) {
        mTopup = topup;
    }

    @Override
    public long getTimestamp() {
        return mTopup.getTimestamp().getTimeInMillis() / 1000;
    }

    @Override
    public String getAgencyName() {
        return null;
    }

    @Override
    public String getShortAgencyName() {
        return Utils.localizeString(mTopup.getAutomatic()
                ? R.string.seqgo_refill_automatic
                : R.string.seqgo_refill_manual);
    }

    @Override
    public long getAmount() {
        return mTopup.getCredit();
    }

    @Override
    public String getAmountString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getAmount() / 100);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        mTopup.writeToParcel(parcel, i);
    }

    private SeqGoRefill(Parcel parcel) {
        mTopup = new SeqGoTopupRecord(parcel);
    }

    public static final Parcelable.Creator<SeqGoRefill> CREATOR = new Parcelable.Creator<SeqGoRefill>() {

        @Override
        public SeqGoRefill createFromParcel(Parcel in) {
            return new SeqGoRefill(in);
        }

        @Override
        public SeqGoRefill[] newArray(int size) {
            return new SeqGoRefill[size];
        }
    };
}
