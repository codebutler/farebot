package com.codebutler.farebot;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard;
import com.codebutler.farebot.card.classic.raw.RawClassicCard;
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard;
import com.codebutler.farebot.card.felica.raw.RawFelicaCard;
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RawCardTypeAdapterFactory implements TypeAdapterFactory {

    static final Map<CardType, Class<? extends RawCard>> CLASSES
            = ImmutableMap.<CardType, Class<? extends RawCard>>builder()
            .put(CardType.MifareDesfire, RawDesfireCard.class)
            .put(CardType.MifareClassic, RawClassicCard.class)
            .put(CardType.MifareUltralight, RawUltralightCard.class)
            .put(CardType.CEPAS, RawCEPASCard.class)
            .put(CardType.FeliCa, RawFelicaCard.class)
            .build();

    static final String KEY_CARD_TYPE = "cardType";

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(final Gson gson, TypeToken<T> type) {
        if (!RawCard.class.isAssignableFrom((Class) type.getRawType())) {
            return null;
        }
        final Map<CardType, TypeAdapter<RawCard>> delegates = new HashMap<>();
        for (Map.Entry<CardType, Class<? extends RawCard>> entry : CLASSES.entrySet()) {
            TypeAdapter<RawCard> delegateAdapter
                    = (TypeAdapter<RawCard>) gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            delegates.put(entry.getKey(), delegateAdapter);
        }
        return (TypeAdapter<T>) new RawCardTypeAdapter(delegates);
    }

    private static class RawCardTypeAdapter extends TypeAdapter<RawCard> {

        @NonNull private final Map<CardType, TypeAdapter<RawCard>> mDelegates;

        RawCardTypeAdapter(@NonNull Map<CardType, TypeAdapter<RawCard>> delegates) {
            mDelegates = delegates;
        }

        @Override
        public void write(JsonWriter out, RawCard value) throws IOException {
            TypeAdapter<RawCard> delegateAdapter = mDelegates.get(value.cardType());
            JsonObject jsonObject = delegateAdapter.toJsonTree(value).getAsJsonObject();
            jsonObject.add(KEY_CARD_TYPE, new JsonPrimitive(value.cardType().name()));
            Streams.write(jsonObject, out);
        }

        @Override
        public RawCard read(JsonReader in) throws IOException {
            JsonElement rootElement = Streams.parse(in);
            JsonElement typeElement = rootElement.getAsJsonObject().remove(KEY_CARD_TYPE);
            CardType cardType = Enum.valueOf(CardType.class, typeElement.getAsString());
            TypeAdapter<RawCard> delegateAdapter = mDelegates.get(cardType);
            return delegateAdapter.fromJsonTree(rootElement);
        }
    }
}
