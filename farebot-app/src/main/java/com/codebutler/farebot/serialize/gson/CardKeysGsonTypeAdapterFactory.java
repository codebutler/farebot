package com.codebutler.farebot.serialize.gson;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.classic.key.ClassicCardKeys;
import com.codebutler.farebot.key.CardKeys;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CardKeysGsonTypeAdapterFactory implements TypeAdapterFactory {

    static final Map<CardType, Class<? extends CardKeys>> CLASSES
            = ImmutableMap.<CardType, Class<? extends CardKeys>>builder()
            .put(CardType.MifareClassic, ClassicCardKeys.class)
            .build();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!CardKeys.class.isAssignableFrom(type.getRawType())) {
            return null;
        }
        final Map<CardType, TypeAdapter<CardKeys>> delegates = new HashMap<>();
        for (Map.Entry<CardType, Class<? extends CardKeys>> entry : CLASSES.entrySet()) {
            TypeAdapter<CardKeys> delegateAdapter
                    = (TypeAdapter<CardKeys>) gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            delegates.put(entry.getKey(), delegateAdapter);
        }
        return (TypeAdapter<T>) new CardKeysTypeAdapter(delegates);
    }

    private class CardKeysTypeAdapter extends TypeAdapter<CardKeys> {

        private static final String KEY_CARD_TYPE = "cardType";

        @NonNull private final Map<CardType, TypeAdapter<CardKeys>> mDelegates;

        CardKeysTypeAdapter(Map<CardType, TypeAdapter<CardKeys>> delegates) {
            mDelegates = delegates;
        }

        @Override
        public void write(JsonWriter out, CardKeys value) throws IOException {
            TypeAdapter<CardKeys> delegateAdapter = mDelegates.get(value.cardType());
            JsonObject jsonObject = delegateAdapter.toJsonTree(value).getAsJsonObject();
            Streams.write(jsonObject, out);
        }

        @Override
        public CardKeys read(JsonReader in) throws IOException {
            JsonElement rootElement = Streams.parse(in);
            JsonElement typeElement = rootElement.getAsJsonObject().remove(KEY_CARD_TYPE);
            CardType cardType = CardType.valueOf(typeElement.getAsString());
            TypeAdapter<CardKeys> delegateAdapter = mDelegates.get(cardType);
            return delegateAdapter.fromJsonTree(rootElement);
        }
    }
}
