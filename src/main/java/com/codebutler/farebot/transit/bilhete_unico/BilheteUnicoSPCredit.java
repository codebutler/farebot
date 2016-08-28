/*
 * BilheteUnicoSPCredit.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013 Marcelo Liberato <mliberato@gmail.com>
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.bilhete_unico;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@AutoValue
abstract class BilheteUnicoSPCredit implements Parcelable {

    @NonNull
    static BilheteUnicoSPCredit create(@Nullable byte[] data) {
        if (data == null) {
            data = new byte[16];
        }
        return new AutoValue_BilheteUnicoSPCredit(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0));
    }

    public abstract int getCredit();
}
