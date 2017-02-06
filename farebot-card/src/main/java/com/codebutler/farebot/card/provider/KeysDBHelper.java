/*
 * KeysDBHelper.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2015-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class KeysDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "keys.db";
    private static final int DATABASE_VERSION = 3;

    KeysDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + KeysTableColumns.TABLE_NAME + " ("
                + KeysTableColumns._ID + " INTEGER PRIMARY KEY, "
                + KeysTableColumns.CARD_ID + " TEXT NOT NULL, "
                + KeysTableColumns.CARD_TYPE + " TEXT NOT NULL, "
                + KeysTableColumns.KEY_DATA + " BLOB NOT NULL, "
                + KeysTableColumns.CREATED_AT + " LONG NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not Implemented...
    }
}
