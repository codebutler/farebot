package com.codebutler.farebot.transit.edy;

import net.kazzz.felica.lib.Util;

import java.util.Calendar;
import java.util.Date;

final class EdyUtil {
    private EdyUtil() { }

    static Date extractDate(byte[] data) {
        int fulloffset = Util.toInt(data[4], data[5], data[6], data[7]);
        if (fulloffset == 0)
            return null;

        int dateoffset = fulloffset >>> 17;
        int timeoffset = fulloffset & 0x1ffff;

        Calendar c = Calendar.getInstance();
        c.set(2000, 0, 1, 0, 0, 0);
        c.add(Calendar.DATE, dateoffset);
        c.add(Calendar.SECOND, timeoffset);

        return c.getTime();
    }
}
