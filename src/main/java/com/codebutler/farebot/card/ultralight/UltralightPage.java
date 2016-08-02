package com.codebutler.farebot.card.ultralight;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.google.auto.value.AutoValue;

/**
 * Represents a page of data on a Mifare Ultralight (4 bytes)
 */
@AutoValue
public abstract class UltralightPage implements Parcelable {

    @NonNull
    public static UltralightPage create(int index, @NonNull byte[] data) {
        return new AutoValue_UltralightPage(index, ByteArray.create(data));
    }

    public abstract int getIndex();

    @NonNull
    public abstract ByteArray getData();
}
