/*
 * InvalidDesfireFile.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
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

package com.codebutler.farebot.card.desfire;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class InvalidDesfireFile implements DesfireFile {

    @NonNull
    public static InvalidDesfireFile create(
            int id,
            @NonNull DesfireFileSettings fileSettings,
            @NonNull String errorMessage) {
        return new AutoValue_InvalidDesfireFile(id, fileSettings, errorMessage);
    }

    @NonNull
    public abstract String getErrorMessage();

    @NonNull
    @Override
    public ByteArray getData() {
        throw new IllegalStateException(String.format("Invalid file: %s", getErrorMessage()));
    }
}
