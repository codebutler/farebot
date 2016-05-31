/*
 * Base64String.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.xml;

import android.util.Base64;

public class Base64String {
    private final byte[] mData;

    public Base64String(byte[] data) {
        mData = data;
    }

    public Base64String(String data) {
        mData = Base64.decode(data, Base64.DEFAULT);
    }

    public byte[] getData() {
        return mData;
    }

    private String toBase64() {
        return Base64.encodeToString(mData, Base64.NO_WRAP);
    }

    public static final class Transform implements org.simpleframework.xml.transform.Transform<Base64String> {
        @Override
        public Base64String read(String value) throws Exception {
            return new Base64String(value);
        }

        @Override
        public String write(Base64String value) throws Exception {
            return value.toBase64();
        }
    }
}
