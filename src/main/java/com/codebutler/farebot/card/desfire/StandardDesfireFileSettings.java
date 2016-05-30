/*
 * StandardDesfireFileSettings.java
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
