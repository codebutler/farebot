/*
 * DBUtil.java
 *
 * Copyright (C) 2015 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.core;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract common stop database class.
 */
public abstract class DBUtil {

    private static final String TAG = "DBUtil";

    private SQLiteDatabase mDatabase;
    private final Context mContext;

    protected DBUtil(Context context) {
        mContext = context;
    }

    /**
     * Implementing classes should specify the filename of their database.
     *
     * @return Path, relative to FareBot's data folder, where to store the database file.
     */
    protected abstract String getDBName();

    /**
     * Implementing classes should specify what the target version of database they should expect.
     *
     * @return The desired database version, as defined in PRAGMA user_version
     */
    protected abstract int getDesiredVersion();

    /**
     * If set to true, this will allow a database which has a greater PRAGMA user_version to
     * satisfy the database requirements.
     * <p>
     * If set to false, the database version (PRAGMA user_version) must be exactly the same as the
     * return value of getDesiredVersion().
     *
     * @return true if exact match is required, false if it just must be at minimum this number.
     */
    private boolean allowGreaterDatabaseVersions() {
        return false;
    }

    public SQLiteDatabase openDatabase() throws SQLException, IOException {
        if (mDatabase != null) {
            return mDatabase;
        }

        if (!this.hasDatabase()) {
            this.copyDatabase();
        }

        mDatabase = SQLiteDatabase.openDatabase(getDBFile().getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
        return mDatabase;
    }

    public synchronized void close() {
        if (mDatabase != null) {
            this.mDatabase.close();
        }
    }

    private boolean hasDatabase() {
        SQLiteDatabase tempDatabase = null;

        File file = getDBFile();
        if (!file.exists()) {
            Log.d(TAG, String.format("Database for %s does not exist, will install version %s",
                    getDBName(), getDesiredVersion()));
            return false;
        }

        try {
            tempDatabase = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            int currentVersion = tempDatabase.getVersion();
            if (allowGreaterDatabaseVersions()
                    ? currentVersion < getDesiredVersion()
                    : currentVersion != getDesiredVersion()) {
                Log.d(TAG, String.format("Updating %s database. Old: %s, new: %s", getDBName(), currentVersion,
                        getDesiredVersion()));
                tempDatabase.close();
                tempDatabase = null;
            } else {
                Log.d(TAG, String.format("Not updating %s database. Current: %s, app has: %s", getDBName(),
                        currentVersion, getDesiredVersion()));
            }
        } catch (SQLiteException ignored) { }

        if (tempDatabase != null) {
            tempDatabase.close();
        }

        return (tempDatabase != null);
    }

    private void copyDatabase() {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = this.mContext.getAssets().open(getDBName());
            out = new FileOutputStream(getDBFile());
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Error copying database", e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    private File getDBFile() {
        return new File(mContext.getCacheDir().getAbsolutePath() + "/" + getDBName());
    }
}
