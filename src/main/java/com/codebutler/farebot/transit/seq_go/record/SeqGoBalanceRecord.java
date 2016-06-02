/*
 * SeqGoBalanceRecord.java
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

import com.codebutler.farebot.util.Utils;

/**
 * Represents balance records on Go card
 * https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29#balance-record-type
 */
public class SeqGoBalanceRecord extends SeqGoRecord implements Comparable<SeqGoBalanceRecord> {

    private int mVersion;
    private int mBalance;

    private SeqGoBalanceRecord() { }

    public static SeqGoBalanceRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x01) {
            throw new AssertionError();
        }


        SeqGoBalanceRecord record = new SeqGoBalanceRecord();
        record.mVersion = Utils.byteArrayToInt(input, 13, 1);

        // Do some flipping for the balance
        byte[] balance = Utils.reverseBuffer(input, 2, 2);
        record.mBalance = Utils.byteArrayToInt(balance, 0, 2);

        return record;
    }

    /**
     * The balance of the card, in cents.
     *
     * @return int number of cents.
     */
    public int getBalance() {
        return mBalance;
    }

    public int getVersion() {
        return mVersion;
    }

    @Override
    public int compareTo(SeqGoBalanceRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);
    }
}
