/*
 * Refill.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011, 2015-2016 Eric Butler <eric@codebutler.com>
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

import android.content.res.Resources;
import android.support.annotation.NonNull;

public abstract class Refill {

    public abstract long getTimestamp();

    public abstract String getAgencyName(@NonNull Resources resources);

    public abstract String getShortAgencyName(@NonNull Resources resources);

    public abstract long getAmount();

    public abstract String getAmountString(@NonNull Resources resources);

    public static class Comparator implements java.util.Comparator<Refill> {
        @Override
        public int compare(Refill lhs, Refill rhs) {
            // For consistency with Trip, this is reversed.
            return Long.valueOf(rhs.getTimestamp()).compareTo(lhs.getTimestamp());
        }
    }
}
