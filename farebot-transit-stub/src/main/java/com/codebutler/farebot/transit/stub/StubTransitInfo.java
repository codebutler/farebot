/*
 * StubTransitInfo.java
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

package com.codebutler.farebot.transit.stub;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.core.ui.HeaderListItem;
import com.codebutler.farebot.core.ui.ListItem;
import com.codebutler.farebot.core.ui.UriListItem;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class used to identify cards that we don't yet know the format of.
 * <p>
 * This allows the cards to be identified by name but will not attempt to read the content.
 */
public abstract class StubTransitInfo extends TransitInfo {

    // Stub out elements that we can't support

    @Nullable
    @Override
    public String getSerialNumber() {
        return null;
    }

    @NonNull
    @Override
    public final String getBalanceString(@NonNull Resources resources) {
        return null;
    }

    @Nullable
    @Override
    public final List<Trip> getTrips() {
        return null;
    }

    @Nullable
    @Override
    public final List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public final List<Refill> getRefills() {
        return null;
    }

    @Nullable
    protected Uri getMoreInfoPage() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo(@NonNull Context context) {
        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(context.getString(R.string.general)));
        items.add(new ListItem(context.getString(R.string.card_type), getCardName(context.getResources())));
        items.add(new ListItem(
                context.getString(R.string.unknown_card_header),
                context.getString(R.string.unknown_card_description)));
        if (getMoreInfoPage() != null) {
            items.add(new UriListItem(
                    context.getString(R.string.unknown_more_info),
                    context.getString(R.string.unknown_more_info_desc),
                    getMoreInfoPage()));
        }
        return items;
    }
}
