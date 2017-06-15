/*
 * UnauthorizedDesfireFile.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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
}
