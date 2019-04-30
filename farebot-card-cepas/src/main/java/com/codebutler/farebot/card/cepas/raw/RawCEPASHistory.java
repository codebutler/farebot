/*
 * RawCEPASHistory.java
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.card.cepas.CEPASHistory;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RawCEPASHistory {

    @NonNull
    public static RawCEPASHistory create(int id, @NonNull byte[] data) {
        return new AutoValue_RawCEPASHistory(id, ByteArray.create(data), null);
    }

    @NonNull
    public static RawCEPASHistory create(int id, @NonNull String errorMessage) {
        return new AutoValue_RawCEPASHistory(id, null, errorMessage);
    }

    @NonNull
    public static TypeAdapter<RawCEPASHistory> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawCEPASHistory.GsonTypeAdapter(gson);
    }

    @NonNull
    public CEPASHistory parse() {
        ByteArray data = data();
        if (data != null) {
            return CEPASHistory.create(id(), data.bytes());
        }
        return CEPASHistory.create(id(), errorMessage());
    }

    abstract int id();

    @Nullable
    abstract ByteArray data();

    @Nullable
    abstract String errorMessage();
}
