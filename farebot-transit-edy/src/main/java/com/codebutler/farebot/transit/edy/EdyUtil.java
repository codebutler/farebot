/*
 * EdyUtil.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.edy;

import net.kazzz.felica.lib.Util;

import java.util.Calendar;
import java.util.Date;

final class EdyUtil {

    private EdyUtil() { }

    static Date extractDate(byte[] data) {
        int fulloffset = Util.toInt(data[4], data[5], data[6], data[7]);
        if (fulloffset == 0) {
            return null;
        }

        int dateoffset = fulloffset >>> 17;
        int timeoffset = fulloffset & 0x1ffff;

        Calendar c = Calendar.getInstance();
        c.set(2000, 0, 1, 0, 0, 0);
        c.add(Calendar.DATE, dateoffset);
        c.add(Calendar.SECOND, timeoffset);

        return c.getTime();
    }
}
