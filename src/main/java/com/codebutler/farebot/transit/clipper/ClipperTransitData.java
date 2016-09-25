/*
 * ClipperTransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

package com.codebutler.farebot.transit.clipper;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class ClipperTransitData extends TransitData {

    private static final int RECORD_LENGTH = 32;
    private static final long EPOCH_OFFSET = 0x83aa7f18;

    @NonNull
    public static ClipperTransitData parse(@NonNull DesfireCard desfireCard) {
        byte[] data;

        try {
            data = desfireCard.getApplication(0x9011f2).getFile(0x08).getData().bytes();
            long serialNumber = Utils.byteArrayToLong(data, 1, 4);

            data = desfireCard.getApplication(0x9011f2).getFile(0x02).getData().bytes();
            short balance = (short) (((0xFF & data[18]) << 8) | (0xFF & data[19]));

            List<ClipperRefill> refills = parseRefills(desfireCard);
            List<ClipperTrip> trips = computeBalances(balance, parseTrips(desfireCard), refills);

            return new AutoValue_ClipperTransitData(
                    Long.toString(serialNumber),
                    ImmutableList.<Trip>copyOf(trips),
                    ImmutableList.<Refill>copyOf(refills),
                    balance);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper data", ex);
        }
    }

    public static boolean check(@NonNull Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x9011f2) != null);
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0x9011f2).getFile(0x08).getData().bytes();
            return new TransitIdentity("Clipper", String.valueOf(Utils.byteArrayToLong(data, 1, 4)));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper serial", ex);
        }
    }

    @NonNull
    @Override
    public String getCardName() {
        return "Clipper";
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getBalance() / 100.0);
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @NonNull
    private static List<ClipperTrip> parseTrips(@NonNull DesfireCard card) {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x0e);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte[] data = file.getData().bytes();
        int pos = data.length - RECORD_LENGTH;
        List<ClipperTrip> result = new ArrayList<>();
        while (pos > 0) {
            byte[] slice = Utils.byteArraySlice(data, pos, RECORD_LENGTH);
            final ClipperTrip trip = createTrip(slice);
            if (trip != null) {
                // Some transaction types are temporary -- remove previous trip with the same timestamp.
                ClipperTrip existingTrip = Utils.findInList(result, new Utils.Matcher<ClipperTrip>() {
                    @Override
                    public boolean matches(ClipperTrip otherTrip) {
                        return trip.getTimestamp() == otherTrip.getTimestamp();
                    }
                });
                if (existingTrip != null) {
                    if (existingTrip.getExitTimestamp() != 0) {
                        // Old trip has exit timestamp, and is therefore better.
                        pos -= RECORD_LENGTH;
                        continue;
                    } else {
                        result.remove(existingTrip);
                    }
                }
                result.add(trip);
            }
            pos -= RECORD_LENGTH;
        }

        Collections.sort(result, new Trip.Comparator());

        return result;
    }

    private static ClipperTrip createTrip(byte[] useData) {
        // Use a magic number to offset the timestamp
        final long timestamp = Utils.byteArrayToLong(useData, 0xc, 4) - EPOCH_OFFSET;
        final long exitTimestamp = Utils.byteArrayToLong(useData, 0x10, 4);
        final long fare = Utils.byteArrayToLong(useData, 0x6, 2);
        final long agency = Utils.byteArrayToLong(useData, 0x2, 2);
        final long from = Utils.byteArrayToLong(useData, 0x14, 2);
        final long to = Utils.byteArrayToLong(useData, 0x16, 2);
        final long route = Utils.byteArrayToLong(useData, 0x1c, 2);

        if (agency == 0) {
            return null;
        }

        return ClipperTrip.builder()
                .timestamp(timestamp)
                .exitTimestamp(exitTimestamp)
                .fare(fare)
                .agency(agency)
                .from(from)
                .to(to)
                .route(route)
                .balance(0) // Filled in later
                .build();
    }

    @NonNull
    private static List<ClipperRefill> parseRefills(@NonNull DesfireCard card) {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x04);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte[] data = file.getData().bytes();
        int pos = data.length - RECORD_LENGTH;
        List<ClipperRefill> result = new ArrayList<>();
        while (pos > 0) {
            byte[] slice = Utils.byteArraySlice(data, pos, RECORD_LENGTH);
            ClipperRefill refill = createRefill(slice);
            if (refill != null) {
                result.add(refill);
            }
            pos -= RECORD_LENGTH;
        }
        Collections.sort(result, new Comparator<ClipperRefill>() {
            @Override
            public int compare(ClipperRefill r, ClipperRefill r1) {
                return Long.valueOf(r1.getTimestamp()).compareTo(r.getTimestamp());
            }
        });
        return result;
    }

    private static ClipperRefill createRefill(byte[] useData) {
        final long timestamp = Utils.byteArrayToLong(useData, 0x4, 4);
        final long agency = Utils.byteArrayToLong(useData, 0x2, 2);
        final long machineid = Utils.byteArrayToLong(useData, 0x8, 4);
        final long amount = Utils.byteArrayToLong(useData, 0xe, 2);

        if (timestamp == 0) {
            return null;
        }

        return ClipperRefill.create(timestamp - EPOCH_OFFSET, amount, agency, machineid);
    }

    @NonNull
    private static List<ClipperTrip> computeBalances(
            long balance,
            @NonNull List<ClipperTrip> trips,
            @NonNull List<ClipperRefill> refills) {
        List<ClipperTrip> tripsWithBalance = new ArrayList<>(Collections.nCopies(trips.size(), (ClipperTrip) null));
        int tripIdx = 0;
        int refillIdx = 0;
        while (tripIdx < trips.size()) {
            while (refillIdx < refills.size()
                    && refills.get(refillIdx).getTimestamp() > trips.get(tripIdx).getTimestamp()) {
                balance -= refills.get(refillIdx).getAmount();
                refillIdx++;
            }
            tripsWithBalance.set(tripIdx, ClipperTrip.builder(trips.get(tripIdx))
                    .balance(balance)
                    .build());
            balance += trips.get(tripIdx).getFare();
            tripIdx++;
        }
        return tripsWithBalance;
    }

    public static String getAgencyName(int agency) {
        if (ClipperData.AGENCIES.containsKey(agency)) {
            return ClipperData.AGENCIES.get(agency);
        }
        return FareBotApplication.getInstance().getString(R.string.unknown_format, "0x" + Long.toString(agency, 16));
    }

    public static String getShortAgencyName(int agency) {
        if (ClipperData.SHORT_AGENCIES.containsKey(agency)) {
            return ClipperData.SHORT_AGENCIES.get(agency);
        }
        return FareBotApplication.getInstance().getString(R.string.unknown_format, "0x" + Long.toString(agency, 16));
    }

    abstract short getBalance();
}
