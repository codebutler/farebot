/*
 * DesfireFileSettings.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.card.desfire;

import com.codebutler.farebot.xml.HexString;

import org.simpleframework.xml.Element;

import java.io.ByteArrayInputStream;

public abstract class DesfireFileSettings {

    /* DesfireFile Types */
    public static final byte STANDARD_DATA_FILE = (byte) 0x00;
    public static final byte BACKUP_DATA_FILE = (byte) 0x01;
    public static final byte VALUE_FILE = (byte) 0x02;
    public static final byte LINEAR_RECORD_FILE = (byte) 0x03;
    public static final byte CYCLIC_RECORD_FILE = (byte) 0x04;

    @Element(name = "filetype") private byte mFileType;
    @Element(name = "commsettings") private byte mCommSetting;
    @Element(name = "accessrights") private HexString mAccessRights;

    DesfireFileSettings() { /* For XML Serializer */ }

    DesfireFileSettings(ByteArrayInputStream stream) {
        mFileType = (byte) stream.read();
        mCommSetting = (byte) stream.read();

        byte[] accessRights = new byte[2];
        stream.read(accessRights, 0, accessRights.length);
        this.mAccessRights = new HexString(accessRights);
    }

    DesfireFileSettings(byte fileType, byte commSetting, byte[] accessRights) {
        this.mFileType = fileType;
        this.mCommSetting = commSetting;
        this.mAccessRights = new HexString(accessRights);
    }

    public static DesfireFileSettings create(byte[] data) throws Exception {
        byte fileType = data[0];

        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        if (fileType == STANDARD_DATA_FILE || fileType == BACKUP_DATA_FILE) {
            return new StandardDesfireFileSettings(stream);
        } else if (fileType == LINEAR_RECORD_FILE || fileType == CYCLIC_RECORD_FILE) {
            return new RecordDesfireFileSettings(stream);
        } else if (fileType == VALUE_FILE) {
            return new ValueDesfireFileSettings(stream);
        } else {
            throw new Exception("Unknown file type: " + Integer.toHexString(fileType));
        }
    }

    public byte getFileType() {
        return mFileType;
    }

    public byte getCommSetting() {
        return mCommSetting;
    }

    public HexString getAccessRights() {
        return mAccessRights;
    }

    public String getFileTypeName() {
        switch (mFileType) {
            case STANDARD_DATA_FILE:
                return "Standard";
            case BACKUP_DATA_FILE:
                return "Backup";
            case VALUE_FILE:
                return "Value";
            case LINEAR_RECORD_FILE:
                return "Linear Record";
            case CYCLIC_RECORD_FILE:
                return "Cyclic Record";
            default:
                return "Unknown";
        }
    }
}
