/*
 * Card.java
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

import android.nfc.Tag;
import android.os.Parcel;
import android.os.Parcelable;
import com.codebutler.farebot.UnsupportedTagException;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.cepas.CEPASCard;
import com.codebutler.farebot.felica.FelicaCard;
import com.codebutler.farebot.transit.TransitData;
import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Date;

public abstract class Card implements Parcelable
{
    private byte[] mTagId;
    private Date   mScannedAt;

    protected Card(byte[] tagId, Date scannedAt)
    {
        mTagId     = tagId;
        mScannedAt = scannedAt;
    }

    public static Card dumpTag(byte[] tagId, Tag tag) throws Exception {
        final String[] techs = tag.getTechList();
        if (ArrayUtils.contains(techs, "android.nfc.tech.NfcB"))
            return CEPASCard.dumpTag(tag);
        else if (ArrayUtils.contains(techs, "android.nfc.tech.IsoDep"))
            return DesfireCard.dumpTag(tag);
        else if (ArrayUtils.contains(techs, "android.nfc.tech.NfcF"))
            return FelicaCard.dumpTag(tagId, tag);
        else
            throw new UnsupportedTagException(techs, Utils.getHexString(tag.getId()));
    }

    public static Card fromXml (String xml) throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        Element rootElement = doc.getDocumentElement();

        CardType type      = CardType.class.getEnumConstants()[Integer.parseInt(rootElement.getAttribute("type"))];
        byte[]   id        = Utils.hexStringToByteArray(rootElement.getAttribute("id"));
        Date     scannedAt = rootElement.hasAttribute("scanned_at") ? new Date(Long.valueOf(rootElement.getAttribute("scanned_at"))) : new Date(0);
        switch (type) {
            case MifareDesfire:
                return DesfireCard.fromXml(id, scannedAt, rootElement);
            case CEPAS:
            	return CEPASCard.fromXML(id, scannedAt, rootElement);
            case FeliCa:
                return FelicaCard.fromXml(id, scannedAt, rootElement);
            default:
                throw new UnsupportedOperationException("Unsupported card type: " + type);
        }
    }

    public abstract CardType getCardType();

    public byte[] getTagId () {
        return mTagId;
    }

    public Date getScannedAt () {
        return mScannedAt;
    }

    public abstract TransitData parseTransitData ();

    public Element toXML () throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        Element element = doc.createElement("card");
        element.setAttribute("type", String.valueOf(getCardType().toInteger()));
        element.setAttribute("id", Utils.getHexString(mTagId));
        element.setAttribute("scanned_at", Long.toString(mScannedAt.getTime()));
        doc.appendChild(element);

        return doc.getDocumentElement();
    }

    public void writeToParcel (Parcel parcel, int flags)
    {
        parcel.writeInt(mTagId.length);
        parcel.writeByteArray(mTagId);
        parcel.writeLong(mScannedAt.getTime());
    }
    
    public final int describeContents ()
    {
        return 0;
    }

    public enum CardType
    {
        MifareClassic(0),
        MifareUltralight(1),
        MifareDesfire(2),
        CEPAS(3),
        FeliCa(4);

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
                case 4:
                    return "FeliCa";
                default:
                    return "Unknown";
            }
        }
    }
}
