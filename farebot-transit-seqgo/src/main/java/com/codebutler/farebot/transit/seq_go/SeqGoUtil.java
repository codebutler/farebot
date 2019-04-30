/*
 * SeqGoUtil.java
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

package com.codebutler.farebot.transit.seq_go;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import android.util.Log;

import com.codebutler.farebot.base.util.ByteUtils;
import com.codebutler.farebot.transit.Station;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Misc utilities for parsing Go Cards
 *
 * @author Michael Farrell
 */
public final class SeqGoUtil {

    private static final String TAG = "SeqGoUtil";

    private SeqGoUtil() { }

    /**
     * Date format:
     * <p>
     * 0001111 1100 00100 = 2015-12-04
     * yyyyyyy mmmm ddddd
     * <p>
     * Bottom 11 bits = minutes since 00:00
     * Time is represented in localtime, Australia/Brisbane.
     * <p>
     * Assumes that data has already been byte-reversed for big endian parsing.
     *
     * @param timestamp Four bytes of input representing the timestamp to parse
     * @return Date and time represented by that value
     */
    public static GregorianCalendar unpackDate(byte[] timestamp) {
        final int minute = ByteUtils.getBitsFromBuffer(timestamp, 5, 11);
        final int year = ByteUtils.getBitsFromBuffer(timestamp, 16, 7) + 2000;
        final int month = ByteUtils.getBitsFromBuffer(timestamp, 23, 4);
        final int day = ByteUtils.getBitsFromBuffer(timestamp, 27, 5);

        //Log.i(TAG, "unpackDate: " + minute + " minutes, " + year + '-' + month + '-' + day);

        if (minute > 1440) {
            throw new AssertionError("Minute > 1440");
        }
        if (minute < 0) {
            throw new AssertionError("Minute < 0");
        }

        if (day > 31) {
            throw new AssertionError("Day > 31");
        }
        if (month > 12) {
            throw new AssertionError("Month > 12");
        }

        GregorianCalendar d = new GregorianCalendar(year, month - 1, day);
        d.add(Calendar.MINUTE, minute);

        return d;
    }

    public static Station getStation(@NonNull SeqGoDBUtil dbUtil, int stationId) {
        if (stationId == 0) {
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            try {
                db = dbUtil.openDatabase();
            } catch (IOException ex) {
                Log.e(TAG, "Error connecting database", ex);
                return null;
            }

            cursor = db.query(
                    SeqGoDBUtil.TABLE_NAME,
                    SeqGoDBUtil.COLUMNS_STATIONDATA,
                    String.format("%s = ?", SeqGoDBUtil.COLUMN_ROW_ID),
                    new String[]{
                            String.valueOf(stationId),
                    },
                    null,
                    null,
                    SeqGoDBUtil.COLUMN_ROW_ID);

            if (!cursor.moveToFirst()) {
                Log.w(TAG, String.format("FAILED get station %s",
                        stationId));

                return null;
            }

            String stationName = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_NAME));
            String latitude = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_LAT));
            String longitude = cursor.getString(cursor.getColumnIndex(SeqGoDBUtil.COLUMN_ROW_LON));

            return Station.builder()
                    .stationName(stationName)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
