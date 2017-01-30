package com.codebutler.farebot.card.desfire;

import android.support.annotation.NonNull;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class DesfireTypeAdapterFactory implements TypeAdapterFactory {

    @NonNull
    public static DesfireTypeAdapterFactory create() {
        return new AutoValueGson_DesfireTypeAdapterFactory();
    }
}
