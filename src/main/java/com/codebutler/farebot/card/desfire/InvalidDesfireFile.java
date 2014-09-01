package com.codebutler.farebot.card.desfire;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="file")
public class InvalidDesfireFile extends DesfireFile {
    @Element(name="error") private String mErrorMessage;

    private InvalidDesfireFile() { /* For XML Serializer */ }

    public InvalidDesfireFile(int fileId, String errorMessage) {
        super(fileId, null, new byte[0]);
        mErrorMessage = errorMessage;
    }

    public String getErrorMessage () {
        return mErrorMessage;
    }

    @Override public byte[] getData() {
        throw new IllegalStateException(String.format("Invalid file: %s", mErrorMessage));
    }
}
