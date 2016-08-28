/*
 * AdelaideMetrocardStubTransitData.java
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

package com.codebutler.farebot.transit.stub;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;
import com.google.auto.value.AutoValue;

/**
 * Stub implementation for Adelaide Metrocard (AU).
 * <p>
 * https://github.com/micolous/metrodroid/wiki/Metrocard-%28Adelaide%29
 */
@AutoValue
public abstract class AdelaideMetrocardStubTransitData extends StubTransitData {

    @NonNull
    public static AdelaideMetrocardStubTransitData create(@NonNull DesfireCard card) {
        return new AutoValue_AdelaideMetrocardStubTransitData();
    }

    public static boolean check(@NonNull Card card) {
        return (card instanceof DesfireCard)
                && (((DesfireCard) card).getApplication(0xb006f2) != null);
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(@NonNull Card card) {
        return new TransitIdentity("Metrocard (Adelaide)", null);
    }

    @NonNull
    @Override
    public String getCardName() {
        return "Metrocard (Adelaide)";
    }
}
