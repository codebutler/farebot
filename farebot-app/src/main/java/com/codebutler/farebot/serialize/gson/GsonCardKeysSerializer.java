package com.codebutler.farebot.serialize.gson;

import android.support.annotation.NonNull;

import com.codebutler.farebot.key.CardKeys;
import com.codebutler.farebot.serialize.CardKeysSerializer;
import com.google.gson.Gson;

public class GsonCardKeysSerializer implements CardKeysSerializer {

    @NonNull private final Gson mGson;

    public GsonCardKeysSerializer(@NonNull Gson gson) {
        mGson = gson;
    }

    @NonNull
    @Override
    public String serialize(@NonNull CardKeys cardKeys) {
        return mGson.toJson(cardKeys);
    }

    @NonNull
    @Override
    public CardKeys deserialize(@NonNull String data) {
        return mGson.fromJson(data, CardKeys.class);
    }
}
