/*
 * TransitData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.ui.ListItem;

import java.util.List;

public abstract class TransitData implements Parcelable {

    @NonNull
    public abstract String getBalanceString();

    @Nullable
    public abstract String getSerialNumber();

    @Nullable
    public abstract List<Trip> getTrips();

    @Nullable
    public abstract List<Refill> getRefills();

    @Nullable
    public abstract List<Subscription> getSubscriptions();

    @NonNull
    public abstract String getCardName();

    /**
     * If a TransitData provider doesn't know some of the stops / stations on a user's card, then
     * it may raise a signal to the user to submit the unknown stations to our web service.
     *
     * @return false if all stations are known (default), true if there are unknown stations
     */
    public boolean hasUnknownStations() {
        return false;
    }

    @Nullable
    public List<ListItem> getInfo() {
        return null;
    }
}
