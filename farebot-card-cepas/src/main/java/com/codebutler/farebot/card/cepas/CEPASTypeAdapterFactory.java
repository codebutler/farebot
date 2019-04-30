package com.codebutler.farebot.card.cepas;

import androidx.annotation.NonNull;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class CEPASTypeAdapterFactory implements TypeAdapterFactory {

    @NonNull
    public static CEPASTypeAdapterFactory create() {
        return new AutoValueGson_CEPASTypeAdapterFactory();
    }
}
