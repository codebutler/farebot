/*
 * SeqGoDBUtil.java
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

import android.content.Context;

import com.codebutler.farebot.util.DBUtil;

/**
 * Database functionality for SEQ Go Cards
 */
public class SeqGoDBUtil extends DBUtil {
    public static final String TABLE_NAME = "stops";
    public static final String COLUMN_ROW_ID = "id";
    public static final String COLUMN_ROW_NAME = "name";
    // TODO: Implement travel zones
    //public static final String COLUMN_ROW_ZONE = "zone";
    public static final String COLUMN_ROW_LON = "x";
    public static final String COLUMN_ROW_LAT = "y";

    public static final String[] COLUMNS_STATIONDATA = {
            COLUMN_ROW_ID,
            COLUMN_ROW_NAME,
            //COLUMN_ROW_ZONE,
            COLUMN_ROW_LON,
            COLUMN_ROW_LAT,
    };

    private static final String DB_NAME = "seq_go_stations.db3";

    private static final int VERSION = 3721;

    public SeqGoDBUtil(Context context) {
        super(context);
    }

    @Override
    protected String getDBName() {
        return DB_NAME;
    }

    @Override
    protected int getDesiredVersion() {
        return VERSION;
    }

}
