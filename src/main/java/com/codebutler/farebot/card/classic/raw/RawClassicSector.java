/*
 * RawClassicSector.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic.raw;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.DataClassicSector;
import com.codebutler.farebot.card.classic.InvalidClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawClassicSector {

    public static final String TYPE_DATA = "data";
    public static final String TYPE_INVALID = "invalid";
    public static final String TYPE_UNAUTHORIZED = "unauthorized";

    @NonNull
    public static TypeAdapter<RawClassicSector> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawClassicSector.GsonTypeAdapter(gson);
    }

    @NonNull
    public static RawClassicSector createData(int index, @NonNull List<RawClassicBlock> blocks) {
        return new AutoValue_RawClassicSector(TYPE_DATA, index, blocks, null);
    }

    @NonNull
    public static RawClassicSector createInvalid(int index, @NonNull String errorMessage) {
        return new AutoValue_RawClassicSector(TYPE_INVALID, index, null, errorMessage);
    }

    @NonNull
    public static RawClassicSector createUnauthorized(int index) {
        return new AutoValue_RawClassicSector(TYPE_UNAUTHORIZED, index, null, null);
    }

    @NonNull
    public ClassicSector parse() {
        switch (type()) {
            case TYPE_DATA:
                List<ClassicBlock> blocks = newArrayList(transform(blocks(),
                        new Function<RawClassicBlock, ClassicBlock>() {
                            @Override
                            public ClassicBlock apply(RawClassicBlock rawClassicBlock) {
                                return rawClassicBlock.parse();
                            }
                        }));
                return DataClassicSector.create(index(), blocks);
            case TYPE_INVALID:
                return InvalidClassicSector.create(index(), errorMessage());
            case TYPE_UNAUTHORIZED:
                return UnauthorizedClassicSector.create(index());
        }
        throw new RuntimeException("Unknown type");
    }

    @NonNull
    public abstract String type();

    public abstract int index();

    @Nullable
    public abstract List<RawClassicBlock> blocks();

    @Nullable
    public abstract String errorMessage();
}
