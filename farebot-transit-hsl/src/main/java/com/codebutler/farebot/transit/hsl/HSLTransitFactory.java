/*
 * HSLTransitFactory.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.hsl;

import androidx.annotation.NonNull;

import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.DesfireRecord;
import com.codebutler.farebot.card.desfire.RecordDesfireFile;
import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.card.desfire.StandardDesfireFile;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HSLTransitFactory implements TransitFactory<DesfireCard, HSLTransitInfo> {

    private static final long EPOCH = 0x32C97ED0;

    @Override
    public boolean check(@NonNull DesfireCard card) {
        return (card.getApplication(0x1120ef) != null);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull DesfireCard card) {
        try {
            byte[] data = ((StandardDesfireFile) card.getApplication(0x1120ef).getFile(0x08)).getData().bytes();
            return TransitIdentity.create("HSL", ByteUtils.getHexString(data).substring(2, 20));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL serial", ex);
        }
    }

    @NonNull
    @Override
    public HSLTransitInfo parseInfo(@NonNull DesfireCard desfireCard) {
        try {
            byte[] data = ((StandardDesfireFile) desfireCard.getApplication(0x1120ef).getFile(0x08)).getData().bytes();
            String serialNumber = ByteUtils.getHexString(data).substring(2, 20);  //Utils.byteArrayToInt(data, 1, 9);

            data = ((StandardDesfireFile) desfireCard.getApplication(0x1120ef).getFile(0x02)).getData().bytes();
            long balance = bitsToLong(0, 20, data);
            HSLRefill lastRefill = createRefill(data);

            List<HSLTrip> trips = parseTrips(desfireCard);

            int balanceIndex = -1;

            for (int i = 0; i < trips.size(); ++i) {
                if (trips.get(i).getArvo() == 1) {
                    balanceIndex = i;
                    break;
                }
            }

            data = ((StandardDesfireFile) desfireCard.getApplication(0x1120ef).getFile(0x03)).getData().bytes();
            long arvoMystery1 = bitsToLong(0, 9, data);
            long arvoDiscoGroup = bitsToLong(9, 5, data);
            long arvoDuration = bitsToLong(14, 13, data);
            long arvoRegional = bitsToLong(27, 5, data);

            long arvoExit = cardDateToTimestamp(bitsToLong(32, 14, data), bitsToLong(46, 11, data));
            long arvoPurchasePrice = bitsToLong(68, 14, data);
            //mArvoDiscoGroup = bitsToLong(82, 6,data);

            //68 price, 82 zone?
            long arvoPurchase = cardDateToTimestamp(bitsToLong(88, 14, data), bitsToLong(102, 11, data));

            //68 price, 82 zone?
            long arvoExpire = cardDateToTimestamp(bitsToLong(113, 14, data), bitsToLong(127, 11, data));

            long arvoPax = bitsToLong(138, 6, data);

            //68 price, 82 zone?
            long arvoXfer = cardDateToTimestamp(bitsToLong(144, 14, data), bitsToLong(158, 11, data));

            long arvoVehicleNumber = bitsToLong(169, 14, data);

            long arvoUnknown = bitsToLong(183, 2, data);

            long arvoLineJORE = bitsToLong(185, 14, data);
            long arvoJOREExt = bitsToLong(199, 4, data);
            long arvoDirection = bitsToLong(203, 1, data);

            if (balanceIndex > -1) {
                trips.set(balanceIndex, trips.get(balanceIndex).toBuilder()
                        .line(Long.toString(arvoLineJORE))
                        .vehicleNumber(arvoVehicleNumber)
                        .build());
            } else if (arvoPurchase > 2) {
                trips.add(HSLTrip.builder()
                        .arvo(1)
                        .expireTimestamp(arvoExpire)
                        .fare(arvoPurchasePrice)
                        .pax(arvoPax)
                        .timestamp(arvoPurchase)
                        .vehicleNumber(arvoVehicleNumber)
                        .line(Long.toString(arvoLineJORE))
                        .build());
                Collections.sort(trips, new Trip.Comparator());
            }

            int seasonIndex = -1;
            for (int i = 0; i < trips.size(); ++i) {
                if (trips.get(i).getArvo() == 0) {
                    seasonIndex = i;
                    break;
                }
            }

            data = ((StandardDesfireFile) desfireCard.getApplication(0x1120ef).getFile(0x01)).getData().bytes();

            boolean kausiNoData = false;
            if (bitsToLong(19, 14, data) == 0 && bitsToLong(67, 14, data) == 0) {
                kausiNoData = true;
            }

            long kausiStart = cardDateToTimestamp(bitsToLong(19, 14, data), 0);
            long kausiEnd = cardDateToTimestamp(bitsToLong(33, 14, data), 0);
            long kausiPrevStart = cardDateToTimestamp(bitsToLong(67, 14, data), 0);
            long kausiPrevEnd = cardDateToTimestamp(bitsToLong(81, 14, data), 0);
            if (kausiPrevStart > kausiStart) {
                final long temp = kausiStart;
                final long temp2 = kausiEnd;
                kausiStart = kausiPrevStart;
                kausiEnd = kausiPrevEnd;
                kausiPrevStart = temp;
                kausiPrevEnd = temp2;
            }
            boolean hasKausi = kausiEnd > (System.currentTimeMillis() / 1000.0);
            long kausiPurchase = cardDateToTimestamp(bitsToLong(110, 14, data), bitsToLong(124, 11, data));
            long kausiPurchasePrice = bitsToLong(149, 15, data);
            long kausiLastUse = cardDateToTimestamp(bitsToLong(192, 14, data), bitsToLong(206, 11, data));
            long kausiVehicleNumber = bitsToLong(217, 14, data);
            //mTrips[0].mVehicleNumber = mArvoVehicleNumber;

            long kausiUnknown = bitsToLong(231, 2, data);

            long kausiLineJORE = bitsToLong(233, 14, data);
            //mTrips[0].mLine = Long.toString(mArvoLineJORE).substring(1);

            long kausiJOREExt = bitsToLong(247, 4, data);
            long kausiDirection = bitsToLong(241, 1, data);
            if (seasonIndex > -1) {
                trips.set(seasonIndex, trips.get(seasonIndex).toBuilder()
                        .vehicleNumber(kausiVehicleNumber)
                        .line(Long.toString(kausiLineJORE))
                        .build());
            } else if (kausiVehicleNumber > 0) {
                trips.add(HSLTrip.builder()
                        .arvo(0)
                        .expireTimestamp(kausiPurchase)
                        .fare(kausiPurchasePrice)
                        .pax(1)
                        .timestamp(kausiPurchase)
                        .vehicleNumber(kausiVehicleNumber)
                        .line(Long.toString(kausiLineJORE))
                        .build());
                Collections.sort(trips, new Trip.Comparator());
            }

            return HSLTransitInfo.builder()
                    .serialNumber(serialNumber)
                    .trips(ImmutableList.<Trip>copyOf(trips))
                    .refills(Collections.<Refill>singletonList(lastRefill))
                    .balance(balance)
                    .hasKausi(hasKausi)
                    .kausiStart(kausiStart)
                    .kausiEnd(kausiEnd)
                    .kausiPrevStart(kausiPrevStart)
                    .kausiPrevEnd(kausiPrevEnd)
                    .kausiPurchasePrice(kausiPurchasePrice)
                    .kausiLastUse(kausiLastUse)
                    .kausiPurchase(kausiPurchase)
                    .kausiNoData(kausiNoData)
                    .arvoExit(arvoExit)
                    .arvoPurchase(arvoPurchase)
                    .arvoExpire(arvoExpire)
                    .arvoPax(arvoPax)
                    .arvoPurchasePrice(arvoPurchasePrice)
                    .arvoXfer(arvoXfer)
                    .arvoDiscoGroup(arvoDiscoGroup)
                    .arvoMystery1(arvoMystery1)
                    .arvoDuration(arvoDuration)
                    .arvoRegional(arvoRegional)
                    .arvoJOREExt(arvoJOREExt)
                    .arvoVehicleNumber(arvoVehicleNumber)
                    .arvoUnknown(arvoUnknown)
                    .arvoLineJORE(arvoLineJORE)
                    .kausiVehicleNumber(kausiVehicleNumber)
                    .kausiUnknown(kausiUnknown)
                    .kausiLineJORE(kausiLineJORE)
                    .kausiJOREExt(kausiJOREExt)
                    .arvoDirection(arvoDirection)
                    .kausiDirection(kausiDirection)
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL data", ex);
        }
    }

    @NonNull
    private static List<HSLTrip> parseTrips(@NonNull DesfireCard card) {
        DesfireFile file = card.getApplication(0x1120ef).getFile(0x04);
        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) card.getApplication(0x1120ef).getFile(0x04);
            List<HSLTrip> useLog = new ArrayList<>();
            for (int i = 0; i < recordFile.getRecords().size(); i++) {
                useLog.add(createTrip(recordFile.getRecords().get(i)));
            }
            Collections.sort(useLog, new Trip.Comparator());
            return useLog;
        }
        return ImmutableList.of();
    }

    @NonNull
    static HSLTrip createTrip(@NonNull DesfireRecord record) {
        byte[] useData = record.getData().bytes();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        long arvo = bitsToLong(0, 1, usefulData);
        long timestamp = cardDateToTimestamp(bitsToLong(1, 14, usefulData), bitsToLong(15, 11, usefulData));
        long expireTimestamp = cardDateToTimestamp(bitsToLong(26, 14, usefulData), bitsToLong(40, 11, usefulData));
        long fare = bitsToLong(51, 14, usefulData);
        long pax = bitsToLong(65, 5, usefulData);
        String line = null;
        long vehicleNumber = -1;
        long newBalance = bitsToLong(70, 20, usefulData);

        return HSLTrip.builder()
                .timestamp(timestamp)
                .line(line)
                .vehicleNumber(vehicleNumber)
                .fare(fare)
                .arvo(arvo)
                .expireTimestamp(expireTimestamp)
                .pax(pax)
                .newBalance(newBalance)
                .build();
    }

    @NonNull
    private static HSLRefill createRefill(byte[] data) {
        long timestamp = cardDateToTimestamp(
                bitsToLong(20, 14, data),
                bitsToLong(34, 11, data));
        long amount = bitsToLong(45, 20, data);
        return HSLRefill.create(timestamp, amount);
    }

    private static long bitsToLong(int start, int len, byte[] data) {
        long ret = 0;
        for (int i = start; i < start + len; ++i) {
            long bit = ((data[i / 8] >> (7 - i % 8)) & 1);
            ret = ret | (bit << ((start + len - 1) - i));
        }
        return ret;
    }

    private static long bitsToLong(int start, int len, long[] data) {
        long ret = 0;
        for (int i = start; i < start + len; ++i) {
            long bit = ((data[i / 8] >> (7 - i % 8)) & 1);
            ret = ret | (bit << ((start + len - 1) - i));
        }
        return ret;
    }

    private static long cardDateToTimestamp(long day, long minute) {
        return (EPOCH) + day * (60 * 60 * 24) + minute * 60;
    }
}
