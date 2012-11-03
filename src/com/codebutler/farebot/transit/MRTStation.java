/*
 * MRTStation.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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

package com.codebutler.farebot.transit;

public class MRTStation extends Station {
    private final String mCode;
    private final String mAbbreviation;

    public MRTStation (String name, String code, String abbreviation, String latitude, String longitude) {
        super(name, latitude, longitude);

        mCode         = code;
        mAbbreviation = abbreviation;
    }
    
    public String getCode () {
        return mCode;
    }
    
    public String getAbbreviation () {
        return mAbbreviation;
    }
}
