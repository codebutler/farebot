/*
 * ClassicBlock.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic;

import android.support.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ClassicBlock {

    public static final String TYPE_DATA = "data";
    public static final String TYPE_MANUFACTURER = "manufacturer";
    public static final String TYPE_TRAILER = "trailer";
    public static final String TYPE_VALUE = "value";

    @NonNull
    public static ClassicBlock create(@NonNull String type, int index, @NonNull ByteArray data) {
        return new AutoValue_ClassicBlock(type, index, data);
    }

    @NonNull
    public abstract String getType();

    public abstract int getIndex();

    @NonNull
    public abstract ByteArray getData();
}
