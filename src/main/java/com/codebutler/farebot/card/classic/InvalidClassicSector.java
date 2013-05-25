/*
 * InvalidClassicSector.java
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

package com.codebutler.farebot.card.classic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class InvalidClassicSector extends ClassicSector {
    private String mError;

    public InvalidClassicSector(int index, String error) {
        super(index, null);
        mError = error;
    }

    public Element toXML(Document doc) {
        Element sectorElement = doc.createElement("sector");
        sectorElement.setAttribute("index", String.valueOf(getIndex()));
        sectorElement.setAttribute("invalid", "true");
        sectorElement.setAttribute("error", mError);
        return sectorElement;
    }

    public String getError() {
        return mError;
    }
}
