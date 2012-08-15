/*
 * ClassicProtocol.java
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

package com.codebutler.farebot.mifareclassic;

import java.io.IOException;

import android.nfc.tech.MifareClassic;
import android.util.Log;

import com.codebutler.farebot.keys.Keys;

public class ClassicProtocol {
	private static final byte[] KEY_FIRST_SECTOR = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
	protected static MifareClassic mTech;

	public ClassicProtocol (MifareClassic tech) {
		mTech = tech;
	}

	public boolean verifyKeys(byte[][] keys) throws IOException {
		for(int sector = 0; sector < mTech.getSectorCount(); sector++){
	         if(!mTech.authenticateSectorWithKeyA(sector, keys[sector])) {
	        	 return false;
	         }
		}

		return true;
	}

	public byte[] getFirstSector () throws Exception {
		return readBlocks(0, 0, 3, null);
	}

	protected byte[] readBlocks(int sector, int blockIndex, int blockCount, Keys keys) throws Exception {
		byte[] data = new byte[blockCount * 16];

		int readBlocks = 0;
   	 	for (int index = blockIndex; index < (blockIndex + blockCount); index++) {	
	         byte[] key;

	         if (keys == null && sector == 0)
	        	 key = KEY_FIRST_SECTOR;
	         else
	        	 key = keys.getKeyByteArray(sector);

	         mTech.authenticateSectorWithKeyA(sector, key);

	         byte[] readBlock = mTech.readBlock(index);

	         System.arraycopy(readBlock, 0, data, readBlocks * 16, readBlock.length);
	         readBlocks++;
        }

   	 	return data;
	}

	public static byte[] readSector(byte[] key, int sector) {
		int blockCount = mTech.getBlockCountInSector(sector) - 1; // Skipping the last block, seeing as it only contains the keys and access bits
		byte[] data = new byte[(blockCount) * 16];

		try {
			if (mTech.authenticateSectorWithKeyA(sector, key)){
	        	 int readBlocks = 0;
	        	 for (int blockIndex = mTech.sectorToBlock(sector); blockIndex < (mTech.sectorToBlock(sector) + blockCount); blockIndex++) {	
			         byte[] readBlock = mTech.readBlock(blockIndex);

			         System.arraycopy(readBlock, 0, data, readBlocks * 16, readBlock.length);
			         readBlocks++;
		         } 
			}
		} catch (IOException e) {
			// TODO: error handling?
			e.printStackTrace();
		}

		return data;
	}

	public static byte[] readBlocksForSector(int sector, int startBlock, int endBlock, byte[] key_a) {    
		byte[] ans = new byte[((endBlock - startBlock) + 1) * 16];

		if(readBlocksForSector(sector, startBlock, endBlock)) {
			return ans;
		}

		try {
			if (!mTech.authenticateSectorWithKeyA(sector, key_a)) {
				Log.e("ClassicProtocol", "Incorrect key at sector: " + sector);
				return ans;
			}

			for (int block = mTech.sectorToBlock(sector) + startBlock; block <= mTech.sectorToBlock(sector) + endBlock; block++) {
				byte[] readBlock = mTech.readBlock(block);

				System.arraycopy(readBlock, 0, ans, (block - (mTech.sectorToBlock(sector) + startBlock)) * 16, readBlock.length);
			}
		} catch (IOException e) {
			Log.e("ClassicProtocol", "Error reading sector: " + sector);
			return ans;
		}

		return ans;
	}

	private static boolean readBlocksForSector(int sector, int startBlock, int endBlock) { 
		if (sector <= 31)
			if (startBlock < 0 || endBlock > 3 || startBlock > endBlock)
				return true;

		if (sector > 31)
			if (startBlock < 0 || endBlock > 15 || startBlock > endBlock)
				return true;

		return false;
	}
}