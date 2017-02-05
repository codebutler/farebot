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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.codebutler.farebot.BuildConfig;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.card.provider.CardDBHelper;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExportHelper {

    @NonNull private final Context mContext;
    @NonNull private final CardPersister mCardPersister;
    @NonNull private final Gson mGson;

    public ExportHelper(@NonNull Context context, @NonNull CardPersister cardPersister, @NonNull Gson gson) {
        mContext = context;
        mCardPersister = cardPersister;
        mGson = gson;
    }

    @NonNull
    public String exportCards() {
        List<RawCard> cards = new ArrayList<>();
        Cursor cursor = CardDBHelper.createCursor(mContext);
        while (cursor.moveToNext()) {
            cards.add(mCardPersister.readCard(cursor));
        }
        Export export = new Export();
        export.versionName = BuildConfig.VERSION_NAME;
        export.versionCode = BuildConfig.VERSION_CODE;
        export.cards = cards;
        return mGson.toJson(export);
    }

    @NonNull
    public List<Uri> importCards(@NonNull String exportJsonString) {
        List<Uri> uris = new ArrayList<>();
        Export export = mGson.fromJson(exportJsonString, Export.class);
        for (RawCard card : export.cards) {
            uris.add(mCardPersister.saveCard(card));
        }
        return Collections.unmodifiableList(uris);
    }

    @SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
    private static class Export {
        String versionName;
        int versionCode;
        List<RawCard> cards;
    }
}
