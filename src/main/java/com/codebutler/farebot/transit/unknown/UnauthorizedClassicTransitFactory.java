/*
 * UnauthorizedClassicTransitFactory.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2016 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.R;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.util.Utils;

public class UnauthorizedClassicTransitFactory implements TransitFactory<ClassicCard, UnauthorizedClassicTransitData> {

    /**
     * This should be the last executed Mifare Classic check, after all the other checks are done.
     * <p>
     * This is because it will catch others' cards.
     *
     * @param card Card to read.
     * @return true if all sectors on the card are locked.
     */
    @Override
    public boolean check(@NonNull ClassicCard card) {
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
    @Override
    public TransitIdentity parseIdentity(@NonNull ClassicCard card) {
        return TransitIdentity.create(Utils.localizeString(R.string.locked_card), null);
    }

    @NonNull
    @Override
    public UnauthorizedClassicTransitData parseData(@NonNull ClassicCard card) {
        return UnauthorizedClassicTransitData.create();
    }
}
