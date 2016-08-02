package com.codebutler.farebot.card.desfire.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.desfire.DesfireManufacturingData;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RawDesfireManufacturingData {

    @NonNull
    public static RawDesfireManufacturingData create(@NonNull byte[] data) {
        return new AutoValue_RawDesfireManufacturingData(ByteArray.create(data));
    }

    @NonNull
    public static TypeAdapter<RawDesfireManufacturingData> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireManufacturingData.GsonTypeAdapter(gson);
    }

    @NonNull
    public abstract ByteArray getData();

    @NonNull
    public final DesfireManufacturingData parse() {
        return DesfireManufacturingData.create(getData().bytes());
    }
}
