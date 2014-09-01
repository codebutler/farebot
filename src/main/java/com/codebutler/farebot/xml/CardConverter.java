package com.codebutler.farebot.xml;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.felica.FelicaCard;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class CardConverter implements Converter<Card> {
    private final Serializer mSerializer;

    public CardConverter(Serializer serializer) {
        mSerializer = serializer;
    }

    @Override public Card read(InputNode node) throws Exception {
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
            default:
                throw new UnsupportedOperationException("Unsupported card type: " + type);
        }
    }

    @Override public void write(OutputNode node, Card value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
