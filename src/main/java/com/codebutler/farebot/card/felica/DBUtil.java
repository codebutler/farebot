/*
 * DBUtil.java
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
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
 */

package com.codebutler.farebot.card.felica;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class DBUtil {
    public static final String COLUMN_ID             = "_id";
    public static final String COLUMN_AREACODE       = "AreaCode";
    public static final String COLUMN_LINECODE       = "LineCode";
    public static final String COLUMN_STATIONCODE    = "StationCode";
    public static final String COLUMN_COMPANYNAME    = "CompanyName";
    public static final String COLUMN_LINENAME       = "LineName";
    public static final String COLUMN_STATIONNAME    = "StationName";
    public static final String COLUMN_COMPANYNAME_EN = "CompanyName_en";
    public static final String COLUMN_LINENAME_EN    = "LineName_en";
    public static final String COLUMN_STATIONNAME_EN = "StationName_en";
    public static final String COLUMN_LATITUDE       = "Latitude";
    public static final String COLUMN_LONGITUDE      = "Longitude";

    public static final String TABLE_STATIONCODE = "StationCode";
    public static final String[] COLUMNS_STATIONCODE = {
        COLUMN_AREACODE,
        COLUMN_LINECODE,
        COLUMN_STATIONCODE,
        COLUMN_COMPANYNAME,
        COLUMN_LINENAME,
        COLUMN_STATIONNAME,
        COLUMN_COMPANYNAME_EN,
        COLUMN_LINENAME_EN,
        COLUMN_STATIONNAME_EN,
        COLUMN_LATITUDE,
        COLUMN_LONGITUDE
    };

    public static final String TABLE_IRUCA_STATIONCODE = "IruCaStationCode";
    public static final String[] COLUMNS_IRUCA_STATIONCODE = {
        COLUMN_LINECODE,
        COLUMN_STATIONCODE,
        COLUMN_COMPANYNAME,
        COLUMN_LINENAME,
        COLUMN_STATIONNAME,
        COLUMN_COMPANYNAME_EN,
        COLUMN_LINENAME_EN,
        COLUMN_STATIONNAME_EN
    };

    private static final String TAG = "SuicaDBUtil";

    private static final String DB_PATH = "/data/data/com.codebutler.farebot/databases/";
    private static final String DB_NAME = "StationCode.db";

    private static final int VERSION = 2;
    
    private SQLiteDatabase mDatabase;
    private final Context mContext;

    public DBUtil(Context context) {
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
                Log.d(TAG, String.format("Updating Suica database. Old: %s, new: %s", currentVersion, VERSION));
                tempDatabase.close();
                tempDatabase = null;
            }
        } catch (SQLiteException ignored) {
        }

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
