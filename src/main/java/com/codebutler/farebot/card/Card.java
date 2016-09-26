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

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitFactoryRegistry;
import com.codebutler.farebot.transit.TransitIdentity;

import java.util.Date;

public abstract class Card implements Parcelable {

    @NonNull
    public abstract CardType getCardType();

    @NonNull
    public abstract ByteArray getTagId();

    @NonNull
    public abstract Date getScannedAt();

    @Nullable
    public TransitIdentity parseTransitIdentity(@NonNull TransitFactoryRegistry registry) {
        for (TransitFactory factory : registry.getFactories(getParentClass())) {
            if (factory.check(this)) {
                return factory.parseIdentity(this);
            }
        }
        return null;
    }

    @Nullable
    public TransitData parseTransitData(@NonNull TransitFactoryRegistry registry) {
        for (TransitFactory factory : registry.getFactories(getParentClass())) {
            if (factory.check(this)) {
                return factory.parseData(this);
            }
        }
        return null;
    }

    @NonNull
    private Class<? extends Card> getParentClass() {
        Class<? extends Card> aClass = getClass();
        while (aClass.getSuperclass() != Card.class) {
            aClass = (Class<? extends Card>) aClass.getSuperclass();
        }
        return aClass;
    }
}
