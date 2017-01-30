package com.codebutler.farebot.card.cepas;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class CEPASTypeAdapterFactory implements TypeAdapterFactory {

    @NonNull
    public static CEPASTypeAdapterFactory create() {
        return new AutoValueGson_CEPASTypeAdapterFactory();
    }
}
