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

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.TransitIdentity;

/**
 * Stub implementation for Adelaide Metrocard (AU).
 *
 * https://github.com/micolous/metrodroid/wiki/Metrocard-%28Adelaide%29
 */
public class AdelaideMetrocardStubTransitData extends StubTransitData {
    public AdelaideMetrocardStubTransitData(Card card) {}

    @Override
    public String getCardName() {
        return "Metrocard (Adelaide)";
    }

    public static boolean check(Card card) {
        return (card instanceof DesfireCard)
                && (((DesfireCard) card).getApplication(0xb006f2) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        return new TransitIdentity("Metrocard (Adelaide)", null);
    }
}
