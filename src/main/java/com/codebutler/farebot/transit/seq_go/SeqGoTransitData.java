/*
 * SeqGoTransitData.java
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.seq_go.record.SeqGoBalanceRecord;
import com.codebutler.farebot.transit.seq_go.record.SeqGoRecord;
import com.codebutler.farebot.transit.seq_go.record.SeqGoTapRecord;
import com.codebutler.farebot.transit.seq_go.record.SeqGoTopupRecord;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
@AutoValue
public abstract class SeqGoTransitData extends TransitData {

    public static final String NAME = "Go card";

    private static final byte[] MANUFACTURER = {
            0x16, 0x18, 0x1A, 0x1B,
            0x1C, 0x1D, 0x1E, 0x1F
    };

    @NonNull
    public static SeqGoTransitData create(@NonNull ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData().bytes();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        BigInteger serialNumber = Utils.byteArrayToBigInteger(serialData, 0, 4);

        ArrayList<SeqGoRecord> records = new ArrayList<>();

        for (ClassicSector sector : card.getSectors()) {
            for (ClassicBlock block : sector.getBlocks()) {
                if (sector.getIndex() == 0 && block.getIndex() == 0) {
                    continue;
                }

                if (block.getIndex() == 3) {
                    continue;
                }

                SeqGoRecord record = SeqGoRecord.recordFromBytes(block.getData().bytes());

                if (record != null) {
                    records.add(record);
                }
            }
        }

        // Now do a first pass for metadata and balance information.
        List<SeqGoBalanceRecord> balances = new ArrayList<>();
        List<SeqGoTrip> trips = new ArrayList<>();
        List<SeqGoRefill> refills = new ArrayList<>();
        List<SeqGoTapRecord> taps = new ArrayList<>();

        for (SeqGoRecord record : records) {
            if (record instanceof SeqGoBalanceRecord) {
                balances.add((SeqGoBalanceRecord) record);
            } else if (record instanceof SeqGoTopupRecord) {
                SeqGoTopupRecord topupRecord = (SeqGoTopupRecord) record;
                refills.add(SeqGoRefill.create(topupRecord));
            } else if (record instanceof SeqGoTapRecord) {
                taps.add((SeqGoTapRecord) record);
            }
        }

        int balance = 0;
        if (balances.size() >= 1) {
            Collections.sort(balances);
            balance = balances.get(0).getBalance();
        }

        if (taps.size() >= 1) {
            Collections.sort(taps);

            // Lets figure out the trips.
            int i = 0;

            while (taps.size() > i) {
                SeqGoTapRecord tapOn = taps.get(i);
                // Start by creating an empty trip
                SeqGoTrip.Builder tripBuilder = SeqGoTrip.builder();

                // Put in the metadatas
                tripBuilder.journeyId(tapOn.getJourney());
                tripBuilder.startTime(tapOn.getTimestamp());
                tripBuilder.startStationId(tapOn.getStation());
                tripBuilder.mode(tapOn.getMode());

                // Peek at the next record and see if it is part of
                // this journey
                if (taps.size() > i + 1 && taps.get(i + 1).getJourney() == tapOn.getJourney()
                        && taps.get(i + 1).getMode() == tapOn.getMode()) {
                    // There is a tap off.  Lets put that data in
                    SeqGoTapRecord tapOff = taps.get(i + 1);

                    tripBuilder.endTime(tapOff.getTimestamp());
                    tripBuilder.endStationId(tapOff.getStation());

                    // Increment to skip the next record
                    i++;
                } else {
                    // There is no tap off. Journey is probably in progress.
                }

                trips.add(tripBuilder.build());

                // Increment to go to the next record
                i++;
            }

            // Now sort the trips array
            Collections.sort(trips, new Trip.Comparator());
        }

        boolean hasUnknownStations = false;
        for (SeqGoTrip trip : trips) {
            if (trip.getStartStation() == null || (trip.getEndTime() != null && trip.getEndStation() == null)) {
                hasUnknownStations = true;
            }
        }

        if (refills.size() > 1) {
            Collections.sort(refills, new Refill.Comparator());
        }

        return new AutoValue_SeqGoTransitData(
                ImmutableList.<Trip>copyOf(trips),
                ImmutableList.<Refill>copyOf(refills),
                hasUnknownStations,
                serialNumber,
                balance);
    }

    public static boolean check(ClassicCard card) {
        try {
            byte[] blockData = card.getSector(0).getBlock(1).getData().bytes();
            return Arrays.equals(Arrays.copyOfRange(blockData, 1, 9), MANUFACTURER);
        } catch (UnauthorizedException ex) {
            // It is not possible to identify the card without a key
            return false;
        }
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        byte[] serialData = card.getSector(0).getBlock(0).getData().bytes();
        serialData = Utils.reverseBuffer(serialData, 0, 4);
        BigInteger serialNumber = Utils.byteArrayToBigInteger(serialData, 0, 4);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber));
    }

    private static String formatSerialNumber(BigInteger serialNumber) {
        String serial = serialNumber.toString();
        while (serial.length() < 12) {
            serial = "0" + serial;
        }

        serial = "016" + serial;
        return serial + Utils.calculateLuhn(serial);
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getBalance() / 100.);
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return formatSerialNumber(getSerialNumberData());
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @NonNull
    @Override
    public String getCardName() {
        return NAME;
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Override
    public abstract boolean hasUnknownStations();

    abstract BigInteger getSerialNumberData();

    abstract int getBalance();
}
