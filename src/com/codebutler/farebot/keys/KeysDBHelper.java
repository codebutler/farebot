/*
 * KeysDBHelper.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.keys;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class KeysDBHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_PATH = "/data/data/com.codebutler.farebot/databases/";
    private static final String DATABASE_NAME = "keys.db";
    private static final int DATABASE_VERSION = 2;
	private static final String TABLE_NAME = "keys";
	private static final String COLUMN_CARDID = "card_id";
	private static final String COLUMN_SECTOR = "sector";
	private static final String COLUMN_TYPE = "type";
	private static final String COLUMN_KEY = "key";

	private static final String[] COLUMNS_KEYS = {
		COLUMN_CARDID,
		COLUMN_SECTOR,
		COLUMN_TYPE,
		COLUMN_KEY
    };

    private SQLiteDatabase mDatabase;

    public KeysDBHelper (Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void open() throws SQLException {
		if (mDatabase == null || !mDatabase.isOpen()) {
			mDatabase = getWritableDatabase();
		}
	}

    @Override
    public void onCreate (SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
        + COLUMN_CARDID + " TEXT NOT NULL, "
        + COLUMN_SECTOR +  " TEXT NOT NULL, "
        + COLUMN_TYPE +    " TEXT NOT NULL, "
        + COLUMN_KEY +     " TEXT NOT NULL);");
    }

    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {}

    public SQLiteDatabase openDatabase() throws SQLException, IOException {
        if (mDatabase != null) {
            return mDatabase;
        }

        mDatabase = SQLiteDatabase.openDatabase(new File(DATABASE_PATH, DATABASE_NAME).getPath(), null, SQLiteDatabase.OPEN_READONLY);
        return mDatabase;
    }

    public synchronized void close() {
        if (mDatabase != null)
            this.mDatabase.close();
    }

    public void insertKey(String card_id, int sector, String type, String key) {
		String sql = "INSERT INTO " + TABLE_NAME + " VALUES ('"
					+ card_id + "', '"
					+ sector + "', '"
					+ type + "', '"
					+ key + "');";

		mDatabase.execSQL(sql);
	}

    public String getKey(String card_id, int sector, String type) {
    	Cursor cursor = mDatabase.query(
        		TABLE_NAME,
        		COLUMNS_KEYS,
             String.format("%s = ? AND %s = ? AND %s = ?", COLUMN_CARDID, COLUMN_SECTOR, COLUMN_TYPE),
             new String[] {
        			card_id,
        			String.valueOf(sector),
        			type
             },
             null,
             null,
             COLUMN_CARDID);

        if (!cursor.moveToFirst()) {
            Log.w("KeysDBHelper", "FAILED get key: c: " + card_id + " s: " + sector + " t: " + type);

            return null;
        }

        String key = cursor.getString(cursor.getColumnIndex(COLUMN_KEY));

        return key;
	}
}