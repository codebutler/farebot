/*
 * RawDesfireCard.java
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

package com.codebutler.farebot.card.desfire.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.core.ByteArray;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawDesfireCard implements RawCard {

    @NonNull
    public static RawDesfireCard create(
            @NonNull byte[] tagId,
            @NonNull Date date,
            @NonNull List<RawDesfireApplication> apps,
            @NonNull RawDesfireManufacturingData manufData) {
        return new AutoValue_RawDesfireCard(ByteArray.create(tagId), date, apps, manufData);
    }

    @NonNull
    public static TypeAdapter<RawDesfireCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.MifareDesfire;
    }

    @NonNull
    @Override
    public DesfireCard parse() {
        List<DesfireApplication> applications = newArrayList(transform(applications(),
                new Function<RawDesfireApplication, DesfireApplication>() {
                    @Override
                    public DesfireApplication apply(RawDesfireApplication rawDesfireApplication) {
                        return rawDesfireApplication.parse();
                    }
                }));
        return DesfireCard.create(tagId(), scannedAt(), applications, manufacturingData().parse());
    }

    @NonNull
    public abstract List<RawDesfireApplication> applications();

    @NonNull
    public abstract RawDesfireManufacturingData manufacturingData();
}
