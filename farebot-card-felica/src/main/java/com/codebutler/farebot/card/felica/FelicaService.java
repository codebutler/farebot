/*
 * FelicaService.java
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

package com.codebutler.farebot.card.felica;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

@AutoValue
public abstract class FelicaService {

    @NonNull
    public static FelicaService create(int serviceCode, @NonNull List<FelicaBlock> blocks) {
        return new AutoValue_FelicaService(serviceCode, blocks);
    }

    @NonNull
    public static TypeAdapter<FelicaService> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_FelicaService.GsonTypeAdapter(gson);
    }

    public abstract int getServiceCode();

    @NonNull
    public abstract List<FelicaBlock> getBlocks();
}
