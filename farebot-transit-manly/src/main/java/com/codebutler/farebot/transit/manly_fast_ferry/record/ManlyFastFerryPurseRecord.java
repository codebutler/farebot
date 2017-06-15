/*
 * ManlyFastFerryPurseRecord.java
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

package com.codebutler.farebot.transit.manly_fast_ferry.record;

import android.support.annotation.Nullable;

import com.codebutler.farebot.base.util.ByteUtils;
import com.google.auto.value.AutoValue;

/**
 * Represents a "purse" type record.
 */
@AutoValue
public abstract class ManlyFastFerryPurseRecord extends ManlyFastFerryRegularRecord {

    @Nullable
    public static ManlyFastFerryPurseRecord recordFromBytes(byte[] input) {
        if (input[0] != 0x02) {
            throw new AssertionError("PurseRecord input[0] != 0x02");
        }

        boolean isCredit;

        if (input[3] == 0x09) {
            isCredit = false;
        } else if (input[3] == 0x08) {
            isCredit = true;
        } else {
            // bad record?
            return null;
        }

        int day = ByteUtils.getBitsFromBuffer(input, 32, 20);
        if (day < 0) {
            throw new AssertionError("Day < 0");
        }

        int minute = ByteUtils.getBitsFromBuffer(input, 52, 12);
        if (minute > 1440) {
            throw new AssertionError("Minute > 1440");
        }
        if (minute < 0) {
            throw new AssertionError("Minute < 0");
        }

        int transactionValue = ByteUtils.byteArrayToInt(input, 8, 4);
        if (transactionValue < 0) {
            throw new AssertionError("Value < 0");
        }

        return new AutoValue_ManlyFastFerryPurseRecord(day, minute, transactionValue, isCredit);
    }

    public abstract int getDay();

    public abstract int getMinute();

    public abstract int getTransactionValue();

    public abstract boolean getIsCredit();
}
