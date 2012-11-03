/*
 * UnsupportedTagException.java
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

package com.codebutler.farebot;

public class UnsupportedTagException extends Exception {
    private String[] mTechList;
    private String   mTagId;

    public UnsupportedTagException (String[] techList, String tagId) {
        mTechList = techList;
        mTagId = tagId;
    }

    public String[] getTechList () {
        return mTechList;
    }

    public String getTagId () {
        return mTagId;
    }

    @Override
    public String getMessage () {
        StringBuilder builder = new StringBuilder();
        for (String tech : mTechList) {
            builder.append("\n  ");
            builder.append(tech.replace("android.nfc.tech.", ""));
        }
        return String.format("Identifier: %s\n\nTechnologies:%s", mTagId, builder.toString());
    }
}
