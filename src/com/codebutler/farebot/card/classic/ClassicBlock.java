/*
 * ClassicBlock.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic;

import android.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ClassicBlock {
    public static final String TYPE_DATA         = "data";
    public static final String TYPE_VALUE        = "value";
    public static final String TYPE_TRAILER      = "trailer";
    public static final String TYPE_MANUFACTURER = "manufacturer";

    private final int    mIndex;
    private final String mType;
    private final byte[] mData;

    public static ClassicBlock create(String type, int index, byte[] data) {
        if (type.equals(TYPE_DATA) || type.equals(TYPE_VALUE)) {
            return new ClassicBlock(index, type, data);
        }
        return null;
    }

    public ClassicBlock(int index, String type, byte[] data) {
        mIndex = index;
        mType  = type;
        mData  = data;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getType() {
        return mType;
    }

    public byte[] getData() {
        return mData;
    }

    public Element toXML(Document doc) {
        Element blockElement = doc.createElement("block");
        blockElement.setAttribute("index", String.valueOf(getIndex()));
        blockElement.setAttribute("type", getType());

        Element dataElement = doc.createElement("data");
        dataElement.setTextContent(Base64.encodeToString(getData(), Base64.DEFAULT));
        blockElement.appendChild(dataElement);

        return blockElement;
    }
}
