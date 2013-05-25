/*
 * TransitData.java
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
import com.codebutler.farebot.ListItem;

import java.util.List;

public abstract class TransitData implements Parcelable {
    public abstract String getBalanceString();
    public abstract String getSerialNumber();
    public abstract Trip[] getTrips();
    public abstract Refill[] getRefills();
    public abstract Subscription[] getSubscriptions();
    public abstract List<ListItem> getInfo();
    public abstract String getCardName();

    public final int describeContents() {
        return 0;
    }

}
