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

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ClipperTransitData extends TransitData {

    public static final Creator<ClipperTransitData> CREATOR = new Creator<ClipperTransitData>() {
        @Override
        public ClipperTransitData createFromParcel(Parcel parcel) {
            return new ClipperTransitData(parcel);
        }

        @Override
        public ClipperTransitData[] newArray(int size) {
            return new ClipperTransitData[size];
        }
    };

    private static final int RECORD_LENGTH = 32;
    private static final long EPOCH_OFFSET = 0x83aa7f18;

    private final long mSerialNumber;
    private final short mBalance;
    private final ClipperTrip[] mTrips;
    private final ClipperRefill[] mRefills;

    private ClipperTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
        mBalance = (short) parcel.readLong();

        mTrips = new ClipperTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, ClipperTrip.CREATOR);

        mRefills = new ClipperRefill[parcel.readInt()];
        parcel.readTypedArray(mRefills, ClipperRefill.CREATOR);
    }

    public ClipperTransitData(Card card) {
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data;

        try {
            data = desfireCard.getApplication(0x9011f2).getFile(0x08).getData();
            mSerialNumber = Utils.byteArrayToLong(data, 1, 4);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper serial", ex);
        }

        try {
            data = desfireCard.getApplication(0x9011f2).getFile(0x02).getData();
            mBalance = (short) (((0xFF & data[18]) << 8) | (0xFF & data[19]));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper balance", ex);
        }

        try {
            mTrips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper trips", ex);
        }

        try {
            mRefills = parseRefills(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper refills", ex);
        }

        setBalances();
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x9011f2) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0x9011f2).getFile(0x08).getData();
            return new TransitIdentity("Clipper", String.valueOf(Utils.byteArrayToLong(data, 1, 4)));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper serial", ex);
        }
    }

    @Override
    public String getCardName() {
        return "Clipper";
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(mBalance / 100.0);
    }

    @Override
    public String getSerialNumber() {
        return Long.toString(mSerialNumber);
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public ClipperRefill[] getRefills() {
        return mRefills;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    private ClipperTrip[] parseTrips(DesfireCard card) {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x0e);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte[] data = file.getData();
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
        ClipperTrip[] useLog = new ClipperTrip[result.size()];
        result.toArray(useLog);

        Arrays.sort(useLog, new Trip.Comparator());

        return useLog;
    }

    private ClipperTrip createTrip(byte[] useData) {
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

        return new ClipperTrip(timestamp, exitTimestamp, fare, agency, from, to, route);
    }

    private ClipperRefill[] parseRefills(DesfireCard card) {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x04);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte[] data = file.getData();
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
        ClipperRefill[] useLog = new ClipperRefill[result.size()];
        useLog = result.toArray(useLog);
        Arrays.sort(useLog, new Comparator<ClipperRefill>() {
            @Override
            public int compare(ClipperRefill r, ClipperRefill r1) {
                return Long.valueOf(r1.getTimestamp()).compareTo(r.getTimestamp());
            }
        });
        return useLog;
    }

    private ClipperRefill createRefill(byte[] useData) {
        final long timestamp = Utils.byteArrayToLong(useData, 0x4, 4);
        final long agency = Utils.byteArrayToLong(useData, 0x2, 2);
        final long machineid = Utils.byteArrayToLong(useData, 0x8, 4);
        final long amount = Utils.byteArrayToLong(useData, 0xe, 2);

        if (timestamp == 0) {
            return null;
        }

        return new ClipperRefill(timestamp - EPOCH_OFFSET, amount, agency, machineid);
    }

    private void setBalances() {
        int tripIdx = 0;
        int refillIdx = 0;
        long balance = (long) mBalance;

        while (tripIdx < mTrips.length) {
            while (refillIdx < mRefills.length && mRefills[refillIdx].getTimestamp() > mTrips[tripIdx].getTimestamp()) {
                balance -= mRefills[refillIdx].getAmount();
                refillIdx++;
            }
            mTrips[tripIdx].mBalance = balance;
            balance += mTrips[tripIdx].mFare;
            tripIdx++;
        }
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

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber);
        parcel.writeLong(mBalance);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);

        parcel.writeInt(mRefills.length);
        parcel.writeTypedArray(mRefills, flags);
    }
}
