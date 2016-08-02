package com.codebutler.farebot.serialize;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.RawCard;
import com.google.gson.Gson;

public class CardJsonSerializer implements CardSerializer {

    @NonNull private final Gson mGson;

    public CardJsonSerializer(@NonNull Gson gson) {
        mGson = gson;
    }

    @Override
    @NonNull
    public String serialize(@NonNull RawCard card) {
        return mGson.toJson(card);
    }

    @Override
    @NonNull
    public RawCard deserialize(@NonNull String json) {
        return mGson.fromJson(json, RawCard.class);
    }
}
