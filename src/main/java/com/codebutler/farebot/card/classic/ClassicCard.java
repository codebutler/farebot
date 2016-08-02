/*
 * ClassicCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2012-2013 Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.card.classic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardHasManufacturingInfo;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.fragment.ClassicCardRawDataFragment;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.bilhete_unico.BilheteUnicoSPTransitData;
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitData;
import com.codebutler.farebot.transit.ovc.OVChipTransitData;
import com.codebutler.farebot.transit.seq_go.SeqGoTransitData;
import com.codebutler.farebot.transit.unknown.UnauthorizedClassicTransitData;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@CardRawDataFragmentClass(ClassicCardRawDataFragment.class)
@CardHasManufacturingInfo(false)
@AutoValue
public abstract class ClassicCard implements Card {

    @NonNull
    public static ClassicCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<ClassicSector> sectors) {
        return new AutoValue_ClassicCard(tagId, scannedAt, sectors);
    }

    @NonNull
    @Override
    public CardType getCardType() {
        return CardType.MifareClassic;
    }

    @Nullable
    @Override
    public TransitIdentity parseTransitIdentity() {
        // All .check() methods should work without a key, and throw an UnauthorizedException
        // Otherwise UnauthorizedClassicTransitData will not trigger
        if (OVChipTransitData.check(this)) {
            return OVChipTransitData.parseTransitIdentity(this);
        } else if (BilheteUnicoSPTransitData.check(this)) {
            return BilheteUnicoSPTransitData.parseTransitIdentity(this);
        } else if (ManlyFastFerryTransitData.check(this)) {
            return ManlyFastFerryTransitData.parseTransitIdentity(this);
        } else if (SeqGoTransitData.check(this)) {
            return SeqGoTransitData.parseTransitIdentity(this);
        } else if (UnauthorizedClassicTransitData.check(this)) {
            // This check must be LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            return UnauthorizedClassicTransitData.parseTransitIdentity(this);
        }

        // The card could not be identified, but has some open sectors.
        return null;
    }

    @Nullable
    @Override
    public TransitData parseTransitData() {
        if (OVChipTransitData.check(this)) {
            return new OVChipTransitData(this);
        } else if (BilheteUnicoSPTransitData.check(this)) {
            return new BilheteUnicoSPTransitData(this);
        } else if (ManlyFastFerryTransitData.check(this)) {
            return new ManlyFastFerryTransitData(this);
        } else if (SeqGoTransitData.check(this)) {
            return new SeqGoTransitData(this);
        } else if (UnauthorizedClassicTransitData.check(this)) {
            // This check must be LAST.
            //
            // This is to throw up a warning whenever there is a card with all locked sectors
            return new UnauthorizedClassicTransitData();
        }

        // The card could not be identified, but has some open sectors.
        return null;
    }

    @NonNull
    public abstract List<ClassicSector> getSectors();

    public ClassicSector getSector(int index) {
        return getSectors().get(index);
    }
}
