/*
 * RawFelicaCard.java
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

package com.codebutler.farebot.card.felica.raw;

import androidx.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaSystem;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import net.kazzz.felica.lib.FeliCaLib;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class RawFelicaCard implements RawCard {

    @NonNull
    public static RawFelicaCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull FeliCaLib.IDm idm,
            @NonNull FeliCaLib.PMm pmm,
            @NonNull List<FelicaSystem> systems) {
        return new AutoValue_RawFelicaCard(ByteArray.create(tagId), scannedAt, idm, pmm, systems);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.FeliCa;
    }

    @NonNull
    public static TypeAdapter<RawFelicaCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawFelicaCard.GsonTypeAdapter(gson);
    }

    @Override
    public boolean isUnauthorized() {
        return false;
    }

    @NonNull
    @Override
    public Card parse() {
        return FelicaCard.create(tagId(), scannedAt(), idm(), pmm(), systems());
    }

    @NonNull
    abstract FeliCaLib.IDm idm();

    @NonNull
    abstract FeliCaLib.PMm pmm();

    @NonNull
    public abstract List<FelicaSystem> systems();
}
