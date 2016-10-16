/*
 * OVChipDBUtil.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc;

import android.content.Context;

import com.codebutler.farebot.util.DBUtil;

public class OVChipDBUtil extends DBUtil {

    static final String TABLE_NAME = "stations_data";

    static final String COLUMN_ROW_COMPANY = "company";
    static final String COLUMN_ROW_OVCID = "ovcid";
    static final String COLUMN_ROW_NAME = "name";
    static final String COLUMN_ROW_CITY = "city";
    static final String COLUMN_ROW_LON = "lon";
    static final String COLUMN_ROW_LAT = "lat";

    static final String[] COLUMNS_STATIONDATA;

    private static final String COLUMN_ROW_LONGNAME = "longname";
    private static final String COLUMN_ROW_HALTENR = "haltenr";
    private static final String COLUMN_ROW_ZONE = "zone";

    private static final String DB_NAME = "ovc_stations.db3";

    private static final int VERSION = 2;

    static {
        COLUMNS_STATIONDATA = new String[]{
                COLUMN_ROW_COMPANY,
                COLUMN_ROW_OVCID,
                COLUMN_ROW_NAME,
                COLUMN_ROW_CITY,
                COLUMN_ROW_LONGNAME,
                COLUMN_ROW_HALTENR,
                COLUMN_ROW_ZONE,
                COLUMN_ROW_LON,
                COLUMN_ROW_LAT,
        };
    }

    public OVChipDBUtil(Context context) {
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
