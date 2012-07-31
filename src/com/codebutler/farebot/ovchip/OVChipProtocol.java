/*
 * OVChipProtocol.java
 *
 * Copyright (C) 2011 Eric Butler
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

package com.codebutler.farebot.ovchip;

import android.nfc.tech.MifareClassic;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.keys.Keys;
import com.codebutler.farebot.mifareclassic.ClassicProtocol;
import com.codebutler.farebot.mifareclassic.ClassicUtils;

public class OVChipProtocol extends ClassicProtocol {
	public OVChipProtocol (MifareClassic tech) {
		super(tech);
	}

	public OVChipPreamble getPreamble () throws Exception {
		byte[] data;

		data = readBlocks(0, 0, 3, null);

		if (data != null) {
			return new OVChipPreamble(data);
		} else
			return new OVChipPreamble(null);
	}

	public OVChipInfo getInfo (int blockIndex, Keys keys) throws Exception {
		byte[] data;
		int sector = (char)blockIndex == (char)0x580 ? 22 : 23;

		data = readBlocks(sector, ClassicUtils.convertBytePointerToBlock(blockIndex), 3, keys);

		if (data != null) {
			return new OVChipInfo(data);
		} else
			return new OVChipInfo(null);
	}

	public OVChipCredit getCredit (int blockIndex, Keys keys) throws Exception {
		byte[] data;

		data = readBlocks(39, ClassicUtils.convertBytePointerToBlock(blockIndex), 1, keys);

		if (data != null) {
			return new OVChipCredit(data);
		} else
			return new OVChipCredit(null);
	}

	public OVChipIndex getIndex (Keys keys) throws Exception {
		byte[] data;

		data = readIndex(keys);

		if (data != null) {
			return new OVChipIndex(data);
		} else
			return new OVChipIndex(null);
	}

	private byte[] readIndex (Keys keys) throws Exception {
		int blockCount = 4;
		byte[] buffer = new byte[blockCount * 16];
		int blockIndex = 0;
		int sector = 39;

		blockIndex = mTech.sectorToBlock(sector) + 11;
		buffer = readBlocks(sector, blockIndex, blockCount, keys);

		return buffer;
	}

	public OVChipTransaction getTransaction (int transactionId, Keys keys) throws Exception {
		byte[] data;

		data = readTransaction(transactionId, keys);

		if (data != null) {
			return new OVChipTransaction(transactionId, data);
		} else
			return new OVChipTransaction(transactionId, "No transaction");
	}

	private byte[] readTransaction (int transactionId, Keys keys) throws Exception {
		int blockCount = 2;
		byte[] buffer = new byte[blockCount * 16];
		int blockIndex = 0;
		int sector = 0;

		if (transactionId <= 6){
			sector = 35;
			blockIndex = mTech.sectorToBlock(sector) + (transactionId * 2);
		} else if (transactionId >= 7 && transactionId <= 13) {
			sector = 36;
			blockIndex = mTech.sectorToBlock(sector) + ((transactionId - 7)  * 2);
		} else if (transactionId >= 14 && transactionId <= 20) {
			sector = 37;
			blockIndex = mTech.sectorToBlock(sector) + ((transactionId - 14)  * 2);
		} else if (transactionId >= 21 && transactionId <= 27) {
			sector = 38;
			blockIndex = mTech.sectorToBlock(sector) + ((transactionId - 21)  * 2);
		} else {
			// TODO: error
		}

		buffer = readBlocks(sector, blockIndex, blockCount, keys);

		return buffer;
	}

	public OVChipSubscription[] getSubscriptions (int subscriptionIndexSlot, int[] subscriptionIndex, Keys keys) throws Exception {
		byte[] data;

		data = readSubscriptionIndexSlot(subscriptionIndexSlot, keys);

		/*
		 * TODO / FIXME
		 * The card can store 15 subscriptions and stores pointers to some extra information
		 * regarding these subscriptions. The problem is, it only stores 12 of these pointers.
		 * In the code used here we get the subscriptions according to these pointers,
		 * but this means that we could miss a few subscriptions. 
		 * 
		 * We could get the last few by looking at what has already been collected and get the
		 * rest ourself, but they will lack the extra information because it simply isn't
		 * there.
		 * 
		 * Or rewrite this and just get all the subscriptions and discard the ones that are
		 * invalid. Afterwards we can get the extra information if it's available.
		 * 
		 * For more info see:
		 * Dutch:   http://ov-chipkaart.pc-active.nl/Indexen
		 * English: http://ov-chipkaart.pc-active.nl/Indexes
		 */
		int count = Utils.getBitsFromBuffer(data, 0, 4);
		OVChipSubscription[] subscriptions = new OVChipSubscription[count];	// Might be *dangerous* to rely on this
		int offset = 4;

		for (int i = 0; i < count; i++) {
			int bits = Utils.getBitsFromBuffer(data, offset + (i * 21), 21);

			/* Based on info from ovc-tools by ocsr ( https://github.com/ocsrunl/ ) */
			int type1 = Utils.getBitsFromInteger(bits, 13, 8);
			int type2 = Utils.getBitsFromInteger(bits, 7, 6);
			int used = Utils.getBitsFromInteger(bits, 6, 1);
			int rest = Utils.getBitsFromInteger(bits, 4, 2);
			int subscriptionIndexId = Utils.getBitsFromInteger(bits, 0, 4);
			int subscriptionAddress = subscriptionIndex[(subscriptionIndexId - 1)];

			subscriptions[i] = getSubscription(subscriptionAddress, keys, type1, type2, used, rest);
		}

		return subscriptions;
	}

	private byte[] readSubscriptionIndexSlot(int subscriptionSlot, Keys keys) throws Exception {
		int blockCount = 2;
		byte[] buffer = new byte[blockCount * 16];
		int blockIndex = ClassicUtils.convertBytePointerToBlock(subscriptionSlot);
		int sector = 39;

		buffer = readBlocks(sector, blockIndex, blockCount, keys);

		return buffer;
	}

	private OVChipSubscription getSubscription (int subscriptionAddress, Keys keys, int type1, int type2, int used, int rest) throws Exception {
		byte[] data;

		data = readSubscription(subscriptionAddress, keys);

		if (data != null) {
			return new OVChipSubscription(subscriptionAddress, data, type1, type2, used, rest);
		} else
			return new OVChipSubscription(subscriptionAddress, "No subscription");
	}

	private byte[] readSubscription (int subscriptionAddress, Keys keys) throws Exception {
		int blockCount = 3;
		byte[] buffer = new byte[blockCount * 16];
		int blockIndex = ClassicUtils.convertBytePointerToBlock(subscriptionAddress);
		int sector = ClassicUtils.convertBytePointerToSector(subscriptionAddress);

		buffer = readBlocks(sector, blockIndex, blockCount, keys);

		return buffer;
	}

	public static byte[] readPreambleData(byte[][] keys) {
		return readSector(keys[0], 0);
	}

	public static byte[] readIndexData(byte[][] keys) {
		byte[] data = new byte[64];

		byte[] readBlock = readSector(keys[39], 39);
		System.arraycopy(readBlock, 176, data, 0, 64);

		return data;
	}
}