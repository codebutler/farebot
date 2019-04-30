/*
 * MykiTransitFactory.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.myki;

import androidx.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.base.util.Luhn;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.StandardDesfireFile;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;

public class MykiTransitFactory implements TransitFactory<DesfireCard, MykiTransitInfo> {

    @Override
    public boolean check(@NonNull DesfireCard card) {
        return (card.getApplication(4594) != null) && (card.getApplication(15732978) != null);
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull DesfireCard desfireCard) {
        byte[] data = ((StandardDesfireFile) desfireCard.getApplication(4594).getFile(15)).getData().bytes();
        data = ByteUtils.reverseBuffer(data, 0, 16);

        long serialNumber1 = ByteUtils.getBitsFromBuffer(data, 96, 32);
        long serialNumber2 = ByteUtils.getBitsFromBuffer(data, 64, 32);
        return TransitIdentity.create(MykiTransitInfo.NAME, formatSerialNumber(serialNumber1, serialNumber2));
    }

    @NonNull
    @Override
    public MykiTransitInfo parseInfo(@NonNull DesfireCard card) {
        try {
            byte[] data = ((StandardDesfireFile) card.getApplication(4594).getFile(15)).getData().bytes();
            byte[] metadata = ByteUtils.reverseBuffer(data, 0, 16);
            int serialNumber1 = ByteUtils.getBitsFromBuffer(metadata, 96, 32);
            int serialNumber2 = ByteUtils.getBitsFromBuffer(metadata, 64, 32);
            return MykiTransitInfo.create(formatSerialNumber(serialNumber1, serialNumber2));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Myki data", ex);
        }
    }

    @NonNull
    private static String formatSerialNumber(long serialNumber1, long serialNumber2) {
        String formattedSerial = String.format("%06d%08d", serialNumber1, serialNumber2);
        return formattedSerial + Luhn.calculateLuhn(formattedSerial);
    }
}
