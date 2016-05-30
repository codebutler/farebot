/*
 * ClassicSector.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name="sector")
public class ClassicSector {
    @Attribute(name="index") private int mIndex;
    @ElementList(name="blocks", required=false, empty=false) private List<ClassicBlock> mBlocks;

    protected ClassicSector() { }

    public ClassicSector(int index, ClassicBlock[] blocks) {
        mIndex  = index;
        if (blocks == null) {
            // invalid / unauthorised sectors should be null
            mBlocks = null;
        } else {
            mBlocks = Utils.arrayAsList(blocks);
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public List<ClassicBlock> getBlocks() {
        return mBlocks;
    }

    public ClassicBlock getBlock(int index) {
        return mBlocks.get(index);
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
