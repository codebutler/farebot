package com.codebutler.farebot.card.desfire;

import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name="file")
public class RecordDesfireFile extends DesfireFile {
    private transient List<DesfireRecord> mRecords;

    private RecordDesfireFile() { /* For XML Serializer */ }

    RecordDesfireFile(int fileId, DesfireFileSettings fileSettings, byte[] fileData) {
        super(fileId, fileSettings, fileData);

        RecordDesfireFileSettings settings = (RecordDesfireFileSettings) fileSettings;

        DesfireRecord[] records = new DesfireRecord[settings.getCurRecords()];
        for (int i = 0; i < settings.getCurRecords(); i++) {
            int offset = settings.getRecordSize() * i;
            records[i] = new DesfireRecord(ArrayUtils.subarray(getData(), offset, offset + settings.getRecordSize()));
        }
        mRecords = Utils.arrayAsList(records);
    }

    public List<DesfireRecord> getRecords() {
        return mRecords;
    }
}
