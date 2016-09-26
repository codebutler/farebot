/*
 * CardJsonSerializer.java
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

package com.codebutler.farebot.serialize;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.RawCard;
import com.google.gson.Gson;

public class CardJsonSerializer implements CardSerializer {

    @NonNull private final Gson mGson;

    public CardJsonSerializer(@NonNull Gson gson) {
        mGson = gson;
    }

    @Override
    @NonNull
    public String serialize(@NonNull RawCard card) {
        return mGson.toJson(card);
    }

    @Override
    @NonNull
    public RawCard deserialize(@NonNull String json) {
        return mGson.fromJson(json, RawCard.class);
    }
}
