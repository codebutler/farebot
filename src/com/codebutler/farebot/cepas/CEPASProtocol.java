/*
 * DesfireProtocol.java
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

package com.codebutler.farebot.cepas;

import com.codebutler.farebot.cepas.CEPASPurse.InvalidCEPASPurse;
import com.codebutler.farebot.cepas.CEPASHistory.InvalidCEPASHistory;


import android.nfc.tech.IsoDep;
import android.util.Log;

import com.codebutler.farebot.Utils;

import java.io.ByteArrayOutputStream;

public class CEPASProtocol
{
    /* Commands */
    static final byte GET_MANUFACTURING_DATA    = (byte) 0x60;
    static final byte GET_APPLICATION_DIRECTORY = (byte) 0x6A;
    static final byte GET_ADDITIONAL_FRAME      = (byte) 0xAF;
    static final byte SELECT_APPLICATION        = (byte) 0x5A;
    static final byte READ_DATA                 = (byte) 0xBD;
    static final byte READ_RECORD               = (byte) 0xBB;
    static final byte GET_FILES                 = (byte) 0x6F;
    static final byte GET_FILE_SETTINGS         = (byte) 0xF5;

    /* Status codes */
    static final byte OPERATION_OK      = (byte) 0x00;
    static final byte PERMISSION_DENIED = (byte) 0x9D;
    static final byte ADDITIONAL_FRAME  = (byte) 0xAF;

    private IsoDep mTagTech;

    public CEPASProtocol(IsoDep tagTech)
    {
        mTagTech = tagTech;
    }

    public CEPASPurse getPurse(int purseId) throws Exception
    {
    	byte[] purseBuff;
    	purseBuff = sendRequest((byte)0x32, (byte) (purseId), (byte)0, (byte)0, null);
    	if(purseBuff != null)
    		return new CEPASPurse(purseId, purseBuff);
    	else
    		return new InvalidCEPASPurse(purseId, "No purse found");
    }
    
    public CEPASHistory getHistory(int purseId) throws Exception
    {
    	byte[] historyBuff;
    	historyBuff = sendRequest((byte)0x32, (byte) (purseId), (byte)0, (byte)1, null);
    	if(historyBuff != null)
    		return new CEPASHistory(purseId, historyBuff);
    	else
    		return new InvalidCEPASHistory(purseId, "No history found");
    }
    
    private byte[] sendRequest (byte command, byte p1, byte p2, byte Lc, byte[] parameters) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        byte[] recvBuffer = mTagTech.transceive(wrapMessage(command, p1, p2, Lc, parameters));

        while (true) {
            if (recvBuffer[recvBuffer.length - 2] != (byte) 0x90) {
            	if(recvBuffer[recvBuffer.length-2] == 0x6b) {
            		Log.d("CEPASProtocol", "File " + p1 + " was an invalid file.");
            		p1++;
            		return null;
            	}
            	else if(recvBuffer[recvBuffer.length-2] == 0x67) {
            		recvBuffer = mTagTech.transceive(wrapMessage(command, p1, p2, Lc, parameters));
            		Log.d("CEPASProtocol", "Got invalid file size response.");
            		return null;
            	}
            	
            	Log.d("CEPASProtocol", "Got generic invalid response: " + Integer.toHexString( ((int)recvBuffer[recvBuffer.length-2]) & 0xff));
                throw new Exception("Invalid response");
            }

            output.write(recvBuffer, 0, recvBuffer.length - 2);

            byte status = recvBuffer[recvBuffer.length - 1];
            if (status == OPERATION_OK) {
                break;
//            } else if (status == ADDITIONAL_FRAME) {
 //               recvBuffer = mTagTech.transceive(wrapMessage(GET_ADDITIONAL_FRAME, null));
            } else if (status == PERMISSION_DENIED) {
                throw new Exception("Permission denied");
            } else {
                throw new Exception("Unknown status code: " + Integer.toHexString(status & 0xFF));
            }
        }
        
        return output.toByteArray();
    }
    
    private byte[] wrapMessage (byte command, byte p1, byte p2, byte Lc, byte[] parameters) throws Exception
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write((byte) 0x90); // CLA
        stream.write(command); // INS
        stream.write((byte) p1); // P1
        stream.write((byte) p2); // P2
        stream.write((byte) Lc); // Lc
        
        // Write Lc and data fields
        if (parameters != null) {
            stream.write((byte) parameters.length); // Lc = length
            stream.write(parameters); // Data field
        }
        else
        	stream.write((byte) 0x00); // Lc = 0

        return stream.toByteArray();
    }
}
