/*
 * UnauthorizedClassicTransitData.java
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

package com.codebutler.farebot.transit.unknown;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * Handle MiFare Classic with no open sectors
 */
@AutoValue
public abstract class UnauthorizedClassicTransitData extends TransitData {

    /**
     * This should be the last executed Mifare Classic check, after all the other checks are done.
     * <p>
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all sectors on the card are locked.
     */
    public static boolean check(@NonNull ClassicCard card) {
        // check to see if all sectors are blocked
        for (ClassicSector s : card.getSectors()) {
            if (!(s instanceof UnauthorizedClassicSector)) {
                // At least one sector is "open", this is not for us
                return false;
            }
        }
        return true;
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(@NonNull Card card) {
        return new TransitIdentity(Utils.localizeString(R.string.locked_card), null);
    }

    @NonNull
    public static UnauthorizedClassicTransitData create() {
        return new AutoValue_UnauthorizedClassicTransitData();
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return null;
    }

    @NonNull
    @Override
    public String getSerialNumber() {
        return null;
    }

    @Nullable
    @Override
    public List<Trip> getTrips() {
        return null;
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
    }

    @NonNull
    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_card);
    }
}
