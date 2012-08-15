/*
 * ClassicCard.java
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

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Parcel;

import com.codebutler.farebot.UnsupportedTagException;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.keys.Keys;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.ovchip.OVChipCard;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;

public class ClassicCard extends Card
{
	private static ClassicProtocol classicTag;
	protected static Keys mKeys;
	protected static boolean mComplete;

    protected ClassicCard(
    		byte[] tagId,
    		Date scannedAt,
    		Keys keys,
    		boolean complete
    ){
    	super(tagId, scannedAt);

    	mKeys = keys;
    	mComplete = complete;
	}

	public static ClassicCard dumpTag (byte[] tagId, Tag tag) throws Exception {
    	MifareClassic tech = MifareClassic.get(tag);
        tech.connect();

        byte[] data = null;
        final String[] techs = tag.getTechList();
        Keys keys = null;
        boolean complete = false;

        try {
        	classicTag = new ClassicProtocol(tech);
        	data = classicTag.getFirstSector();
        	keys = Keys.getAllKeys(classicTag, Utils.getHexString(tagId));

	        if (keys != null)
	        	complete = true;
        } finally {
        	if (tech.isConnected())
        		tech.close();
        }

        mKeys = keys;
        mComplete = complete;

        if (OVChipCard.check(data))
        	return OVChipCard.dumpTag(tagId, tag, keys);
        else
            throw new UnsupportedTagException(techs, Utils.getHexString(tag.getId()));
	}

	public Element toXML () throws Exception
    {
        Element root = super.toXML();

        Document doc = root.getOwnerDocument();

        Element read = doc.createElement("read");
        read.setAttribute("complete", String.valueOf(mComplete ? 1 : 0));
        root.appendChild(read);

        return root;
    }

	public static Card fromXml(byte[] tagId, Date scannedAt, Element rootElement) {
		Element readElement = (Element)rootElement.getElementsByTagName("read").item(0);
		boolean complete = (Integer.parseInt(readElement.getAttribute("complete")) == 1);

		return new ClassicCard(tagId, new Date(), null, complete);
	}

	@Override
	public TransitIdentity parseTransitIdentity() {
		return null;
	}

	@Override
	public TransitData parseTransitData() {
		return null;
	}

	@Override
	public CardType getCardType() {
		return CardType.MifareClassic;
	}

	public boolean getComplete() {
		return mComplete;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);

		parcel.writeParcelable(mKeys, flags);
		parcel.writeInt(mComplete ? 1 : 0);
    }
}