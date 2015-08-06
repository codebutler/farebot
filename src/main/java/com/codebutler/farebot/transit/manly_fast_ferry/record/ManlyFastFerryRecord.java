package com.codebutler.farebot.transit.manly_fast_ferry.record;

/**
 * Represents a record inside of a Manly Fast Ferry
 */
public class ManlyFastFerryRecord {

    protected ManlyFastFerryRecord() {}

    public static ManlyFastFerryRecord recordFromBytes(byte[] input) {
        ManlyFastFerryRecord record = null;
        switch (input[0]) {
            case 0x01:
                // Check if the next bytes are null
                if (input[1] == 0x00 || input[1] == 0x01) {
                    if (input[2] != 0x00) {
                        // Fork off to handle balance
                        record = ManlyFastFerryBalanceRecord.recordFromBytes(input);
                    }
                }
                break;

            case 0x02:
                // Regular record
                record = ManlyFastFerryRegularRecord.recordFromBytes(input);
                break;

            case 0x32:
                // Preamble record
                record = ManlyFastFerryPreambleRecord.recordFromBytes(input);
                break;

            case 0x00:
            case 0x06:
                // Null record / ignorable record
            default:
                // Unknown record type
                break;
        }

        return record;
    }

}
