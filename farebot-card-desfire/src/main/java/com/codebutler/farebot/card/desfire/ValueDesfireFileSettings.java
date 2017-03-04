/*
 * ValueDesfireFileSettings.java
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

import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;

/**
 * Contains FileSettings for Value file types.
 * See GetFileSettings for schemadata.
 */
@AutoValue
public abstract class ValueDesfireFileSettings extends DesfireFileSettings {

    @NonNull
    public static ValueDesfireFileSettings create(
            byte fileType,
            byte commSetting,
            @NonNull byte[] accessRights,
            int lowerLimit,
            int upperLimit,
            int limitedCreditValue,
            boolean limitedCreditEnabled) {
        return new AutoValue_ValueDesfireFileSettings(
                fileType,
                commSetting,
                ByteArray.create(accessRights),
                lowerLimit,
                upperLimit,
                limitedCreditValue,
                limitedCreditEnabled);
    }

    public abstract int getLowerLimit();

    public abstract int getUpperLimit();

    public abstract int getLimitedCreditValue();

    public abstract boolean getLimitedCreditEnabled();
}
