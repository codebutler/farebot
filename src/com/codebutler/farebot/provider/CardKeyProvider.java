/*
 * CardKeyProvider.java
 *
 * Copyright (C) 2012 Eric Butler
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

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.Date;

public class CardKeyProvider extends BetterContentProvider {
    public static final String AUTHORITY   = "com.codebutler.farebot.keyprovider";
    public static final Uri    CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/keys");

    public CardKeyProvider() {
        super(
            KeysDBHelper.class,
            KeysDBHelper.KEY_DIR_TYPE,
            KeysDBHelper.KEY_ITEM_TYPE,
            KeysTableColumns.TABLE_NAME,
            CONTENT_URI
        );
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long now = new Date().getTime();
        values.put(KeysTableColumns.CREATED_AT, now);
        return super.insert(uri, values);
    }

    @Override
    protected UriMatcher createUriMatcher(Uri contentUri, String basePath) {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(contentUri.getAuthority(), basePath,        CODE_COLLECTION);
        matcher.addURI(contentUri.getAuthority(), basePath + "/*", CODE_SINGLE);
        return matcher;
    }

    @Override
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
