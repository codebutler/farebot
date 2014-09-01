package com.codebutler.farebot.xml;

import com.codebutler.farebot.card.desfire.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.RecordDesfireFileSettings;
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings;
import com.codebutler.farebot.card.desfire.UnsupportedDesfireFileSettings;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class DesfireFileSettingsConverter implements Converter<DesfireFileSettings> {
    @Override public DesfireFileSettings read(InputNode source) throws Exception {
        byte fileType = -1;
        int fileSize = -1;
        byte commSetting = -1;
        byte[] accessRights = new byte[0];
        int recordSize = -1;
        int maxRecords = -1;
        int curRecords = -1;

        while (true) {
            InputNode node = source.getNext();
            if (node == null) {
                break;
            }
            switch (node.getName()) {
                case "filetype":
                    fileType = Byte.parseByte(node.getValue());
                    break;
                case "filesize":
                    fileSize = Integer.parseInt(node.getValue());
                    break;
                case "commsetting":
                    commSetting = Byte.parseByte(node.getValue());
                    break;
                case "accessrights":
                    accessRights = Utils.hexStringToByteArray(node.getValue());
                    break;
                case "recordsize":
                    recordSize = Integer.parseInt(node.getValue());
                    break;
                case "maxrecords":
                    maxRecords = Integer.parseInt(node.getValue());
                    break;
                case "currecords":
                    curRecords = Integer.parseInt(node.getValue());
                    break;
            }
        }

        switch (fileType) {
            case DesfireFileSettings.STANDARD_DATA_FILE:
            case DesfireFileSettings.BACKUP_DATA_FILE:
                return new StandardDesfireFileSettings(fileType, commSetting, accessRights, fileSize);
            case DesfireFileSettings.LINEAR_RECORD_FILE:
            case DesfireFileSettings.CYCLIC_RECORD_FILE:
                return new RecordDesfireFileSettings(fileType, commSetting, accessRights, recordSize, maxRecords,
                        curRecords);
            default:
                return new UnsupportedDesfireFileSettings(fileType);
        }
    }

    @Override public void write(OutputNode node, DesfireFileSettings value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
