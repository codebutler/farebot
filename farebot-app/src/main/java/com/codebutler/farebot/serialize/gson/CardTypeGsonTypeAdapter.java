package com.codebutler.farebot.serialize.gson;

import com.codebutler.farebot.card.CardType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class CardTypeGsonTypeAdapter extends TypeAdapter<CardType> {

    @Override
    public void write(JsonWriter out, CardType value) throws IOException {
        out.value(value.name());
    }

    @Override
    public CardType read(JsonReader in) throws IOException {
        return CardType.valueOf(in.nextString());
    }
}
