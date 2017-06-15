/*
 * ManlyFastFerryTransitFactory.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.manly_fast_ferry;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.DataClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryBalanceRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryMetadataRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPreambleRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRegularRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;

public class ManlyFastFerryTransitFactory implements TransitFactory<ClassicCard, ManlyFastFerryTransitInfo> {

    public static final byte[] SIGNATURE = {
            0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01
    };

    @Override
    public boolean check(@NonNull ClassicCard card) {
        // TODO: Improve this check
        // The card contains two copies of the card's serial number on the card.
        // Lets use this for now to check that this is a Manly Fast Ferry card.
        byte[] file1; //, file2;

        if (!(card.getSector(0) instanceof DataClassicSector)) {
            return false;
        }

        try {
            file1 = ((DataClassicSector) card.getSector(0)).getBlock(1).getData().bytes();
            //file2 = card.getSector(0).getBlock(2).bytes();
        } catch (UnauthorizedException ex) {
            // These blocks of the card are not protected.
            // This must not be a Manly Fast Ferry smartcard.
            return false;
        }

        // Serial number is from byte 10 in file 1 and byte 7 of file 2, for 4 bytes.
        // DISABLED: This check fails on 2012-era cards.
        //if (!Arrays.equals(Arrays.copyOfRange(file1, 10, 14), Arrays.copyOfRange(file2, 7, 11))) {
        //    return false;
        //}

        // Check a signature
        return Arrays.equals(Arrays.copyOfRange(file1, 0, SIGNATURE.length), SIGNATURE);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull ClassicCard card) {
        byte[] file2 = ((DataClassicSector) card.getSector(0)).getBlock(2).getData().bytes();
        ManlyFastFerryRecord metadata = recordFromBytes(file2);
        if (!(metadata instanceof ManlyFastFerryMetadataRecord)) {
            throw new AssertionError("Unexpected Manly record type: " + metadata.getClass().toString());
        }
        return TransitIdentity.create(
                ManlyFastFerryTransitInfo.NAME,
                ((ManlyFastFerryMetadataRecord) metadata).getCardSerial());
    }

    @NonNull
    @Override
    public ManlyFastFerryTransitInfo parseInfo(@NonNull ClassicCard card) {
        ArrayList<ManlyFastFerryRecord> records = new ArrayList<>();

        // Iterate through blocks on the card and deserialize all the binary data.
        for (ClassicSector sector : card.getSectors()) {
            if (!(sector instanceof DataClassicSector)) {
                continue;
            }
            for (ClassicBlock block : ((DataClassicSector) sector).getBlocks()) {
                if (sector.getIndex() == 0 && block.getIndex() == 0) {
                    continue;
                }

                if (block.getIndex() == 3) {
                    continue;
                }

                ManlyFastFerryRecord record = recordFromBytes(block.getData().bytes());
                if (record != null) {
                    records.add(record);
                }
            }
        }

        // Now do a first pass for metadata and balance information.
        ArrayList<ManlyFastFerryBalanceRecord> balances = new ArrayList<>();

        String serialNumber = null;
        GregorianCalendar epochDate = null;

        for (ManlyFastFerryRecord record : records) {
            if (record instanceof ManlyFastFerryMetadataRecord) {
                serialNumber = ((ManlyFastFerryMetadataRecord) record).getCardSerial();
                epochDate = ((ManlyFastFerryMetadataRecord) record).getEpochDate();
            } else if (record instanceof ManlyFastFerryBalanceRecord) {
                balances.add((ManlyFastFerryBalanceRecord) record);
            }
        }

        int balance = 0;

        if (balances.size() >= 1) {
            Collections.sort(balances);
            balance = balances.get(0).getBalance();
        }

        // Now generate a transaction list.
        // These need the Epoch to be known first.
        ArrayList<Trip> trips = new ArrayList<>();
        ArrayList<Refill> refills = new ArrayList<>();

        for (ManlyFastFerryRecord record : records) {
            if (record instanceof ManlyFastFerryPurseRecord) {
                ManlyFastFerryPurseRecord purseRecord = (ManlyFastFerryPurseRecord) record;

                // Now convert this.
                if (purseRecord.getIsCredit()) {
                    // Credit
                    refills.add(ManlyFastFerryRefill.create(purseRecord, epochDate));
                } else {
                    // Debit
                    trips.add(ManlyFastFerryTrip.create(purseRecord, epochDate));
                }
            }
        }

        Collections.sort(trips, new Trip.Comparator());
        Collections.sort(refills, new Refill.Comparator());

        return ManlyFastFerryTransitInfo.create(serialNumber, trips, refills, epochDate, balance);
    }

    @NonNull
    private static ManlyFastFerryRecord recordFromBytes(byte[] input) {
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
