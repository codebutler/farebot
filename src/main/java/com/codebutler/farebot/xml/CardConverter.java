/*
 * CardConverter.java
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

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.ultralight.UltralightCard;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class CardConverter implements Converter<Card> {
    private final Serializer mSerializer;

    public CardConverter(Serializer serializer) {
        mSerializer = serializer;
    }

    @Override
    public Card read(InputNode node) throws Exception {
        CardType type = CardType.parseValue(node.getAttribute("type").getValue());
        switch (type) {
            case MifareDesfire:
                return mSerializer.read(DesfireCard.class, node);
            case CEPAS:
                return mSerializer.read(CEPASCard.class, node);
            case FeliCa:
                return mSerializer.read(FelicaCard.class, node);
            case MifareClassic:
                return mSerializer.read(ClassicCard.class, node);
            case MifareUltralight:
                return mSerializer.read(UltralightCard.class, node);
            default:
                throw new UnsupportedOperationException("Unsupported card type: " + type);
        }
    }

    @Override
    public void write(OutputNode node, Card value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
