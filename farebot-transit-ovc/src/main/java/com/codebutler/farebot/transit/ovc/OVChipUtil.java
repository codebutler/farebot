/*
 * OVChipUtil.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;

public final class OVChipUtil {

    private OVChipUtil() { }

    @NonNull
    static Date convertDate(int date) {
        return convertDate(date, 0);
    }

    @NonNull
    static Date convertDate(int date, int time) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1997);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, time / 60);
        calendar.set(Calendar.MINUTE, time % 60);

        calendar.add(Calendar.DATE, date);

        return calendar.getTime();
    }

    @NonNull
    static String convertAmount(int amount) {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance();
        formatter.setCurrency(Currency.getInstance("EUR"));
        String symbol = formatter.getCurrency().getSymbol();
        formatter.setNegativePrefix(symbol + "-");
        formatter.setNegativeSuffix("");

        return formatter.format((double) amount / 100.0);
    }
}
