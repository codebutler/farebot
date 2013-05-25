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

import android.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClassicSector {
    private int mIndex;
    private ClassicBlock[] mBlocks;

    public static ClassicSector fromXml(Element sectorElement) {
        int sectorIndex = Integer.parseInt(sectorElement.getAttribute("index"));
        if (sectorElement.hasAttribute("unauthorized") && sectorElement.getAttribute("unauthorized").equals("true")) {
            return new UnauthorizedClassicSector(sectorIndex);
        } else if (sectorElement.hasAttribute("invalid") && sectorElement.getAttribute("invalid").equals("true")) {
            return new InvalidClassicSector(sectorIndex, sectorElement.getAttribute("error"));
        } else {
            Element blocksElement = (Element) sectorElement.getElementsByTagName("blocks").item(0);
            NodeList blockElements = blocksElement.getElementsByTagName("block");
            ClassicBlock[] blocks = new ClassicBlock[blockElements.getLength()];
            for (int j = 0; j < blockElements.getLength(); j++) {
                Element blockElement = (Element) blockElements.item(j);
                String type  = blockElement.getAttribute("type");
                int blockIndex = Integer.parseInt(blockElement.getAttribute("index"));
                Node dataElement = blockElement.getElementsByTagName("data").item(0);
                byte[] data = Base64.decode(dataElement.getTextContent().trim(), Base64.DEFAULT);
                blocks[j] = ClassicBlock.create(type, blockIndex, data);
            }
            return new ClassicSector(sectorIndex, blocks);
        }
    }

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
