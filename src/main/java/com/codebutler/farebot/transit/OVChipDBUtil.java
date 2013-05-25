/*
 * OVChipDBUtil.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class OVChipDBUtil {
    public static final String TABLE_NAME = "stations_data";
    public static final String COLUMN_ROW_COMPANY = "company";
    public static final String COLUMN_ROW_OVCID = "ovcid";
    public static final String COLUMN_ROW_NAME = "name";
    public static final String COLUMN_ROW_CITY = "city";
    public static final String COLUMN_ROW_LONGNAME = "longname";
    public static final String COLUMN_ROW_HALTENR = "haltenr";
    public static final String COLUMN_ROW_ZONE = "zone";
    public static final String COLUMN_ROW_LON = "lon";
    public static final String COLUMN_ROW_LAT = "lat";

    public static final String[] COLUMNS_STATIONDATA = {
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

    private static final String TAG = "OVChipDBUtil";

    private static final String DB_PATH = "/data/data/com.codebutler.farebot/databases/";
    private static final String DB_NAME = "stations.sqlite";

    private static final int VERSION = 2;

    private SQLiteDatabase mDatabase;
    private final Context mContext;

    public OVChipDBUtil(Context context) {
        this.mContext = context;
    }

    public SQLiteDatabase openDatabase() throws SQLException, IOException {
        if (mDatabase != null) {
            return mDatabase;
        }

        if (!this.hasDatabase()) {
            this.copyDatabase();
        }

        mDatabase = SQLiteDatabase.openDatabase(new File(DB_PATH, DB_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);
        return mDatabase;
    }

    public synchronized void close() {
        if (mDatabase != null)
            this.mDatabase.close();
    }

    private boolean hasDatabase() {
        SQLiteDatabase tempDatabase = null;

        File file = new File(DB_PATH, DB_NAME);
        if (!file.exists()) {
            return false;
        }

        try {
            tempDatabase = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            int currentVersion = tempDatabase.getVersion();
            if (currentVersion != VERSION) {
                Log.d(TAG, String.format("Updating OVChip database. Old: %s, new: %s", currentVersion, VERSION));
                tempDatabase.close();
                tempDatabase = null;
            }
        } catch (SQLiteException ignored) { }

        if (tempDatabase != null){
            tempDatabase.close();
        }

        return (tempDatabase != null);
    }

    private void copyDatabase() {
        InputStream in   = null;
        OutputStream out = null;
        try {
            in  = this.mContext.getAssets().open(DB_NAME);
            out = new FileOutputStream(new File(DB_PATH, DB_NAME));
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Error copying database", e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }
}