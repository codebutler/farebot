/*
 * OpalTransitFactory.java
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
package com.codebutler.farebot.transit.opal;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.util.Utils;

/**
 * Transit data type for Opal (Sydney, AU).
 * <p>
 * This uses the publicly-readable file on the card (7) in order to get the data.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Opal
 */
public class OpalTransitFactory implements TransitFactory<DesfireCard, OpalTransitData> {

    @Override
    public boolean check(@NonNull DesfireCard card) {
        return (card.getApplication(0x314553) != null);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull DesfireCard desfireCard) {
        byte[] data = desfireCard.getApplication(0x314553).getFile(0x07).getData().bytes();
        data = Utils.reverseBuffer(data, 0, 5);

        int lastDigit = Utils.getBitsFromBuffer(data, 4, 4);
        int serialNumber = Utils.getBitsFromBuffer(data, 8, 32);
        return TransitIdentity.create(OpalTransitData.NAME, formatSerialNumber(serialNumber, lastDigit));
    }

    @NonNull
    @Override
    public OpalTransitData parseData(@NonNull DesfireCard desfireCard) {
        try {
            byte[] data = desfireCard.getApplication(0x314553).getFile(0x07).getData().bytes();
            int iRawBalance;

            data = Utils.reverseBuffer(data, 0, 16);

            int checksum = Utils.getBitsFromBuffer(data, 0, 16);
            int weeklyTrips = Utils.getBitsFromBuffer(data, 16, 4);
            boolean autoTopup = Utils.getBitsFromBuffer(data, 20, 1) == 0x01;
            int actionType = Utils.getBitsFromBuffer(data, 21, 4);
            int vehicleType = Utils.getBitsFromBuffer(data, 25, 3);
            int minute = Utils.getBitsFromBuffer(data, 28, 11);
            int day = Utils.getBitsFromBuffer(data, 39, 15);
            iRawBalance = Utils.getBitsFromBuffer(data, 54, 21);
            int transactionNumber = Utils.getBitsFromBuffer(data, 75, 16);
            // Skip bit here
            int lastDigit = Utils.getBitsFromBuffer(data, 92, 4);
            int serialNumber = Utils.getBitsFromBuffer(data, 96, 32);

            int balance = Utils.unsignedToTwoComplement(iRawBalance, 20);

            return new AutoValue_OpalTransitData(
                    formatSerialNumber(serialNumber, lastDigit),
                    balance,
                    checksum,
                    weeklyTrips,
                    autoTopup,
                    actionType,
                    vehicleType,
                    minute,
                    day,
                    transactionNumber);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Opal data", ex);
        }
    }

    @NonNull
    private static String formatSerialNumber(int serialNumber, int lastDigit) {
        return String.format("308522%09d%01d", serialNumber, lastDigit);
    }
}
