package com.codebutler.farebot.card.ultralight;

import com.codebutler.farebot.xml.Base64String;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Represents a page of data on a Mifare Ultralight (4 bytes)
 */
@Root(name="page")
public class UltralightPage {
    @Attribute(name = "index")
    private int mIndex;
    @Element(name = "data")
    private Base64String mData;

    public static UltralightPage create(int index, byte[] data) {
        return new UltralightPage(index, data);
    }

    public UltralightPage() {
    }

    public UltralightPage(int index, byte[] data) {
        mIndex = index;
        mData = new Base64String(data);
    }

    public int getIndex() {
        return mIndex;
    }

    public byte[] getData() {
        return mData.getData();
    }


}
