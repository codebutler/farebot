/*
 * Subscription.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2015-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

import java.util.Date;

public abstract class Subscription implements Parcelable {
    public abstract int getId();

    public abstract Date getValidFrom();

    public abstract Date getValidTo();

    public abstract String getAgencyName();

    public abstract String getShortAgencyName();

    public abstract int getMachineId();

    public abstract String getSubscriptionName();

    public abstract String getActivation();
}
