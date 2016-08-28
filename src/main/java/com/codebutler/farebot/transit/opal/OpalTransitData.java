/*
 * OpalTransitData.java
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
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.ArrayList;
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
public abstract class OpalTransitData extends TransitData {

    public static final String NAME = "Opal";

    private static final GregorianCalendar OPAL_EPOCH = new GregorianCalendar(1980, Calendar.JANUARY, 1);
    private static final OpalSubscription OPAL_AUTOMATIC_TOP_UP = OpalSubscription.create();

    @NonNull
    public static OpalTransitData create(@NonNull DesfireCard desfireCard) {
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
                    serialNumber,
                    balance,
                    checksum,
                    weeklyTrips,
                    autoTopup,
                    actionType,
                    vehicleType,
                    minute,
                    day,
                    transactionNumber,
                    lastDigit);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Opal data", ex);
        }
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x314553) != null);
    }

    @NonNull
    @Override
    public String getCardName() {
        return NAME;
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getBalance() / 100.);
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return formatSerialNumber(getSerialNumberData(), getLastDigit());
    }

    private static String formatSerialNumber(int serialNumber, int lastDigit) {
        return String.format("308522%09d%01d", serialNumber, lastDigit);
    }

    private Calendar getLastTransactionTime() {
        Calendar cLastTransaction = GregorianCalendar.getInstance();
        cLastTransaction.setTimeInMillis(OPAL_EPOCH.getTimeInMillis());
        cLastTransaction.add(Calendar.DATE, getDay());
        cLastTransaction.add(Calendar.MINUTE, getMinute());
        return cLastTransaction;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        FareBotApplication app = FareBotApplication.getInstance();
        Date cLastTransactionTime = getLastTransactionTime().getTime();

        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.opal_weekly_trips, Integer.toString(getWeeklyTrips())));
        items.add(new ListItem(R.string.checksum, Integer.toString(getChecksum())));
        items.add(new HeaderListItem(R.string.last_transaction));
        items.add(new ListItem(R.string.transaction_sequence, Integer.toString(getTransactionNumber())));
        items.add(new ListItem(R.string.date, DateFormat.getLongDateFormat(app).format(cLastTransactionTime)));
        items.add(new ListItem(R.string.time, DateFormat.getTimeFormat(app).format(cLastTransactionTime)));
        items.add(new ListItem(R.string.vehicle_type, getVehicleTypeName(getVehicleType())));
        items.add(new ListItem(R.string.transaction_type, getActionTypeName(getActionType())));
        return items;
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(0x314553).getFile(0x07).getData().bytes();
        data = Utils.reverseBuffer(data, 0, 5);

        int lastDigit = Utils.getBitsFromBuffer(data, 4, 4);
        int serialNumber = Utils.getBitsFromBuffer(data, 8, 32);
        return new TransitIdentity(NAME, formatSerialNumber(serialNumber, lastDigit));
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

    private static String getVehicleTypeName(int vehicleType) {
        if (OpalData.VEHICLES.containsKey(vehicleType)) {
            return Utils.localizeString(OpalData.VEHICLES.get(vehicleType));
        }
        return Utils.localizeString(R.string.unknown_format, "0x" + Long.toString(vehicleType, 16));
    }

    private static String getActionTypeName(int actionType) {
        if (OpalData.ACTIONS.containsKey(actionType)) {
            return Utils.localizeString(OpalData.ACTIONS.get(actionType));
        }

        return Utils.localizeString(R.string.unknown_format, "0x" + Long.toString(actionType, 16));
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

    abstract int getSerialNumberData();

    abstract int getBalance(); // cent

    abstract int getChecksum();

    abstract int getWeeklyTrips();

    abstract boolean getAutoTopup();

    abstract int getActionType();

    abstract int getVehicleType();

    abstract int getMinute();

    abstract int getDay();

    abstract int getTransactionNumber();

    abstract int getLastDigit();
}
