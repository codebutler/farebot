/*
 * ManlyFastFerryRecord.java
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

package com.codebutler.farebot.transit.manly_fast_ferry.record;

/**
 * Represents a record inside of a Manly Fast Ferry
 */
public class ManlyFastFerryRecord {

    ManlyFastFerryRecord() {
    }

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
                break;
            default:
                // Unknown record type
                break;
        }

        return record;
    }

}
