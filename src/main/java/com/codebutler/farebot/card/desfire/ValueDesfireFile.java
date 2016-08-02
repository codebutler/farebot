package com.codebutler.farebot.card.desfire;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.util.Utils;
import com.google.auto.value.AutoValue;

import org.apache.commons.lang3.ArrayUtils;


/**
 * Represents a value file in Desfire
 */
@AutoValue
public abstract class ValueDesfireFile implements DesfireFile {

    @NonNull
    public static ValueDesfireFile create(
            int fileId,
            @NonNull DesfireFileSettings fileSettings,
            @NonNull byte[] fileData) {
        byte[] myData = ArrayUtils.clone(fileData);
        ArrayUtils.reverse(myData);
        int value = Utils.byteArrayToInt(myData);
        return new AutoValue_ValueDesfireFile(fileId, fileSettings, ByteArray.create(fileData), value);
    }

    public abstract int getValue();
}

