/*
 * CEPASCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
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

package com.codebutler.farebot.card.cepas;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.ezlink.EZLinkTransitData;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class CEPASCard implements Card, Parcelable {

    @NonNull
    public static CEPASCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<CEPASPurse> purses,
            @NonNull List<CEPASHistory> histories) {
        return new AutoValue_CEPASCard(tagId, scannedAt, purses, histories);
    }

    @NonNull
    @Override
    public CardType getCardType() {
        return CardType.CEPAS;
    }

    @Nullable
    @Override
    public TransitIdentity parseTransitIdentity() {
        if (EZLinkTransitData.check(this)) {
            return EZLinkTransitData.parseTransitIdentity(this);
        }
        return null;
    }

    @Nullable
    @Override
    public TransitData parseTransitData() {
        if (EZLinkTransitData.check(this)) {
            return EZLinkTransitData.create(this);
        }
        return null;
    }

    @NonNull
    public abstract List<CEPASPurse> getPurses();

    @NonNull
    public abstract List<CEPASHistory> getHistories();

    @Nullable
    public CEPASPurse getPurse(int purse) {
        return getPurses().get(purse);
    }

    @Nullable
    public CEPASHistory getHistory(int purse) {
        return getHistories().get(purse);
    }
}
