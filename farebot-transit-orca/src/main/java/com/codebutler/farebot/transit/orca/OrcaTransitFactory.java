/*
 * OrcaTransitFactory.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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
import com.codebutler.farebot.base.util.ArrayUtils;
import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.RecordDesfireFile;
import com.codebutler.farebot.card.desfire.StandardDesfireFile;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.registry.annotations.TransitCard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.codebutler.farebot.transit.orca.OrcaData.TRANS_TYPE_CANCEL_TRIP;
import static com.codebutler.farebot.transit.orca.OrcaData.TRANS_TYPE_TAP_IN;
import static com.codebutler.farebot.transit.orca.OrcaData.TRANS_TYPE_TAP_OUT;

@TransitCard
public class OrcaTransitFactory implements TransitFactory<DesfireCard, OrcaTransitInfo> {

    @Override
    public boolean check(@NonNull DesfireCard card) {
        return (card.getApplication(0x3010f2) != null);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull DesfireCard card) {
        try {
            byte[] data = ((StandardDesfireFile) card.getApplication(0xffffff).getFile(0x0f)).getData().bytes();
            return TransitIdentity.create("ORCA", String.valueOf(ByteUtils.byteArrayToInt(data, 4, 4)));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }
    }

    @NonNull
    @Override
    public OrcaTransitInfo parseInfo(@NonNull DesfireCard card) {
        byte[] data;
        int serialNumber;
        int balance;
        List<Trip> trips;

        try {
            data = ((StandardDesfireFile) card.getApplication(0xffffff).getFile(0x0f)).getData().bytes();
            serialNumber = ByteUtils.byteArrayToInt(data, 5, 3);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }

        try {
            data = ((StandardDesfireFile) card.getApplication(0x3010f2).getFile(0x04)).getData().bytes();
            balance = ByteUtils.byteArrayToInt(data, 41, 2);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA balance", ex);
        }

        try {
            trips = parseTrips(card);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA trips", ex);
        }

        return new AutoValue_OrcaTransitInfo(trips, serialNumber, balance);
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

    private static boolean isSameTrip(@NonNull OrcaTrip firstTrip, @NonNull OrcaTrip secondTrip) {
        return firstTrip != null
                && secondTrip != null
                && firstTrip.getTransType() == TRANS_TYPE_TAP_IN
                && (secondTrip.getTransType() == TRANS_TYPE_TAP_OUT
                    || secondTrip.getTransType() == TRANS_TYPE_CANCEL_TRIP)
                && firstTrip.getAgency() == secondTrip.getAgency();
    }

}
