/*
 * ListItem.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.core.ui;

import android.support.annotation.Nullable;

public class ListItem {

    @Nullable private final String mText1;
    @Nullable private final String mText2;

    public ListItem(@Nullable String text1, @Nullable String text2) {
        mText1 = text1;
        mText2 = text2;
    }

    @Nullable
    public String getText1() {
        return mText1;
    }

    @Nullable
    public String getText2() {
        return mText2;
    }
}
