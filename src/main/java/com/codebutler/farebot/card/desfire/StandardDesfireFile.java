package com.codebutler.farebot.card.desfire;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class StandardDesfireFile implements DesfireFile {

    @NonNull
    public static DesfireFile create(int fileId, @NonNull DesfireFileSettings fileSettings, @NonNull byte[] fileData) {
        return new AutoValue_StandardDesfireFile(fileId, fileSettings, ByteArray.create(fileData));
    }
}
