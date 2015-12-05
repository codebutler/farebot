package com.codebutler.farebot.transit.manly_fast_ferry.record;

import java.util.Arrays;

import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import com.codebutler.farebot.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryPreambleRecord extends ManlyFastFerryRecord {
    private String mCardSerial;
    static byte[] OLD_CARD_ID = {0x00, 0x00, 0x00};

    public static ManlyFastFerryPreambleRecord recordFromBytes(byte[] input) {
        ManlyFastFerryPreambleRecord record = new ManlyFastFerryPreambleRecord();

        // Check that the record is valid for a preamble
        if (!Arrays.equals(Arrays.copyOfRange(input, 0, ManlyFastFerryTransitData.SIGNATURE.length), ManlyFastFerryTransitData.SIGNATURE)) {
            throw new IllegalArgumentException("Preamble signature does not match");
        }

        // This is not set on 2012-era cards
        if (Arrays.equals(Arrays.copyOfRange(input, 10, 13), OLD_CARD_ID)) {
            record.mCardSerial = null;
        } else {
            record.mCardSerial = Utils.getHexString(Arrays.copyOfRange(input, 10, 14));
        }
        return record;
    }

    protected ManlyFastFerryPreambleRecord() {}

    /**
     * Returns the card serial number. Returns null on old cards.
     *
     */
    public String getCardSerial() { return mCardSerial; }

}
