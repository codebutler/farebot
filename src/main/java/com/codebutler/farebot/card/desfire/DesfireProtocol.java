/*
 * DesfireProtocol.java
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

import android.nfc.tech.IsoDep;

import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings;
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData;
import com.codebutler.farebot.util.Utils;

import java.io.ByteArrayOutputStream;
import java.security.AccessControlException;

class DesfireProtocol {
    // Reference: http://neteril.org/files/M075031_desfire.pdf
    // Commands
    private static final byte GET_MANUFACTURING_DATA = (byte) 0x60;
    private static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    private static final byte GET_ADDITIONAL_FRAME = (byte) 0xAF;
    private static final byte SELECT_APPLICATION = (byte) 0x5A;
    private static final byte READ_DATA = (byte) 0xBD;
    private static final byte READ_RECORD = (byte) 0xBB;
    private static final byte GET_VALUE = (byte) 0x6C;
    private static final byte GET_FILES = (byte) 0x6F;
    private static final byte GET_FILE_SETTINGS = (byte) 0xF5;

    // Status codes (Section 3.4)
    private static final byte OPERATION_OK = (byte) 0x00;
    private static final byte PERMISSION_DENIED = (byte) 0x9D;
    private static final byte AUTHENTICATION_ERROR = (byte) 0xAE;
    private static final byte ADDITIONAL_FRAME = (byte) 0xAF;

    private IsoDep mTagTech;

    DesfireProtocol(IsoDep tagTech) {
        mTagTech = tagTech;
    }

    RawDesfireManufacturingData getManufacturingData() throws Exception {
        byte[] respBuffer = sendRequest(GET_MANUFACTURING_DATA);

        if (respBuffer.length != 28) {
            throw new Exception("Invalid response");
        }

        return RawDesfireManufacturingData.create(respBuffer);
    }

    int[] getAppList() throws Exception {
        byte[] appDirBuf = sendRequest(GET_APPLICATION_DIRECTORY);

        int[] appIds = new int[appDirBuf.length / 3];

        for (int app = 0; app < appDirBuf.length; app += 3) {
            byte[] appId = new byte[3];
            System.arraycopy(appDirBuf, app, appId, 0, 3);

            appIds[app / 3] = Utils.byteArrayToInt(appId);
        }

        return appIds;
    }

    void selectApp(int appId) throws Exception {
        byte[] appIdBuff = new byte[3];
        appIdBuff[0] = (byte) ((appId & 0xFF0000) >> 16);
        appIdBuff[1] = (byte) ((appId & 0xFF00) >> 8);
        appIdBuff[2] = (byte) (appId & 0xFF);

        sendRequest(SELECT_APPLICATION, appIdBuff);
    }

    int[] getFileList() throws Exception {
        byte[] buf = sendRequest(GET_FILES);
        int[] fileIds = new int[buf.length];
        for (int x = 0; x < buf.length; x++) {
            fileIds[x] = (int) buf[x];
        }
        return fileIds;
    }

    RawDesfireFileSettings getFileSettings(int fileNo) throws Exception {
        byte[] data = sendRequest(GET_FILE_SETTINGS, new byte[]{(byte) fileNo});
        return RawDesfireFileSettings.create(data);
    }

    byte[] readFile(int fileNo) throws Exception {
        return sendRequest(READ_DATA, new byte[]{
                (byte) fileNo,
                (byte) 0x0, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0
        });
    }

    byte[] readRecord(int fileNum) throws Exception {
        return sendRequest(READ_RECORD, new byte[]{
                (byte) fileNum,
                (byte) 0x0, (byte) 0x0, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x0
        });
    }

    byte[] getValue(int fileNum) throws Exception {
        return sendRequest(GET_VALUE, new byte[]{
                (byte) fileNum
        });
    }

    private byte[] sendRequest(byte command) throws Exception {
        return sendRequest(command, null);
    }

    private byte[] sendRequest(byte command, byte[] parameters) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, parameters));

        while (true) {
            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x91) {
                throw new Exception("Invalid response");
            }

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == OPERATION_OK) {
                break;
            } else if (status == ADDITIONAL_FRAME) {
                recvBuffer = mTagTech.transceive(wrapMessage(GET_ADDITIONAL_FRAME, null));
            } else if (status == PERMISSION_DENIED) {
                throw new AccessControlException("Permission denied");
            } else if (status == AUTHENTICATION_ERROR) {
                throw new AccessControlException("Authentication error");
            } else {
                throw new Exception("Unknown status code: " + Integer.toHexString(status & 0xFF));
            }
        }

        return output.toByteArray();
    }

    private byte[] wrapMessage(byte command, byte[] parameters) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write((byte) 0x90);
        stream.write(command);
        stream.write((byte) 0x00);
        stream.write((byte) 0x00);
        if (parameters != null) {
            stream.write((byte) parameters.length);
            stream.write(parameters);
        }
        stream.write((byte) 0x00);

        return stream.toByteArray();
    }
}
