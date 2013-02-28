/*
 * BilheteUnicoSPTransitData.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.transit;

import android.os.Parcel;
import com.codebutler.farebot.ListItem;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

public class BilheteUnicoSPTransitData extends TransitData {

    private static final byte[] MANUFACTURER = {
        (byte) 0x62,
        (byte) 0x63,
        (byte) 0x64,
        (byte) 0x65,
        (byte) 0x66,
        (byte) 0x67,
        (byte) 0x68,
        (byte) 0x69
    };

    private final BilheteUnicoSPCredit mCredit;

    public Creator<BilheteUnicoSPTransitData> CREATOR = new Creator<BilheteUnicoSPTransitData>() {
        public BilheteUnicoSPTransitData createFromParcel(Parcel parcel) {
            return new BilheteUnicoSPTransitData(parcel);
        }

        public BilheteUnicoSPTransitData[] newArray(int size) {
            return new BilheteUnicoSPTransitData[size];
        }
    };

    public static boolean check(Card card) {
        if (!(card instanceof ClassicCard))
            return false;

        ClassicCard classicCard = (ClassicCard) card;
        byte[] blockData = classicCard.getSector(0).getBlock(0).getData();
        return Arrays.equals(Arrays.copyOfRange(blockData, 8, 16), MANUFACTURER);
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        return null;
    }

    public BilheteUnicoSPTransitData(Parcel parcel) {
        mCredit   = parcel.readParcelable(OVChipCredit.class.getClassLoader());
    }

    public BilheteUnicoSPTransitData(ClassicCard card) {
        mCredit = new BilheteUnicoSPCredit(card.getSector(8).getBlock(1).getData());
    }

    public static String convertAmount(int amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        formatter.setCurrency(Currency.getInstance("BRL"));

        return formatter.format((double)amount / 100.0);
    }

    @Override
    public String getCardName () {
        return "Bilhete Único";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(mCredit, flags);
    }

    @Override
    public String getBalanceString() {
        return BilheteUnicoSPTransitData.convertAmount(mCredit.getCredit());
    }

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return null;
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }
}