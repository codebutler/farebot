/*
 * CardProvider.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class CardProvider extends ContentProvider {

    private CardDBHelper mDbHelper;
    private UriMatcher mUriMatcher;

    @NonNull
    public static String getAuthority(@NonNull Context context) {
        return context.getPackageName() + ".cardprovider";
    }

    @NonNull
    public static Uri getContentUri(@NonNull Context context) {
        return Uri.parse("content://" + getAuthority(context) + "/cards");
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new CardDBHelper(getContext());

        String authority = getAuthority(getContext());

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(authority, "cards", CardDBHelper.CARD_COLLECTION_URI_INDICATOR);
        mUriMatcher.addURI(authority, "cards/#", CardDBHelper.SINGLE_CARD_URI_INDICATOR);

        return true;
    }

    @Override
    public Cursor query(
            @NonNull Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        switch (mUriMatcher.match(uri)) {
            case CardDBHelper.CARD_COLLECTION_URI_INDICATOR:
                builder.setTables(CardsTableColumns.TABLE_NAME);
                //builder.setProjectionMap();
                break;
            case CardDBHelper.SINGLE_CARD_URI_INDICATOR:
                builder.setTables(CardsTableColumns.TABLE_NAME);
                builder.appendWhere(CardsTableColumns._ID + " = " + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = builder.query(db, null, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        String packageName = getContext().getPackageName();
        switch (mUriMatcher.match(uri)) {
            case CardDBHelper.CARD_COLLECTION_URI_INDICATOR:
                return "vnd.android.cursor.dir/" + packageName + ".card";
            case CardDBHelper.SINGLE_CARD_URI_INDICATOR:
                return "vnd.android.cursor.item/" + packageName + ".card";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (mUriMatcher.match(uri) != CardDBHelper.CARD_COLLECTION_URI_INDICATOR) {
            throw new IllegalArgumentException("Incorrect URI: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long rowId = db.insertOrThrow(CardsTableColumns.TABLE_NAME, null, values);

        Uri cardUri = ContentUris.withAppendedId(getContentUri(getContext()), rowId);
        getContext().getContentResolver().notifyChange(cardUri, null);

        return cardUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case CardDBHelper.CARD_COLLECTION_URI_INDICATOR:
                count = db.delete(CardsTableColumns.TABLE_NAME, selection, selectionArgs);
                break;
            case CardDBHelper.SINGLE_CARD_URI_INDICATOR:
                String rowId = uri.getPathSegments().get(1);
                count = db.delete(CardsTableColumns.TABLE_NAME,
                        CardsTableColumns._ID + "=" + rowId
                                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        switch (mUriMatcher.match(uri)) {
            case CardDBHelper.CARD_COLLECTION_URI_INDICATOR:
                count = db.update(CardsTableColumns.TABLE_NAME, values, selection, selectionArgs);
                break;
            case CardDBHelper.SINGLE_CARD_URI_INDICATOR:
                String rowId = uri.getPathSegments().get(1);
                count = db.update(CardsTableColumns.TABLE_NAME,
                        values,
                        CardsTableColumns._ID + "=" + rowId
                                + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
