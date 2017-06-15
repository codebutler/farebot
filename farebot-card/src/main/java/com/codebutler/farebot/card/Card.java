/*
 * Card.java
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

package com.codebutler.farebot.card;

import android.content.Context;
import android.support.annotation.NonNull;

import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.codebutler.farebot.base.util.ByteArray;

import java.util.Date;

public abstract class Card {

    @NonNull
    public abstract CardType getCardType();

    @NonNull
    public abstract ByteArray getTagId();

    @NonNull
    public abstract Date getScannedAt();

    @NonNull
    public abstract FareBotUiTree getAdvancedUi(Context context);

    @NonNull
    public Class<? extends Card> getParentClass() {
        Class<? extends Card> aClass = getClass();
        while (aClass.getSuperclass() != Card.class) {
            aClass = (Class<? extends Card>) aClass.getSuperclass();
        }
        return aClass;
    }
}
