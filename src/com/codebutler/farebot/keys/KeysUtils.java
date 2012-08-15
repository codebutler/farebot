/*
 * KeysUtils.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.keys;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifareclassic.ClassicProtocol;

public class KeysUtils {
	private static String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	private static String KEYS_DIR = "FareBot/Keys";	// FIXME: Choose where to store the keys (dump files)

	private static Context mContext;

    public KeysUtils(Context context) {
        this.mContext = context;
    }

    public String[] getKeysForCard(ClassicProtocol protocol, String id, String type) {
    	String[] sKeys = null;
    	byte[][] bKeys = null;
    	boolean inDatabase = true;

    	try {
    		sKeys = getKeysFromDatabase(id, type);

	    	if (sKeys == null) {
	    		inDatabase = false;
	    		bKeys = getKeysFromDump(id, type);
	    	}

	    	if (!inDatabase && bKeys != null) {
	    		if (protocol.verifyKeys(bKeys)) {
	    			sKeys = new String[bKeys.length];
	    			for (int sector = 0; sector < bKeys.length; sector++) {
	    				sKeys[sector] = Utils.getHexString(bKeys[sector]);
	    			}

	    			insertKeysIntoDatabase(id, type, sKeys);
	    		} else {
	    			return null;
	    		}
	    	}
    	} catch (Exception e) {
            Log.e("KeysUtils", "Error in getKeysForCard", e);
            return null;
        }

    	return sKeys;
    }

    private static void insertKeysIntoDatabase(String id, String type, String[] keys) throws Exception {
    	KeysDBHelper keysDBHelper = new KeysDBHelper(mContext);
    	keysDBHelper.open();
    	keysDBHelper.openDatabase();

    	for (int sector = 0; sector < keys.length; sector++) {
    		keysDBHelper.insertKey(id, sector, type, keys[sector]);
    	}

    	keysDBHelper.close();
    }

    private static String[] getKeysFromDatabase(String id, String type) throws Exception {
    	String[] keys = new String[40];

    	KeysDBHelper keysDBHelper = new KeysDBHelper(mContext);
    	keysDBHelper.open();
    	keysDBHelper.openDatabase();

    	for (int sector = 0; sector < 40; sector++) {
    		String key = keysDBHelper.getKey(id, sector, type);

    		if (key != null) {
    			keys[sector] = key;
    		} else {
    			keysDBHelper.close();
    			return null;
    		}
    	}

    	keysDBHelper.close();

    	return keys;
    }

    private static byte[][] getKeysFromDump(String id, String type) throws Exception {
		byte[][] allkeys = new byte[40][6];		
		byte[] keys = readKeyFile(id, type);

		if (keys != null) {
			int offset = 0;
			for (int i = 0; i < 40; i++) {
				System.arraycopy(keys, offset, allkeys[i], 0, 6);
				offset += 6;
			}

			return allkeys;
		}

		return null;
	}

    private static byte[] readKeyFile(String cardnumber, String keytype) {
		byte[] keys = null;
		File file = new File (SDCARD_PATH + "/" + KEYS_DIR + "/" + keytype + cardnumber + ".dump");

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			if (file.exists()) {
				if (file.length() == 240) { /* 40 keys, 6 bytes each */
					try {
						keys = readBinFileIntoByteArray(file);
					} catch (IOException e) {
						e.printStackTrace();
					}

					return keys;
				}
			}
		}

		return null;
	}

	private static byte[] readBinFileIntoByteArray(File file) throws IOException {
		FileInputStream is = new FileInputStream(file);
		long length = file.length();

		if (length > Integer.MAX_VALUE) {
			throw new IOException("The file is too big");
		}

		byte[] bytes = new byte[(int) length];

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		if (offset < bytes.length) {
			throw new IOException("The file was not completely read: " + file.getName());
		}

		is.close();
		return bytes;
	}
}