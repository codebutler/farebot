package com.codebutler.farebot.card.classic.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class RawClassicBlock {

    @NonNull
    public static RawClassicBlock create(int blockIndex, byte[] data) {
        return new AutoValue_RawClassicBlock(blockIndex, ByteArray.create(data));
    }

    @NonNull
    public static TypeAdapter<RawClassicBlock> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawClassicBlock.GsonTypeAdapter(gson);
    }

    @NonNull
    public ClassicBlock parse() {
        return ClassicBlock.create(type(), index(), data());
    }

    public abstract int index();

    @NonNull
    public abstract ByteArray data();

    public String type() {
        // FIXME: Support other types
        return ClassicBlock.TYPE_DATA;
    }
}
