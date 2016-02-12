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

package com.codebutler.farebot.card;

import android.nfc.Tag;
import android.util.Log;

import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.util.Utils;
import com.codebutler.farebot.xml.HexString;

import org.apache.commons.lang3.ArrayUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Serializer;

import java.io.StringWriter;
import java.util.Date;

public abstract class Card {
    @Attribute(name="type") private CardType mType;
    @Attribute(name="id") private HexString mTagId;
    @Attribute(name="scanned_at") private Date mScannedAt;
    @Attribute(name="nickname", required=false) private String mNickname;

    protected Card() { }

    protected Card(CardType type, byte[] tagId, Date scannedAt) {
        mType = type;
        mTagId = new HexString(tagId);
        mScannedAt = scannedAt;
    }

    protected Card(CardType type, byte[] tagId, Date scannedAt, String nick) {
        mType = type;
        mTagId = new HexString(tagId);
        mScannedAt = scannedAt;
        mNickname = nick;
    }

    public static Card dumpTag(byte[] tagId, Tag tag) throws Exception {
        final String[] techs = tag.getTechList();
        if (ArrayUtils.contains(techs, "android.nfc.tech.NfcB"))
            return CEPASCard.dumpTag(tag);
        else if (ArrayUtils.contains(techs, "android.nfc.tech.IsoDep"))
            return DesfireCard.dumpTag(tag);
        else if (ArrayUtils.contains(techs, "android.nfc.tech.NfcF"))
            return FelicaCard.dumpTag(tagId, tag);
        else if (ArrayUtils.contains(techs, "android.nfc.tech.MifareClassic"))
            return ClassicCard.dumpTag(tagId, tag);
        else
            throw new UnsupportedTagException(techs, Utils.getHexString(tag.getId()));
    }

    public static Card fromXml(Serializer serializer, String xml) {
        try {
            return serializer.read(Card.class, xml);
        } catch (Exception ex) {
            Log.e("Card", "Failed to deserialize", ex);
            throw new RuntimeException(ex);
        }
    }

    public String toXml(Serializer serializer) {
        try {
            StringWriter writer = new StringWriter();
            serializer.write(this, writer);
            return writer.toString();
        } catch (Exception ex) {
            Log.e("Card", "Failed to serialize", ex);
            throw new RuntimeException(ex);
        }
    }

    public CardType getCardType() {
        return mType;
    }

    public byte[] getTagId() {
        return mTagId.getData();
    }

    public Date getScannedAt() {
        return mScannedAt;
    }

    public void setNickname(String nick){
        mNickname = nick;
    }

    public String getNickname() {
        if(mNickname == null){
            TransitData data = parseTransitData();
            String titleSerial = (data.getSerialNumber() != null) ? data.getSerialNumber()
                    : Utils.getHexString(getTagId(), "");
        }
        return mNickname;
    }

    public boolean hasNickname(){
        return mNickname != null;
    }

    public abstract TransitIdentity parseTransitIdentity();
    public abstract TransitData parseTransitData();
}
