package com.codebutler.farebot.card.classic;

import androidx.annotation.NonNull;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class ClassicTypeAdapterFactory implements TypeAdapterFactory {

    @NonNull
    public static ClassicTypeAdapterFactory create() {
        return new AutoValueGson_ClassicTypeAdapterFactory();
    }
}
