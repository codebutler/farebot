/*
 * ExportHelper.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.util;

import android.support.annotation.NonNull;

import com.codebutler.farebot.BuildConfig;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.serialize.CardSerializer;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.persist.model.SavedCard;
import com.google.common.base.Function;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

public class ExportHelper {

    @NonNull private final CardPersister mCardPersister;
    @NonNull private final CardSerializer mCardSerializer;
    @NonNull private final Gson mGson;

    public ExportHelper(
            @NonNull CardPersister cardPersister,
            @NonNull CardSerializer cardSerializer,
            @NonNull Gson gson) {
        mCardPersister = cardPersister;
        mCardSerializer = cardSerializer;
        mGson = gson;
    }

    @NonNull
    public String exportCards() {
        Export export = new Export();
        export.versionName = BuildConfig.VERSION_NAME;
        export.versionCode = BuildConfig.VERSION_CODE;
        export.cards = newArrayList(transform(mCardPersister.getCards(), new Function<SavedCard, RawCard>() {
            @Override
            public RawCard apply(SavedCard savedCard) {
                return mCardSerializer.deserialize(savedCard.data());
            }
        }));
        return mGson.toJson(export);
    }

    @NonNull
    public List<Long> importCards(@NonNull String exportJsonString) {
        List<Long> ids = new ArrayList<>();
        Export export = mGson.fromJson(exportJsonString, Export.class);
        for (RawCard card : export.cards) {
            ids.add(mCardPersister.insertCard(SavedCard.create(
                    card.cardType(),
                    card.tagId().hex(),
                    mCardSerializer.serialize(card))));
        }
        return Collections.unmodifiableList(ids);
    }

    @SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
    private static class Export {
        String versionName;
        int versionCode;
        List<RawCard> cards;
    }
}
