/*
 * CEPASProtocol.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
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

package com.codebutler.farebot.card.cepas;

import android.nfc.tech.IsoDep;
import android.support.annotation.NonNull;
import android.util.Log;

import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory;
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class CEPASProtocol {

    private static final String TAG = "CEPASProtocol";

    private static final byte[] CEPAS_SELECT_FILE_COMMAND = new byte[]{
            (byte) 0x00, (byte) 0xA4, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x40, (byte) 0x00};

    /* Status codes */
    private static final byte OPERATION_OK = (byte) 0x00;
    private static final byte PERMISSION_DENIED = (byte) 0x9D;

    @NonNull private final IsoDep mTagTech;

    CEPASProtocol(@NonNull IsoDep tagTech) {
        mTagTech = tagTech;
    }

    RawCEPASPurse getPurse(int purseId) throws IOException {
        try {
            sendSelectFile();
            byte[] purseBuff = sendRequest((byte) 0x32, (byte) (purseId), (byte) 0, (byte) 0, new byte[]{(byte) 0});
            if (purseBuff != null) {
                return RawCEPASPurse.create(purseId, purseBuff);
            } else {
                return RawCEPASPurse.create(purseId, "No purse found");
            }
        } catch (CEPASException ex) {
            Log.w(TAG, "Error reading purse " + purseId, ex);
            return RawCEPASPurse.create(purseId, ex.getMessage());
        }
    }

    @NonNull
    RawCEPASHistory getHistory(int purseId, int recordCount) throws IOException {
        try {
            byte[] fullHistoryBuff = null;
            byte[] historyBuff = sendRequest((byte) 0x32, (byte) (purseId), (byte) 0, (byte) 1,
                    new byte[]{(byte) 0, (byte) (recordCount <= 15 ? recordCount * 16 : 15 * 16)});

            if (historyBuff != null) {
                if (recordCount > 15) {
                    byte[] historyBuff2 = null;
                    try {
                        historyBuff2 = sendRequest((byte) 0x32, (byte) (purseId), (byte) 0, (byte) 1,
                                new byte[]{(byte) 0x0F, (byte) ((recordCount - 15) * 16)});
                    } catch (CEPASException ex) {
                        Log.w(TAG, "Error reading 2nd purse history " + purseId, ex);
                    }
                    fullHistoryBuff = new byte[historyBuff.length + (historyBuff2 != null ? historyBuff2.length : 0)];

                    System.arraycopy(historyBuff, 0, fullHistoryBuff, 0, historyBuff.length);
                    if (historyBuff2 != null) {
                        System.arraycopy(historyBuff2, 0, fullHistoryBuff, historyBuff.length, historyBuff2.length);
                    }
                } else {
                    fullHistoryBuff = historyBuff;
                }
            }

            if (fullHistoryBuff != null) {
                return RawCEPASHistory.create(purseId, fullHistoryBuff);
            } else {
                return RawCEPASHistory.create(purseId, "No history found");
            }
        } catch (CEPASException ex) {
            Log.w(TAG, "Error reading purse history " + purseId, ex);
            return RawCEPASHistory.create(purseId, ex.getMessage());
        }
    }

    private byte[] sendSelectFile() throws IOException {
        return mTagTech.transceive(CEPAS_SELECT_FILE_COMMAND);
    }

    private byte[] sendRequest(byte command, byte p1, byte p2, byte lc, byte[] parameters) throws CEPASException,
            IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, p1, p2, lc, parameters));

        if (recvBuffer[recvBuffer.length - 2] != (byte) 0x90) {
            if (recvBuffer[recvBuffer.length - 2] == 0x6b) {
                throw new CEPASException("File " + p1 + " was an invalid file.");

            } else if (recvBuffer[recvBuffer.length - 2] == 0x67) {
                throw new CEPASException("Got invalid file size response.");
            }

            throw new CEPASException("Got generic invalid response: "
                    + Integer.toHexString(((int) recvBuffer[recvBuffer.length - 2]) & 0xff));
        }

        output.write(recvBuffer, 0, recvBuffer.length - 2);

        byte status = recvBuffer[recvBuffer.length - 1];
        if (status == OPERATION_OK) {
            return output.toByteArray();
        } else if (status == PERMISSION_DENIED) {
            throw new CEPASException("Permission denied");
        } else {
            throw new CEPASException("Unknown status code: " + Integer.toHexString(status & 0xFF));
        }
    }

    private byte[] wrapMessage(byte command, byte p1, byte p2, byte lc, byte[] parameters) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write((byte) 0x90); // CLA
        stream.write(command);     // INS
        stream.write(p1);          // P1
        stream.write(p2);          // P2
        stream.write(lc);          // Lc

        // Write Lc and data fields
        if (parameters != null) {
            stream.write(parameters); // Data field
        }

        return stream.toByteArray();
    }
}
