/*
 * OVChipInfo.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.transit.ovc;

import android.support.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteUtils;
import com.google.auto.value.AutoValue;

import java.util.Calendar;
import java.util.Date;

@AutoValue
abstract class OVChipInfo {

    @NonNull
    static OVChipInfo create(byte[] data) {
        if (data == null) {
            data = new byte[48];
        }

        int company;
        int expdate;
        Date birthdate = new Date();
        int active = 0;
        int limit = 0;
        int charge = 0;
        int unknown = 0;

        company = ((char) data[6] >> 3) & (char) 0x1F; // Could be 4 bits though
        expdate = (((char) data[6] & (char) 0x07) << 11)
                | (((char) data[7] & (char) 0xFF) << 3)
                | (((char) data[8] >> 5) & (char) 0x07);

        if ((data[13] & (byte) 0x02) == (byte) 0x02) {
            // Has date of birth, so it's a personal card (no autocharge on anonymous cards)
            int year = (ByteUtils.convertBCDtoInteger(data[14]) * 100) + ByteUtils.convertBCDtoInteger(data[15]);
            int month = ByteUtils.convertBCDtoInteger(data[16]);
            int day = ByteUtils.convertBCDtoInteger(data[17]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            birthdate = calendar.getTime();

            active = (data[22] >> 5) & (byte) 0x07;
            limit = (((char) data[22] & (char) 0x1F) << 11) | (((char) data[23] & (char) 0xFF) << 3)
                    | (((char) data[24] >> 5) & (char) 0x07);
            charge = (((char) data[24] & (char) 0x1F) << 11) | (((char) data[25] & (char) 0xFF) << 3)
                    | (((char) data[26] >> 5) & (char) 0x07);
            unknown = (((char) data[26] & (char) 0x1F) << 11) | (((char) data[27] & (char) 0xFF) << 3)
                    | (((char) data[28] >> 5) & (char) 0x07);
        }

        return new AutoValue_OVChipInfo(
                company,
                expdate,
                birthdate,
                active,
                limit,
                charge,
                unknown);
    }

    public abstract int getCompany();

    public abstract int getExpdate();

    public abstract Date getBirthdate();

    public abstract int getActive();

    public abstract int getLimit();

    public abstract int getCharge();

    public abstract int getUnknown();
}
