package com.codebutler.farebot.card.desfire;

import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;

@Root(name="settings")
public class RecordDesfireFileSettings extends DesfireFileSettings {
    @Element(name="recordsize") private int mRecordSize;
    @Element(name="maxrecords") private int mMaxRecords;
    @Element(name="currecords") private int mCurRecords;

    private RecordDesfireFileSettings() { /* For XML Serializer */ }

    public RecordDesfireFileSettings(byte fileType, byte commSetting, byte[] accessRights, int recordSize, int maxRecords, int curRecords) {
        super(fileType, commSetting, accessRights);
        this.mRecordSize = recordSize;
        this.mMaxRecords = maxRecords;
        this.mCurRecords = curRecords;
    }

    public RecordDesfireFileSettings(ByteArrayInputStream stream) {
        super(stream);

        byte[] buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mRecordSize = Utils.byteArrayToInt(buf);

        buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mMaxRecords = Utils.byteArrayToInt(buf);

        buf = new byte[3];
        stream.read(buf, 0, buf.length);
        ArrayUtils.reverse(buf);
        mCurRecords = Utils.byteArrayToInt(buf);
    }

    public int getRecordSize() {
        return mRecordSize;
    }

    public int getMaxRecords() {
        return mMaxRecords;
    }

    public int getCurRecords() {
        return mCurRecords;
    }
}
