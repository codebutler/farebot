package com.codebutler.farebot.card.cepas.raw;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.cepas.CEPASHistory;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RawCEPASHistory {

    @NonNull
    public static RawCEPASHistory create(int id, @NonNull byte[] data) {
        return new AutoValue_RawCEPASHistory(id, ByteArray.create(data), null);
    }

    @NonNull
    public static RawCEPASHistory create(int id, @NonNull String errorMessage) {
        return new AutoValue_RawCEPASHistory(id, null, errorMessage);
    }

    @NonNull
    public static TypeAdapter<RawCEPASHistory> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawCEPASHistory.GsonTypeAdapter(gson);
    }

    @NonNull
    public CEPASHistory parse() {
        ByteArray data = data();
        if (data != null) {
            return CEPASHistory.create(id(), data.bytes());
        }
        return CEPASHistory.create(id(), errorMessage());
    }

    abstract int id();

    @Nullable
    abstract ByteArray data();

    @Nullable
    abstract String errorMessage();
}
