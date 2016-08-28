/*
 * OrcaTransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2015 Sean CyberKitsune McClenaghan <cyberkitsune09@gmail.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit.orca;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.RecordDesfireFile;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

import org.apache.commons.lang3.ArrayUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class OrcaTransitData extends TransitData {

    static final int AGENCY_KCM = 0x04;
    static final int AGENCY_PT = 0x06;
    static final int AGENCY_ST = 0x07;
    static final int AGENCY_CT = 0x02;
    static final int AGENCY_WSF = 0x08;
    static final int AGENCY_ET = 0x03;

    static final int TRANS_TYPE_PURSE_USE = 0x0c;
    static final int TRANS_TYPE_CANCEL_TRIP = 0x01;
    static final int TRANS_TYPE_PASS_USE = 0x60;

    private static final int TRANS_TYPE_TAP_IN = 0x03;
    private static final int TRANS_TYPE_TAP_OUT = 0x07;

    @NonNull
    public static OrcaTransitData create(DesfireCard desfireCard) {
        byte[] data;
        int serialNumber;
        int balance;
        List<Trip> trips;

        try {
            data = desfireCard.getApplication(0xffffff).getFile(0x0f).getData().bytes();
            serialNumber = Utils.byteArrayToInt(data, 5, 3);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }

        try {
            data = desfireCard.getApplication(0x3010f2).getFile(0x04).getData().bytes();
            balance = Utils.byteArrayToInt(data, 41, 2);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA balance", ex);
        }

        try {
            trips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA trips", ex);
        }

        return new AutoValue_OrcaTransitData(trips, serialNumber, balance);
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x3010f2) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0xffffff).getFile(0x0f).getData().bytes();
            return new TransitIdentity("ORCA", String.valueOf(Utils.byteArrayToInt(data, 4, 4)));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }
    }

    @NonNull
    @Override
    public String getCardName() {
        return "ORCA";
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getBalance() / 100);
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return Integer.toString(getSerialNumberData());
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @NonNull
    private static List<Trip> parseTrips(@NonNull DesfireCard card) {
        List<Trip> trips = new ArrayList<>();

        DesfireFile file = card.getApplication(0x3010f2).getFile(0x02);
        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) card.getApplication(0x3010f2).getFile(0x02);

            OrcaTrip[] useLog = new OrcaTrip[recordFile.getRecords().size()];
            for (int i = 0; i < useLog.length; i++) {
                useLog[i] = OrcaTrip.create(recordFile.getRecords().get(i));
            }
            Arrays.sort(useLog, new Trip.Comparator());
            ArrayUtils.reverse(useLog);

            for (int i = 0; i < useLog.length; i++) {
                OrcaTrip trip = useLog[i];
                OrcaTrip nextTrip = (i + 1 < useLog.length) ? useLog[i + 1] : null;

                if (isSameTrip(trip, nextTrip)) {
                    trips.add(MergedOrcaTrip.create(trip, nextTrip));
                    i++;
                    continue;
                }

                trips.add(trip);
            }
        }
        Collections.sort(trips, new Trip.Comparator());
        return trips;
    }

    private static boolean isSameTrip(OrcaTrip firstTrip, OrcaTrip secondTrip) {
        return firstTrip != null
                && secondTrip != null
                && firstTrip.getTransType() == TRANS_TYPE_TAP_IN
                && (secondTrip.getTransType() == TRANS_TYPE_TAP_OUT
                        || secondTrip.getTransType() == TRANS_TYPE_CANCEL_TRIP)
                && firstTrip.getAgency() == secondTrip.getAgency();
    }

    abstract int getSerialNumberData();

    abstract double getBalance();
}
