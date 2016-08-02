/*
 * InvalidClassicSector.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class InvalidClassicSector extends ClassicSector {

    @NonNull
    public static InvalidClassicSector create(int index, String error) {
        return new AutoValue_InvalidClassicSector(index, error);
    }

    @NonNull
    public abstract String getError();

    @NonNull
    @Override
    public byte[] readBlocks(int startBlock, int blockCount) {
        throw new IllegalStateException(getError());
    }

    @NonNull
    @Override
    public List<ClassicBlock> getBlocks() {
        throw new IllegalStateException(getError());
    }

    @NonNull
    @Override
    public ClassicBlock getBlock(int index) {
        throw new IllegalStateException(getError());
    }
}
