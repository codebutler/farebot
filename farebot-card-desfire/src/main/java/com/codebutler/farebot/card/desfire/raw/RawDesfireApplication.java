/*
 * RawDesfireApplication.java
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

import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawDesfireApplication {

    @NonNull
    public static RawDesfireApplication create(int appId, @NonNull List<RawDesfireFile> rawDesfireFiles) {
        return new AutoValue_RawDesfireApplication(appId, rawDesfireFiles);
    }

    @NonNull
    public static TypeAdapter<RawDesfireApplication> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireApplication.GsonTypeAdapter(gson);
    }

    @NonNull
    public DesfireApplication parse() {
        List<DesfireFile> files = newArrayList(transform(files(), new Function<RawDesfireFile, DesfireFile>() {
            @Override
            public DesfireFile apply(RawDesfireFile rawDesfireFile) {
                return rawDesfireFile.parse();
            }
        }));
        return DesfireApplication.create(appId(), files);
    }

    public abstract int appId();

    @NonNull
    public abstract List<RawDesfireFile> files();
}
