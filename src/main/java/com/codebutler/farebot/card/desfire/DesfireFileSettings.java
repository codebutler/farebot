/*
 * DesfireFileSettings.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import android.os.Parcel;
import android.os.Parcelable;
import com.codebutler.farebot.Utils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayInputStream;

public abstract class DesfireFileSettings implements Parcelable {
    public final byte   fileType;
    public final byte   commSetting;
    public final byte[] accessRights;

    /* DesfireFile Types */
    static final byte STANDARD_DATA_FILE = (byte) 0x00;
    static final byte BACKUP_DATA_FILE   = (byte) 0x01;
    static final byte VALUE_FILE         = (byte) 0x02;
    static final byte LINEAR_RECORD_FILE = (byte) 0x03;
    static final byte CYCLIC_RECORD_FILE = (byte) 0x04;
    
    public static DesfireFileSettings Create (byte[] data) throws Exception {
        byte fileType = (byte) data[0];

        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        if (fileType == STANDARD_DATA_FILE || fileType == BACKUP_DATA_FILE)
            return new StandardDesfireFileSettings(stream);
        else if (fileType == LINEAR_RECORD_FILE || fileType == CYCLIC_RECORD_FILE)
            return new RecordDesfireFileSettings(stream);
        else if (fileType == VALUE_FILE)
            throw new UnsupportedOperationException("Value files not yet supported");
        else
            throw new Exception("Unknown file type: " + Integer.toHexString(fileType));
    }

    private DesfireFileSettings (ByteArrayInputStream stream) {
        fileType    = (byte) stream.read();
        commSetting = (byte) stream.read();

        accessRights = new byte[2];
        stream.read(accessRights, 0, accessRights.length);
    }

    private DesfireFileSettings (byte fileType, byte commSetting, byte[] accessRights) {
        this.fileType     = fileType;
        this.commSetting  = commSetting;
        this.accessRights = accessRights;
    }

    public String getFileTypeName () {
        switch (fileType) {
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

    public static final Parcelable.Creator<DesfireFileSettings> CREATOR = new Parcelable.Creator<DesfireFileSettings>() {
        public DesfireFileSettings createFromParcel(Parcel source) {
            byte fileType       = source.readByte();
            byte commSetting    = source.readByte();
            byte[] accessRights = new byte[source.readInt()];
            source.readByteArray(accessRights);

            if (fileType == STANDARD_DATA_FILE || fileType == BACKUP_DATA_FILE) {
                int fileSize = source.readInt();
                return new StandardDesfireFileSettings(fileType, commSetting, accessRights, fileSize);
            } else if (fileType == LINEAR_RECORD_FILE || fileType == CYCLIC_RECORD_FILE) {
                int recordSize = source.readInt();
                int maxRecords = source.readInt();
                int curRecords = source.readInt();
                return new RecordDesfireFileSettings(fileType, commSetting, accessRights, recordSize, maxRecords, curRecords);
            } else {
                return new UnsupportedDesfireFileSettings(fileType);
            }
        }

        public DesfireFileSettings[] newArray(int size) {
            return new DesfireFileSettings[size];
        }
    };

    public void writeToParcel (Parcel parcel, int flags) {
        parcel.writeByte(fileType);
        parcel.writeByte(commSetting);
        parcel.writeInt(accessRights.length);
        parcel.writeByteArray(accessRights);
    }

    public int describeContents () {
        return 0;
    }

    public static class StandardDesfireFileSettings extends DesfireFileSettings {
        public final int fileSize;

        private StandardDesfireFileSettings (ByteArrayInputStream stream) {
            super(stream);
            byte[] buf = new byte[3];
            stream.read(buf, 0, buf.length);
            ArrayUtils.reverse(buf);
            fileSize = Utils.byteArrayToInt(buf);
        }

        StandardDesfireFileSettings (byte fileType, byte commSetting, byte[] accessRights, int fileSize) {
            super(fileType, commSetting, accessRights);
            this.fileSize = fileSize;
        }

        @Override
        public void writeToParcel (Parcel parcel, int flags) {
            super.writeToParcel(parcel, flags);
            parcel.writeInt(fileSize);
        }
    }

    public static class RecordDesfireFileSettings extends DesfireFileSettings {
        public final int recordSize;
        public final int maxRecords;
        public final int curRecords;

        public RecordDesfireFileSettings(ByteArrayInputStream stream) {
            super(stream);

            byte[] buf = new byte[3];
            stream.read(buf, 0, buf.length);
            ArrayUtils.reverse(buf);
            recordSize = Utils.byteArrayToInt(buf);

            buf = new byte[3];
            stream.read(buf, 0, buf.length);
            ArrayUtils.reverse(buf);
            maxRecords = Utils.byteArrayToInt(buf);

            buf = new byte[3];
            stream.read(buf, 0, buf.length);
            ArrayUtils.reverse(buf);
            curRecords = Utils.byteArrayToInt(buf);
        }

        RecordDesfireFileSettings (byte fileType, byte commSetting, byte[] accessRights, int recordSize, int maxRecords, int curRecords) {
            super(fileType, commSetting, accessRights);
            this.recordSize = recordSize;
            this.maxRecords = maxRecords;
            this.curRecords = curRecords;
        }

        @Override
        public void writeToParcel (Parcel parcel, int flags) {
            super.writeToParcel(parcel, flags);
            parcel.writeInt(recordSize);
            parcel.writeInt(maxRecords);
            parcel.writeInt(curRecords);
        }
    }

    public static class UnsupportedDesfireFileSettings extends DesfireFileSettings {
        public UnsupportedDesfireFileSettings(byte fileType) {
            super(fileType, Byte.MIN_VALUE, new byte[0]);
        }
    }
}

