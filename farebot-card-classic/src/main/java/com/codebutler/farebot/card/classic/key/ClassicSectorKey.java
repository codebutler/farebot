/*
 * ClassicSectorKey.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
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

package com.codebutler.farebot.card.classic.key;

import android.support.annotation.NonNull;

import com.codebutler.farebot.core.ByteArray;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ClassicSectorKey {

    public static final String TYPE_KEYA = "KeyA";
    public static final String TYPE_KEYB = "KeyB";

    @NonNull
    public static ClassicSectorKey create(@NonNull String keyType, @NonNull byte[] key) {
        return new AutoValue_ClassicSectorKey(keyType, ByteArray.create(key));
    }

    public abstract String getType();

    public abstract ByteArray getKey();
}
