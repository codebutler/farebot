/*
 * RecordDesfireFileSettings.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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
