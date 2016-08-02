package com.codebutler.farebot;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.codebutler.farebot.util.Utils;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Wrapper around byte[] that always provides a copy, ensuring immutability.
 */
public class ByteArray implements Parcelable {

    @NonNull
    public static final Creator<ByteArray> CREATOR = new Creator<ByteArray>() {
        @Override
        public ByteArray createFromParcel(Parcel in) {
            return new ByteArray(in);
        }

        @Override
        public ByteArray[] newArray(int size) {
            return new ByteArray[size];
        }
    };

    @NonNull private final byte[] mData;

    public ByteArray(@NonNull byte[] data) {
        mData = data;
    }

    private ByteArray(@NonNull Parcel in) {
        mData = in.createByteArray();
    }

    @NonNull
    public static ByteArray create(@NonNull byte[] data) {
        return new ByteArray(data);
    }

    @NonNull
    private static ByteArray createFromBase64(@NonNull String base64String) {
        return new ByteArray(Base64.decode(base64String, Base64.DEFAULT));
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(mData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public byte[] bytes() {
        return Arrays.copyOf(mData, mData.length);
    }

    @Nullable
    public String hex() {
        return Utils.getHexString(mData, null);
    }

    @NonNull
    public String base64() {
        return Base64.encodeToString(mData, Base64.NO_WRAP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ByteArray byteArray = (ByteArray) o;

        return Arrays.equals(mData, byteArray.mData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mData);
    }

    public static class GsonTypeAdapter extends TypeAdapter<ByteArray> {

        @Override
        public void write(@NonNull JsonWriter out, @NonNull ByteArray value) throws IOException {
            out.value(value.base64());
        }

        @Override
        public ByteArray read(@NonNull JsonReader in) throws IOException {
            return ByteArray.createFromBase64(in.nextString());
        }
    }
}
