/*
 * ClassicCard.java
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

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Parcel;
import com.codebutler.farebot.CardHasManufacturingInfo;
import com.codebutler.farebot.CardRawDataFragmentClass;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.fragments.ClassicCardRawDataFragment;
import com.codebutler.farebot.keys.CardKeys;
import com.codebutler.farebot.keys.ClassicCardKeys;
import com.codebutler.farebot.keys.ClassicSectorKey;
import com.codebutler.farebot.transit.BilheteUnicoSPTransitData;
import com.codebutler.farebot.transit.OVChipTransitData;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CardRawDataFragmentClass(ClassicCardRawDataFragment.class)
@CardHasManufacturingInfo(false)
public class ClassicCard extends Card {
    public static final byte[] PREAMBLE_KEY = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    private ClassicSector[] mSectors;

    protected ClassicCard(byte[] tagId, Date scannedAt, ClassicSector[] sectors) {
        super(tagId, scannedAt);
        mSectors = sectors;
    }

    public static ClassicCard dumpTag(byte[] tagId, Tag tag) throws Exception {
        MifareClassic tech = null;

        try {
            tech = MifareClassic.get(tag);
            tech.connect();

            ClassicCardKeys keys = (ClassicCardKeys) CardKeys.forTagId(tagId);

            List<ClassicSector> sectors = new ArrayList<ClassicSector>();

            for (int sectorIndex = 0; sectorIndex < tech.getSectorCount(); sectorIndex++) {
                try {
                    boolean authSuccess = false;

                    ClassicSectorKey sectorKey;
                    if (keys != null && (sectorKey = keys.keyForSector(sectorIndex)) != null) {
                        if (sectorKey.getType().equals(ClassicSectorKey.TYPE_KEYA)) {
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.getKey());
                        } else {
                            authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.getKey());
                        }
                    }

                    if (!authSuccess && sectorIndex == 0) {
                        authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, PREAMBLE_KEY);
                    }

                    if (!authSuccess) {
                        authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT);
                    }

                    if (authSuccess) {
                        List<ClassicBlock> blocks = new ArrayList<ClassicBlock>();
                        // FIXME: First read trailer block to get type of other blocks.
                        int firstBlockIndex = tech.sectorToBlock(sectorIndex);
                        for (int blockIndex = 0; blockIndex < tech.getBlockCountInSector(sectorIndex); blockIndex++) {
                            byte[] data = tech.readBlock(firstBlockIndex + blockIndex);
                            String type = ClassicBlock.TYPE_DATA; // FIXME
                            blocks.add(ClassicBlock.create(type, blockIndex, data));
                        }
                        sectors.add(new ClassicSector(sectorIndex, blocks.toArray(new ClassicBlock[blocks.size()])));
                    } else {
                        sectors.add(new UnauthorizedClassicSector(sectorIndex));
                    }
                } catch (IOException ex) {
                    sectors.add(new InvalidClassicSector(sectorIndex, Utils.getErrorMessage(ex)));
                }
            }

            return new ClassicCard(tagId, new Date(), sectors.toArray(new ClassicSector[sectors.size()]));

        } finally {
            if (tech != null && tech.isConnected()) {
                tech.close();
            }
        }
    }

    public static Card fromXml(byte[] tagId, Date scannedAt, Element rootElement) {
        Element sectorsElement = (Element) rootElement.getElementsByTagName("sectors").item(0);
        NodeList sectorElements = sectorsElement.getElementsByTagName("sector");

        ClassicSector[] sectors = new ClassicSector[sectorElements.getLength()];
        for (int i = 0; i < sectorElements.getLength(); i++) {
            Element sectorElement = (Element) sectorElements.item(i);
            sectors[i] = ClassicSector.fromXml(sectorElement);
        }

        return new ClassicCard(tagId, new Date(), sectors);
    }

    public static final Creator<ClassicCard> CREATOR = new Creator<ClassicCard>() {
        @Override
        public ClassicCard createFromParcel(Parcel source) {
            try {
                return (ClassicCard) ClassicCard.fromXml(source.readString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ClassicCard[] newArray(int size) {
            return new ClassicCard[size];
        }
    };

    public Element toXML() throws Exception {
        Element root = super.toXML();
        Document doc = root.getOwnerDocument();

        Element sectorsElement = doc.createElement("sectors");
        for (ClassicSector sector : mSectors) {
            sectorsElement.appendChild(sector.toXML(doc));
        }
        root.appendChild(sectorsElement);

        return root;
    }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (OVChipTransitData.check(this))
            return OVChipTransitData.parseTransitIdentity(this);
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        if (OVChipTransitData.check(this))
            return new OVChipTransitData(this);
        else if (BilheteUnicoSPTransitData.check(this))
            return new BilheteUnicoSPTransitData(this);
        return null;
    }

    @Override
    public CardType getCardType() {
        return CardType.MifareClassic;
    }

    public ClassicSector[] getSectors() {
        return mSectors;
    }

    public ClassicSector getSector(int index) {
        return mSectors[index];
    }

    public void writeToParcel(Parcel parcel, int flags) {
        try {
            parcel.writeString(Utils.xmlNodeToString(toXML().getOwnerDocument()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}