package com.codebutler.farebot.transit.manly_fast_ferry.record;

import com.codebutler.farebot.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryBalanceRecord extends ManlyFastFerryRecord implements Comparable<ManlyFastFerryBalanceRecord> {
    private int mBalance;
    private int mVersion;

    public static ManlyFastFerryBalanceRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x01) throw new AssertionError();


        ManlyFastFerryBalanceRecord record = new ManlyFastFerryBalanceRecord();
        record.mVersion = Utils.byteArrayToInt(input, 2, 1);
        record.mBalance = Utils.byteArrayToInt(input, 11, 4);

        return record;
    }

    protected ManlyFastFerryBalanceRecord() {}

    /**
     * The balance of the card, in cents.
     * @return int number of cents.
     */
    public int getBalance() {
        return mBalance;
    }
    public int getVersion() { return mVersion; }

    @Override
    public int compareTo(ManlyFastFerryBalanceRecord rhs) {
        // So sorting works, we reverse the order so highest number is first.
        return Integer.valueOf(rhs.mVersion).compareTo(this.mVersion);
    }
}
