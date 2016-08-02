package com.codebutler.farebot.card.classic;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class DataClassicSector extends ClassicSector {

    @NonNull
    public static ClassicSector create(int sectorIndex, List<ClassicBlock> classicBlocks) {
        return new AutoValue_DataClassicSector(sectorIndex, classicBlocks);
    }
}
