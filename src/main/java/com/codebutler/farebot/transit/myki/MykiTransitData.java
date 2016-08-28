/*
 * MykiTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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

import android.net.Uri;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.stub.StubTransitData;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

/**
 * Transit data type for Myki (Melbourne, AU).
 * <p>
 * This is a very limited implementation of reading Myki, because most of the data is stored in
 * locked files.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Myki
 */
@AutoValue
public abstract class MykiTransitData extends StubTransitData {

    public static final String NAME = "Myki";

    @NonNull
    public static MykiTransitData create(@NonNull DesfireCard card) {
        try {
            byte[] metadata = Utils.reverseBuffer(card.getApplication(4594).getFile(15).getData().bytes(), 0, 16);
            int serialNumber1 = Utils.getBitsFromBuffer(metadata, 96, 32);
            int serialNumber2 = Utils.getBitsFromBuffer(metadata, 64, 32);
            return new AutoValue_MykiTransitData(serialNumber1, serialNumber2);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Myki data", ex);
        }
    }

    public static boolean check(@NonNull Card card) {
        return (card instanceof DesfireCard)
                && (((DesfireCard) card).getApplication(4594) != null)
                && (((DesfireCard) card).getApplication(15732978) != null);
    }

    @NonNull
    @Override
    public String getCardName() {
        return NAME;
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return formatSerialNumber(getSerialNumber1(), getSerialNumber2());
    }

    @NonNull
    private static String formatSerialNumber(long serialNumber1, long serialNumber2) {
        String formattedSerial = String.format("%06d%08d", serialNumber1, serialNumber2);
        return formattedSerial + Utils.calculateLuhn(formattedSerial);
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(@NonNull Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(4594).getFile(15).getData().bytes();
        data = Utils.reverseBuffer(data, 0, 16);

        long serialNumber1 = Utils.getBitsFromBuffer(data, 96, 32);
        long serialNumber2 = Utils.getBitsFromBuffer(data, 64, 32);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber1, serialNumber2));
    }

    @Override
    public Uri getMoreInfoPage() {
        return Uri.parse("https://micolous.github.io/metrodroid/myki");
    }

    abstract long getSerialNumber1();

    abstract long getSerialNumber2();
}
