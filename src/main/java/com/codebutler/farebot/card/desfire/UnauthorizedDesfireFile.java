/*
 * UnsupportedDesfireFile.java
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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

/**
 * Represents a DESFire file which could not be read due to
 * access control limits.
 */
@AutoValue
public abstract class UnauthorizedDesfireFile implements DesfireFile {

    @NonNull
    public static UnauthorizedDesfireFile create(
            int fileId,
            @NonNull DesfireFileSettings settings,
            @NonNull String errorMessage) {
        return new AutoValue_UnauthorizedDesfireFile(fileId, settings, errorMessage);
    }

    @NonNull
    public abstract String getErrorMessage();

    @NonNull
    @Override
    public ByteArray getData() {
        throw new IllegalStateException(String.format("Unauthorized access to file: %s", getErrorMessage()));
    }
}
