/*
 * ClassicCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.classic.ui.ClassicCardRawDataFragment;
import com.codebutler.farebot.core.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardHasManufacturingInfo;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@CardRawDataFragmentClass(ClassicCardRawDataFragment.class)
@CardHasManufacturingInfo(false)
@AutoValue
public abstract class ClassicCard extends Card {

    @NonNull
    public static ClassicCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<ClassicSector> sectors) {
        return new AutoValue_ClassicCard(tagId, scannedAt, sectors);
    }

    @NonNull
    public CardType getCardType() {
        return CardType.MifareClassic;
    }

    @NonNull
    public abstract List<ClassicSector> getSectors();

    public ClassicSector getSector(int index) {
        return getSectors().get(index);
    }
}
