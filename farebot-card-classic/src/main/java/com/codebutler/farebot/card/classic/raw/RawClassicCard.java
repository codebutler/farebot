/*
 * RawClassicCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class RawClassicCard implements RawCard<ClassicCard> {

    @NonNull
    public static RawClassicCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull List<RawClassicSector> sectors) {
        return new AutoValue_RawClassicCard(ByteArray.create(tagId), scannedAt, sectors);
    }

    @NonNull
    public static TypeAdapter<RawClassicCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawClassicCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.MifareClassic;
    }

    @Override
    public boolean isUnauthorized() {
        for (RawClassicSector sector : sectors()) {
            if (!sector.type().equals(RawClassicSector.TYPE_UNAUTHORIZED)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    @Override
    public ClassicCard parse() {
        List<ClassicSector> sectors = Lists.newArrayList(Iterables.transform(sectors(),
                new Function<RawClassicSector, ClassicSector>() {
                    @Override
                    public ClassicSector apply(RawClassicSector rawClassicSector) {
                        return rawClassicSector.parse();
                    }
                }));
        return ClassicCard.create(tagId(), scannedAt(), sectors);
    }

    @NonNull
    public abstract List<RawClassicSector> sectors();
}
