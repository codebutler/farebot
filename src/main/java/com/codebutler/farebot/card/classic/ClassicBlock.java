/*
 * ClassicBlock.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic;

import com.codebutler.farebot.xml.Base64String;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="block")
public class ClassicBlock {
    public static final String TYPE_DATA         = "data";
    public static final String TYPE_VALUE        = "value";
    public static final String TYPE_TRAILER      = "trailer";
    public static final String TYPE_MANUFACTURER = "manufacturer";

    @Attribute(name="index") private int mIndex;
    @Attribute(name="type") private String mType;
    @Element(name="data") private Base64String mData;

    public static ClassicBlock create(String type, int index, byte[] data) {
        if (type.equals(TYPE_DATA) || type.equals(TYPE_VALUE)) {
            return new ClassicBlock(index, type, data);
        }
        return null;
    }

    public ClassicBlock() { }

    public ClassicBlock(int index, String type, byte[] data) {
        mIndex = index;
        mType = type;
        mData = new Base64String(data);
    }

    public int getIndex() {
        return mIndex;
    }

    public String getType() {
        return mType;
    }

    public byte[] getData() {
        return mData.getData();
    }
}
