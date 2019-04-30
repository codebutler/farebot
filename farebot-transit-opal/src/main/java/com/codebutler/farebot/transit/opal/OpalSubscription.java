/*
 * OpalSubscription.java
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

import android.content.res.Resources;
import androidx.annotation.NonNull;

import com.codebutler.farebot.transit.Subscription;
import com.google.auto.value.AutoValue;

import java.util.Date;

/**
 * Class describing auto-topup on Opal.
 *
 * Opal has no concept of subscriptions, but when auto-topup is enabled, you no longer need to
 * manually refill the card with credit.
 *
 * Dates given are not valid.
 */
@AutoValue
abstract class OpalSubscription extends Subscription {

    @NonNull
    public static OpalSubscription create() {
        return new AutoValue_OpalSubscription();
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Date getValidFrom() {
        // Start of Opal trial
        return new Date(2012 - 1900, 12 - 1, 7);
    }

    @Override
    public Date getValidTo() {
        // Maximum possible date representable on the card
        return new Date(2159 - 1900, 6 - 1, 6);
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        return getShortAgencyName(resources);
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        return "Opal";
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName(@NonNull Resources resources) {
        return resources.getString(R.string.opal_automatic_top_up);
    }

    @Override
    public String getActivation() {
        return null;
    }
}
