/*
 * RawCEPASPurse.java
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
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.cepas.CEPASPurse;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RawCEPASPurse {

    @NonNull
    public static RawCEPASPurse create(int id, byte[] data) {
        return new AutoValue_RawCEPASPurse(id, ByteArray.create(data), null);
    }

    @NonNull
    public static RawCEPASPurse create(int id, String errorMessage) {
        return new AutoValue_RawCEPASPurse(id, null, errorMessage);
    }

    @NonNull
    public static TypeAdapter<RawCEPASPurse> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawCEPASPurse.GsonTypeAdapter(gson);
    }

    public boolean isValid() {
        return data() != null;
    }

    @NonNull
    public CEPASPurse parse() {
        if (isValid()) {
            return CEPASPurse.create(id(), data().bytes());
        }
        return CEPASPurse.create(id(), errorMessage());
    }

    public byte logfileRecordCount() {
        return data().bytes()[40];
    }

    abstract int id();

    @Nullable
    abstract ByteArray data();

    @Nullable
    abstract String errorMessage();
}
