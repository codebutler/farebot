/*
 * Subscription.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 *
 * Based on code from http://http://www.huuf.info/OV/
 * by Huuf. See project URL for complete author information.
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

    public final int describeContents() {
        return 0;
    }
}
