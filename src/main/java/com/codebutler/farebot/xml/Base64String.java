package com.codebutler.farebot.xml;

import android.util.Base64;

public class Base64String {
    private final byte[] mData;

    public Base64String(byte[] data) {
        mData = data;
    }

    public Base64String(String data) {
        mData = Base64.decode(data, Base64.DEFAULT);
    }

    public byte[] getData() {
        return mData;
    }

    public String toBase64() {
        return Base64.encodeToString(mData, Base64.NO_WRAP);
    }

    public static final class Transform implements org.simpleframework.xml.transform.Transform<Base64String> {
        @Override public Base64String read(String value) throws Exception {
            return new Base64String(value);
        }
        @Override public String write(Base64String value) throws Exception {
            return value.toBase64();
        }
    }
}
