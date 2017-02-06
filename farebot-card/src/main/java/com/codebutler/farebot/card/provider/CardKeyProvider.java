/*
 * CardKeyProvider.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Date;

public class CardKeyProvider extends ContentProvider {

    static final int CODE_COLLECTION = 100;
    static final int CODE_SINGLE = 101;

    private SQLiteOpenHelper mDbHelper;
    private UriMatcher mUriMatcher;

    @NonNull
    public static String getAuthority(@NonNull Context context) {
        return context.getPackageName() + ".keyprovider";
    }

    @NonNull
    public static Uri getContentUri(@NonNull Context context) {
        return Uri.parse("content://" + getAuthority(context) + "/keys");
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new KeysDBHelper(getContext());

        String authority = getAuthority(getContext());

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(authority, "keys", CODE_COLLECTION);
        mUriMatcher.addURI(authority, "keys/#", CODE_SINGLE);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case CODE_SINGLE:
                String rowId = uri.getPathSegments().get(1);
                if (TextUtils.isEmpty(selection)) {
                    count = db.delete(KeysTableColumns.TABLE_NAME, BaseColumns._ID + "=?", new String[]{rowId});
                } else {
                    count = db.delete(KeysTableColumns.TABLE_NAME,
                            selection + " AND " + BaseColumns._ID + "=" + rowId,
                            selectionArgs);
                }
                break;
            case CODE_COLLECTION:
                count = db.delete(KeysTableColumns.TABLE_NAME, selection, selectionArgs);
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        String packageName = getContext().getPackageName();
        switch (mUriMatcher.match(uri)) {
            case CODE_COLLECTION:
                return "vnd.android.cursor.dir/" + packageName + ".key";
            case CODE_SINGLE:
                return "vnd.android.cursor.item/" + packageName + ".key";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long now = new Date().getTime();
        values.put(KeysTableColumns.CREATED_AT, now);
        return insert(uri, values);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(KeysTableColumns.TABLE_NAME);
        appendWheres(builder, mUriMatcher, uri);

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = builder.query(db, null, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        switch (mUriMatcher.match(uri)) {
            case CODE_COLLECTION:
                count = db.update(KeysTableColumns.TABLE_NAME, values, selection, selectionArgs);
                break;
            case CODE_SINGLE:
                String rowId = uri.getPathSegments().get(1);
                if (TextUtils.isEmpty(selection)) {
                    count = db.update(KeysTableColumns.TABLE_NAME, values, BaseColumns._ID + "=" + rowId, null);
                } else {
                    count = db.update(KeysTableColumns.TABLE_NAME, values,
                            selection + " AND " + BaseColumns._ID + "=" + rowId,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    protected void appendWheres(SQLiteQueryBuilder builder, UriMatcher matcher, Uri uri) {
        switch (matcher.match(uri)) {
            case CODE_COLLECTION:
                // Nothing needed here
                break;
            case CODE_SINGLE:
                // FIXME: Prevent sql injection.
                builder.appendWhere(KeysTableColumns.CARD_ID + "= \"" + uri.getPathSegments().get(1) + "\"");
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
