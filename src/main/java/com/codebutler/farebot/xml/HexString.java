package com.codebutler.farebot.xml;

import com.codebutler.farebot.util.Utils;

public class HexString {
    private final byte[] mData;

    public HexString(byte[] data) {
        mData = data;
    }

    public HexString(String hex) {
        mData = Utils.hexStringToByteArray(hex);
    }

    public byte[] getData() {
        return mData;
    }

    public String toHexString() {
        return Utils.getHexString(mData);
    }

    public static final class Transform implements org.simpleframework.xml.transform.Transform<HexString> {
        @Override public HexString read(String value) throws Exception {
            return new HexString(value);
        }
        @Override public String write(HexString value) throws Exception {
            return value.toHexString();
        }
    }
}
