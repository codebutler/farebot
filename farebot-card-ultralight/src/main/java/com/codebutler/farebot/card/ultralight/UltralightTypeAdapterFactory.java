package com.codebutler.farebot.card.ultralight;

import android.support.annotation.NonNull;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class UltralightTypeAdapterFactory implements TypeAdapterFactory {

    @NonNull
    public static UltralightTypeAdapterFactory create() {
        return new AutoValueGson_UltralightTypeAdapterFactory();
    }
}
