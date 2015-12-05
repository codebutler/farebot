/*
 * Refill.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

public abstract class Refill implements Parcelable {
    public abstract long getTimestamp();

    public abstract String getAgencyName();
    public abstract String getShortAgencyName();

    public abstract long getAmount();
    public abstract String getAmountString();

    public final int describeContents() {
        return 0;
    }

    public static class Comparator implements java.util.Comparator<Refill> {
        @Override public int compare(Refill lhs, Refill rhs) {
            // For consistency with Trip, this is reversed.
            return Long.valueOf(rhs.getTimestamp()).compareTo(lhs.getTimestamp());
        }
    }
}
