/*
 * KMTUtil.java
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codebutler.farebot.transit.kmt;

import net.kazzz.felica.lib.Util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

final class KMTUtil {

    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Asia/Jakarta");
    private static final long KMT_EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TIME_ZONE);
        epoch.set(2000, 0, 1, 7, 0, 0);
        KMT_EPOCH = epoch.getTimeInMillis();
    }

    static Date extractDate(byte[] data) {
        int fulloffset = Util.toInt(data[0], data[1], data[2], data[3]);
        if (fulloffset == 0) {
            return null;
        }
        Calendar c = new GregorianCalendar(TIME_ZONE);
        c.setTimeInMillis(KMT_EPOCH);
        c.add(Calendar.SECOND, fulloffset);
        return c.getTime();
    }
}
