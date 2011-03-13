/*
 * MifareCard.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
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

package com.codebutler.farebot.mifare;

import android.os.Parcel;
import android.os.Parcelable;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.cepas.CEPASCard;
import com.codebutler.farebot.transit.ClipperTransitData;
import com.codebutler.farebot.transit.EZLinkTransitData;
import com.codebutler.farebot.transit.OrcaTransitData;
import com.codebutler.farebot.transit.TransitData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public abstract class MifareCard implements Parcelable
{
    private byte[] mTagId;

    protected MifareCard (byte[] tagId)
    {
        mTagId = tagId;
    }

    public abstract CardType getCardType();

    public byte[] getTagId () {
        return mTagId;
    }

    public TransitData parseTransitData ()
    {
        if (OrcaTransitData.check(this))
            return new OrcaTransitData(this);
        if (ClipperTransitData.check(this))
            return new ClipperTransitData(this);
        if (EZLinkTransitData.check(this))
        	return new EZLinkTransitData(this);
        return null;
    }

    public static MifareCard fromXml (String xml) throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        
        Element rootElement = doc.getDocumentElement();

        CardType type = CardType.class.getEnumConstants()[Integer.parseInt(rootElement.getAttribute("type"))];
        byte[] id     = Utils.hexStringToByteArray(rootElement.getAttribute("id"));

        switch (type) {
            case MifareDesfire:
                return DesfireCard.fromXml(id, rootElement);
            case CEPAS:
            	return CEPASCard.fromXML(id, rootElement);
            default:
                throw new UnsupportedOperationException("Unsupported card type: " + type);
        }
    }

    public Element toXML () throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        Element element = doc.createElement("card");
        element.setAttribute("type", String.valueOf(getCardType().toInteger()));
        element.setAttribute("id", Utils.getHexString(mTagId));
        doc.appendChild(element);

        return doc.getDocumentElement();
    }

    public void writeToParcel (Parcel parcel, int flags)
    {
        parcel.writeInt(mTagId.length);
        parcel.writeByteArray(mTagId);
    }

    public enum CardType
    {
        MifareClassic(0),
        MifareUltralight(1),
        MifareDesfire(2),
        CEPAS(3);

        private int mValue;

        CardType (int value)
        {
            mValue = value;
        }

        public int toInteger ()
        {
            return mValue;
        }

        public String toString ()
        {
            switch (mValue) {
                case 0:
                    return "MIFARE Classic";
                case 1:
                    return "MIFARE Ultralight";
                case 2:
                    return "MIFARE DESFire";
                case 3:
                	return "CEPAS";
                default:
                    return "Unknown";
            }
        }
    }
}
