package com.codebutler.farebot.card.desfire;

import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;

@Root(name="settings")
public class StandardDesfireFileSettings extends DesfireFileSettings {
    @Element(name="filesize") private int mFileSize;

    private StandardDesfireFileSettings() { /* For XML Serializer */ }

    StandardDesfireFileSettings(ByteArrayInputStream stream) {
        super(stream);
        byte[] buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mFileSize = Utils.byteArrayToInt(buf);
    }

    public StandardDesfireFileSettings(byte fileType, byte commSetting, byte[] accessRights, int fileSize) {
        super(fileType, commSetting, accessRights);
        this.mFileSize = fileSize;
    }

    public int getFileSize() {
        return mFileSize;
    }
}
