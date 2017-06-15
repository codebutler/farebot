/*
 * OpalTransitInfo.java
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

package com.codebutler.farebot.transit.opal;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Opal (Sydney, AU).
 * <p>
 * This uses the publicly-readable file on the card (7) in order to get the data.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Opal
 */
@AutoValue
public abstract class OpalTransitInfo extends TransitInfo {

    public static final String NAME = "Opal";

    private static final GregorianCalendar OPAL_EPOCH = new GregorianCalendar(1980, Calendar.JANUARY, 1);
    private static final OpalSubscription OPAL_AUTOMATIC_TOP_UP = OpalSubscription.create();

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return NAME;
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getBalance() / 100.);
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        // Opal has no concept of "subscriptions" (travel pass), only automatic top up.
        if (getAutoTopup()) {
            return Collections.<Subscription>singletonList(OPAL_AUTOMATIC_TOP_UP);
        }
        return Collections.emptyList();
    }

    @NonNull
    private static String getVehicleTypeName(@NonNull Resources resources, int vehicleType) {
        if (OpalData.VEHICLES.containsKey(vehicleType)) {
            return resources.getString(OpalData.VEHICLES.get(vehicleType));
        }
        return resources.getString(R.string.opal_unknown_format, "0x" + Long.toString(vehicleType, 16));
    }

    @NonNull
    private static String getActionTypeName(@NonNull Resources resources, int actionType) {
        if (OpalData.ACTIONS.containsKey(actionType)) {
            return resources.getString(OpalData.ACTIONS.get(actionType));
        }
        return resources.getString(R.string.opal_unknown_format, "0x" + Long.toString(actionType, 16));
    }

    @NonNull
    private Calendar getLastTransactionTime() {
        Calendar cLastTransaction = GregorianCalendar.getInstance();
        cLastTransaction.setTimeInMillis(OPAL_EPOCH.getTimeInMillis());
        cLastTransaction.add(Calendar.DATE, getDay());
        cLastTransaction.add(Calendar.MINUTE, getMinute());
        return cLastTransaction;
    }

    // Unsupported elements
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
    public FareBotUiTree getAdvancedUi(@NonNull Context context) {
        Date cLastTransactionTime = getLastTransactionTime().getTime();

        FareBotUiTree.Builder uiBuilder = FareBotUiTree.builder(context);

        FareBotUiTree.Item.Builder generalBuilder = uiBuilder.item()
                .title(R.string.opal_general);
        generalBuilder.item(R.string.opal_weekly_trips, getWeeklyTrips());
        generalBuilder.item(R.string.opal_checksum, Integer.toString(getChecksum()));

        FareBotUiTree.Item.Builder transactionUiBuilder = uiBuilder.item()
                .title(R.string.opal_last_transaction);

        transactionUiBuilder.item(R.string.opal_transaction_sequence, getTransactionNumber());
        transactionUiBuilder.item(R.string.opal_date,
                DateFormat.getLongDateFormat(context).format(cLastTransactionTime));
        transactionUiBuilder.item(R.string.opal_time,
                DateFormat.getTimeFormat(context).format(cLastTransactionTime));
        transactionUiBuilder.item(R.string.opal_vehicle_type,
                getVehicleTypeName(context.getResources(), getVehicleType()));
        transactionUiBuilder.item(R.string.opal_transaction_type,
                getActionTypeName(context.getResources(), getActionType()));

        return uiBuilder.build();
    }

    abstract int getBalance(); // cent

    abstract int getChecksum();

    abstract int getWeeklyTrips();

    abstract boolean getAutoTopup();

    abstract int getActionType();

    abstract int getVehicleType();

    abstract int getMinute();

    abstract int getDay();

    abstract int getTransactionNumber();
}
