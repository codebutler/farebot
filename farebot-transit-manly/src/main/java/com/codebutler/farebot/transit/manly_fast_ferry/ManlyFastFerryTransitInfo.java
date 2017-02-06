/*
 * ManlyFastFerryTransitInfo.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.manly_fast_ferry;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import com.codebutler.farebot.core.ui.HeaderListItem;
import com.codebutler.farebot.core.ui.ListItem;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 * <p>
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 * <p>
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Manly-Fast-Ferry
 */
@AutoValue
public abstract class ManlyFastFerryTransitInfo extends TransitInfo {

    public static final String NAME = "Manly Fast Ferry";

    @NonNull
    static ManlyFastFerryTransitInfo create(
            @NonNull String serialNumber,
            @NonNull ArrayList<Trip> trips,
            @NonNull ArrayList<Refill> refills,
            @NonNull GregorianCalendar epochDate,
            int balance) {
        return new AutoValue_ManlyFastFerryTransitInfo(serialNumber, trips, refills, epochDate, balance);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getBalance() / 100.);
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        // There is no concept of "subscriptions".
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo(@NonNull Context context) {
        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(context.getString(R.string.general)));
        Date cLastTransactionTime = getEpochDate().getTime();
        items.add(new ListItem(
                context.getString(R.string.card_epoch),
                DateFormat.getLongDateFormat(context).format(cLastTransactionTime)));
        return items;
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return NAME;
    }

    abstract GregorianCalendar getEpochDate();

    abstract int getBalance();
}
