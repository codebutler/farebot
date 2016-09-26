/*
 * BilheteUnicoSPTransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.transit.bilhete_unico;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

@AutoValue
public abstract class BilheteUnicoSPTransitData extends TransitData {

    private static final String NAME = "Bilhete Único";

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

    @NonNull
    public static BilheteUnicoSPTransitData create(@NonNull ClassicCard card) {
        BilheteUnicoSPCredit credit = BilheteUnicoSPCredit.create(card.getSector(8).getBlock(1).getData().bytes());
        return new AutoValue_BilheteUnicoSPTransitData(credit);
    }

    public static boolean check(@NonNull ClassicCard card) {
        byte[] blockData = card.getSector(0).getBlock(0).getData().bytes();
        return Arrays.equals(Arrays.copyOfRange(blockData, 8, 16), MANUFACTURER);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        return new TransitIdentity(NAME, null);
    }

    private static String convertAmount(int amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        formatter.setCurrency(Currency.getInstance("BRL"));

        return formatter.format((double) amount / 100.0);
    }

    @NonNull
    @Override
    public String getCardName() {
        return NAME;
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return BilheteUnicoSPTransitData.convertAmount(getCredit().getCredit());
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return null;
    }

    @Nullable
    @Override
    public List<Trip> getTrips() {
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

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @NonNull
    abstract BilheteUnicoSPCredit getCredit();
}
