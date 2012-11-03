/*
 * CardsTableColumns.java
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

import android.provider.BaseColumns;

public class CardsTableColumns implements BaseColumns {
    public static final String TABLE_NAME = "cards";

    public static final String TYPE       = "type";
    public static final String TAG_SERIAL = "serial";
    public static final String DATA       = "data";
    public static final String SCANNED_AT = "scanned_at";
}
