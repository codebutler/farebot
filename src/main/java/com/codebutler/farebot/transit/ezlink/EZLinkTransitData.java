/*
 * EZLinkTransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
 * Copyright (C) 2012 Victor Heng <bakavic@gmail.com>
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

package com.codebutler.farebot.transit.ezlink;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.cepas.CEPASTransaction;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;

@AutoValue
public abstract class EZLinkTransitData extends TransitData {

    @NonNull
    public static EZLinkTransitData create(@NonNull CEPASCard cepasCard) {
        String serialNumber = cepasCard.getPurse(3).getCAN().hex();
        int balance = cepasCard.getPurse(3).getPurseBalance();
        EZLinkTrip[] trips = parseTrips(serialNumber, cepasCard);
        return new AutoValue_EZLinkTransitData(serialNumber, ImmutableList.<Trip>copyOf(trips), balance);
    }

    public static boolean check(@NonNull Card card) {
        if (card instanceof CEPASCard) {
            CEPASCard cepasCard = (CEPASCard) card;
            return cepasCard.getHistory(3) != null
                    && cepasCard.getHistory(3).isValid()
                    && cepasCard.getPurse(3) != null
                    && cepasCard.getPurse(3).isValid();
        }
        return false;
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(@NonNull Card card) {
        String canNo = ((CEPASCard) card).getPurse(3).getCAN().hex();
        return new TransitIdentity(getCardIssuer(canNo), canNo);
    }

    @NonNull
    @Override
    public String getCardName() {
        return getCardIssuer(getSerialNumber());
    }

    @NonNull
    @Override
    public String getBalanceString() {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance("SGD"));
        return numberFormat.format(getBalance() / 100);
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
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

    abstract double getBalance();

    @NonNull
    private static EZLinkTrip[] parseTrips(@NonNull String serialNumber, @NonNull CEPASCard card) {
        List<CEPASTransaction> transactions = card.getHistory(3).getTransactions();
        if (transactions != null) {
            EZLinkTrip[] trips = new EZLinkTrip[transactions.size()];
            for (int i = 0; i < trips.length; i++) {
                trips[i] = EZLinkTrip.create(transactions.get(i), getCardIssuer(serialNumber));
            }
            return trips;
        }
        return new EZLinkTrip[0];
    }

    @NonNull
    private static String getCardIssuer(@NonNull String canNo) {
        int issuerId = Integer.parseInt(canNo.substring(0, 3));
        switch (issuerId) {
            case 100:
                return "EZ-Link";
            case 111:
                return "NETS";
            default:
                return "CEPAS";
        }
    }
}
