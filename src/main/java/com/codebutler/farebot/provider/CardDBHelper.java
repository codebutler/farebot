/*
 * CardDBHelper.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
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

package com.codebutler.farebot.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.codebutler.farebot.BuildConfig;

public class CardDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "cards.db";
    public static final int DATABASE_VERSION = 3;

    public static final int CARD_COLLECTION_URI_INDICATOR = 1;
    public static final int SINGLE_CARD_URI_INDICATOR = 2;

    public static final String CARD_DIR_TYPE  = "vnd.android.cursor.dir/" + BuildConfig.APPLICATION_ID + ".card";
    public static final String CARD_ITEM_TYPE = "vnd.android.cursor.item/" + BuildConfig.APPLICATION_ID + ".card";

    public static final String[] PROJECTION = new String[] {
        CardsTableColumns._ID,
        CardsTableColumns.TYPE,
        CardsTableColumns.TAG_SERIAL,
        CardsTableColumns.DATA,
        CardsTableColumns.SCANNED_AT,
        CardsTableColumns.NICKNAME
    };

    public static Cursor createCursor(Context context) {
        return context.getContentResolver().query(CardProvider.CONTENT_URI_CARD,
            PROJECTION,
            null,
            null,
            CardsTableColumns.SCANNED_AT + " DESC, " + CardsTableColumns._ID + " DESC");
    }

    public CardDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cards ("
        + "_id        INTEGER PRIMARY KEY, "
        + "type       TEXT NOT NULL, "
        + "serial     TEXT NOT NULL, "
        + "data       BLOB NOT NULL, "
        + "scanned_at LONG, "
        + "nickname   TEXT"
        + ");");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && (newVersion == 2 || newVersion == 3)) {
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE cards RENAME TO cards_old");
                onCreate(db);
                db.execSQL("INSERT INTO cards (type, serial, data) SELECT type, serial, data from cards_old");
                db.execSQL("DROP TABLE cards_old");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return;
        } else if (oldVersion == 2 && newVersion == 3) {
            db.beginTransaction();
            try{
                db.execSQL("ALTER TABLE cards RENAME TO cards_old");
                onCreate(db);
                db.execSQL("INSERT INTO cards (_id, type, serial, data, scanned_at) SELECT _id, type, serial, data, scanned_at from cards_old");
                db.execSQL("DROP TABLE cards_old");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return;
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
