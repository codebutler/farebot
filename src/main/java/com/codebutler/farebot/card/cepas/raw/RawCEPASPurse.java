package com.codebutler.farebot.card.cepas.raw;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.cepas.CEPASPurse;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RawCEPASPurse {

    @NonNull
    public static RawCEPASPurse create(int id, byte[] data) {
        return new AutoValue_RawCEPASPurse(id, ByteArray.create(data), null);
    }

    @NonNull
    public static RawCEPASPurse create(int id, String errorMessage) {
        return new AutoValue_RawCEPASPurse(id, null, errorMessage);
    }

    @NonNull
    public static TypeAdapter<RawCEPASPurse> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawCEPASPurse.GsonTypeAdapter(gson);
    }

    public boolean isValid() {
        return data() != null;
    }

    @NonNull
    public CEPASPurse parse() {
        if (isValid()) {
            return CEPASPurse.create(id(), data().bytes());
        }
        return CEPASPurse.create(id(), errorMessage());
    }

    public byte logfileRecordCount() {
        return data().bytes()[40];
    }

    abstract int id();

    @Nullable
    abstract ByteArray data();

    @Nullable
    abstract String errorMessage();
}
