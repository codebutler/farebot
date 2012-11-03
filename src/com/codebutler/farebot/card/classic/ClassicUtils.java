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

package com.codebutler.farebot.card.classic;

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

    public static int sectorToBlock(int sectorIndex) {
        if (sectorIndex < 32) {
            return sectorIndex * 4;
        } else {
            return 32 * 4 + (sectorIndex - 32) * 16;
        }
    }

    public static int blockToSector(int blockIndex) {
        if (blockIndex < 32 * 4) {
            return blockIndex / 4;
        } else {
            return 32 + (blockIndex - 32 * 4) / 16;
        }
    }
}