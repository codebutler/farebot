package com.codebutler.farebot.transit.manly_fast_ferry.record;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryRegularRecord extends ManlyFastFerryRecord {

    public static ManlyFastFerryRegularRecord recordFromBytes(byte[] input) {
        ManlyFastFerryRegularRecord record = null;
        if (input[0] != 0x02) throw new AssertionError("Regular record must start with 0x02");

        switch (input[1]) {
            case 0x02:
                record = ManlyFastFerryPurseRecord.recordFromBytes(input);
                break;
            case 0x03:
                record = ManlyFastFerryMetadataRecord.recordFromBytes(input);
                break;
            default:
                // Unknown record type
                break;
        }

        return record;
    }
}
