/*
 * KMTUtil.java
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

package com.codebutler.farebot.transit.kmt;

import net.kazzz.felica.lib.Util;

import java.util.Date;

final class KMTUtil {

    private KMTUtil() {
    }

    static Date extractDate(byte[] data) {
        int fulloffset = Util.toInt(data[0], data[1], data[2], data[3]);
        if (fulloffset == 0) {
            return null;
        }
        return new Date((long) (fulloffset + 946684758) * 1000);
    }
}
