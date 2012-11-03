/*
 * ClassicSector.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ClassicSector {
    private int mIndex;
    private ClassicBlock[] mBlocks;

    public ClassicSector(int index, ClassicBlock[] blocks) {
        mIndex  = index;
        mBlocks = blocks;
    }

    public int getIndex() {
        return mIndex;
    }

    public ClassicBlock[] getBlocks() {
        return mBlocks;
    }

    public ClassicBlock getBlock(int index) {
        return mBlocks[index];
    }

    public Element toXML(Document doc) {
        Element sectorElement = doc.createElement("sector");
        sectorElement.setAttribute("index", String.valueOf(getIndex()));

        Element blocksElement = doc.createElement("blocks");
        for (ClassicBlock block : getBlocks()) {
            blocksElement.appendChild(block.toXML(doc));
        }
        sectorElement.appendChild(blocksElement);

        return sectorElement;
    }

    public byte[] readBlocks(int startBlock, int blockCount) {
        int readBlocks = 0;
        byte[] data = new byte[blockCount * 16];
        for (int index = startBlock; index < (startBlock + blockCount); index++) {
            byte[] blockData = getBlock(index).getData();
            System.arraycopy(blockData, 0, data, readBlocks * 16, blockData.length);
            readBlocks++;
        }
        return data;
    }
}
