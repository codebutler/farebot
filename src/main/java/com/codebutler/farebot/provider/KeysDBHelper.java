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

package com.codebutler.farebot.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KeysDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "keys.db";
    private static final int DATABASE_VERSION = 3;

    public static final String KEY_DIR_TYPE  = "vnd.android.cursor.dir/com.codebutler.farebot.key";
    public static final String KEY_ITEM_TYPE = "vnd.android.cursor.item/com.codebutler.farebot.key";

    public KeysDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + KeysTableColumns.TABLE_NAME + " ("
            + KeysTableColumns._ID        + " INTEGER PRIMARY KEY, "
            + KeysTableColumns.CARD_ID    + " TEXT NOT NULL, "
            + KeysTableColumns.CARD_TYPE  + " TEXT NOT NULL, "
            + KeysTableColumns.KEY_DATA   + " BLOB NOT NULL, "
            + KeysTableColumns.CREATED_AT + " LONG NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not Implemented...
    }
}