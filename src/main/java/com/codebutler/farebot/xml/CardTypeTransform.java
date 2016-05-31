/*
 * CardTypeTransform.java
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

import com.codebutler.farebot.card.CardType;

import org.simpleframework.xml.transform.Transform;

public class CardTypeTransform implements Transform<CardType> {
    @Override
    public CardType read(String value) throws Exception {
        return CardType.class.getEnumConstants()[Integer.parseInt(value)];
    }

    @Override
    public String write(CardType value) throws Exception {
        return String.valueOf(value.toInteger());
    }
}
