/*
 * DesfireFileSettings.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.codebutler.farebot.core.ByteArray;

public abstract class DesfireFileSettings implements Parcelable {

    /* DesfireFile Types */
    public static final byte STANDARD_DATA_FILE = (byte) 0x00;
    public static final byte BACKUP_DATA_FILE = (byte) 0x01;
    public static final byte VALUE_FILE = (byte) 0x02;
    public static final byte LINEAR_RECORD_FILE = (byte) 0x03;
    public static final byte CYCLIC_RECORD_FILE = (byte) 0x04;

    public abstract byte getFileType();

    abstract byte getCommSetting();

    @NonNull
    abstract ByteArray getAccessRights();

    @NonNull
    // FIXME: Localize
    public String getFileTypeName() {
        switch (getFileType()) {
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
