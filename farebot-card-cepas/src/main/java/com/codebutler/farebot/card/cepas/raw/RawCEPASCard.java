/*
 * RawCEPASCard.java
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

package com.codebutler.farebot.card.cepas.raw;

import android.support.annotation.NonNull;
import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.cepas.CEPASHistory;
import com.codebutler.farebot.card.cepas.CEPASPurse;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawCEPASCard implements RawCard<CEPASCard> {

    @NonNull
    public static RawCEPASCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull List<RawCEPASPurse> purses,
            @NonNull List<RawCEPASHistory> histories) {
        return new AutoValue_RawCEPASCard(ByteArray.create(tagId), scannedAt, purses, histories);
    }

    @NonNull
    public static TypeAdapter<RawCEPASCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawCEPASCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.CEPAS;
    }

    @Override
    public boolean isUnauthorized() {
        return false;
    }

    @NonNull
    @Override
    public CEPASCard parse() {
        List<CEPASPurse> purses = newArrayList(transform(purses(),
                new Function<RawCEPASPurse, CEPASPurse>() {
                    @Override
                    public CEPASPurse apply(RawCEPASPurse rawCEPASPurse) {
                        return rawCEPASPurse.parse();
                    }
                }));
        List<CEPASHistory> histories = newArrayList(transform(histories(),
                new Function<RawCEPASHistory, CEPASHistory>() {
                    @Override
                    public CEPASHistory apply(RawCEPASHistory rawCEPASHistory) {
                        return rawCEPASHistory.parse();
                    }
                }));
        return CEPASCard.create(tagId(), scannedAt(), purses, histories);
    }

    @NonNull
    abstract List<RawCEPASPurse> purses();

    @NonNull
    abstract List<RawCEPASHistory> histories();
}
