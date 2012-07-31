/*
 * ClassicUtils.java
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

public class ClassicUtils {
	public static int convertBytePointerToBlock(int index) {
		int block = 0;

		if (index >= 2048) { // Sector 32 (0x800)
			block = 128;
			index -= 2048;
			block += index / 16;
		} else {
			block = index / 16;
		}

		return block;
	}

	public static int convertBytePointerToSector(int index) {
		int sector = 0;

		if (index >= 2048) { // Sector 32 (0x800)
			index = index - 2048;
			sector = 32 + (index / 256);
		} else {
			sector = index / 64;
		}

		return sector;
	}
}