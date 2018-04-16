/*
 * RawUltralightCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
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

package com.codebutler.farebot.card.ultralight.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.transit.registry.annotations.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.ultralight.UltralightCard;
import com.codebutler.farebot.card.ultralight.UltralightPage;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class RawUltralightCard implements RawCard<UltralightCard> {

    @NonNull
    public static RawUltralightCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull List<UltralightPage> pages,
            int type) {
        return new AutoValue_RawUltralightCard(ByteArray.create(tagId), scannedAt, pages, type);
    }

    @NonNull
    public static TypeAdapter<RawUltralightCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawUltralightCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.MifareUltralight;
    }

    @Override
    public boolean isUnauthorized() {
        return false;
    }

    @NonNull
    @Override
    public UltralightCard parse() {
        return UltralightCard.create(tagId(), scannedAt(), pages(), ultralightType());
    }

    @NonNull
    public abstract List<UltralightPage> pages();

    abstract int ultralightType();
}
