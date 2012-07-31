/*
 * OVChipCard.java
 *
 * Copyright (C) 2011 Eric Butler
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

package com.codebutler.farebot.ovchip;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Parcel;

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.keys.Keys;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.mifareclassic.ClassicCard;
import com.codebutler.farebot.transit.OVChipTransitData;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;

public class OVChipCard extends ClassicCard
{
	private static final String ovchipConstant1 = "840000000603a00013aee4";
	private static final String ovchipConstant2 = "80e8080e8";
	private boolean mComplete;
    private OVChipPreamble mOVChipPreamble;
    private OVChipIndex mOVChipIndex;
    private OVChipCredit mOVChipCredit;
    private OVChipInfo mOVChipInfo;
    private OVChipSubscription[] mOVChipSubscriptions;
    private OVChipTransaction[] mOVChipTransactions;

    protected OVChipCard(
    		byte[] tagId,
    		Date scannedAt,
    		boolean complete,
    		OVChipPreamble ovchipPreamble,
    	    OVChipIndex ovchipIndex,
    	    OVChipCredit ovchipCredit,
    	    OVChipInfo ovchipInfo,
    	    OVChipSubscription[] ovchipSubscriptions,
    	    OVChipTransaction[] ovchipTransactions
    ){
    	super(tagId, scannedAt, null, complete);

    	mComplete = complete;
    	mOVChipPreamble = ovchipPreamble;
    	mOVChipIndex = ovchipIndex;
    	mOVChipCredit = ovchipCredit;
    	mOVChipInfo = ovchipInfo;
    	mOVChipSubscriptions = ovchipSubscriptions;
    	mOVChipTransactions = ovchipTransactions;
	}

    protected OVChipCard(
    		byte[] tagId,
    		Date scannedAt,
    		boolean complete,
    		OVChipPreamble ovchipPreamble
    ){
    	super(tagId, scannedAt, null, complete);

    	mComplete = complete;
    	mOVChipPreamble = ovchipPreamble;
    	mOVChipIndex = null;
    	mOVChipCredit = null;
    	mOVChipInfo = null;
    	mOVChipSubscriptions = null;
    	mOVChipTransactions = null;
	}

    public static Creator<OVChipCard> CREATOR = new Creator<OVChipCard>() {
        public OVChipCard createFromParcel(Parcel source) {
        	int tagIdLength = source.readInt();
            byte[] tagId = new byte[tagIdLength];
            source.readByteArray(tagId);

            Date scannedAt = new Date(source.readLong());

            Keys keys = source.readParcelable(Keys.class.getClassLoader());

            boolean complete = (source.readInt() == 1);

            OVChipPreamble ovchipPreamble = source.readParcelable(OVChipPreamble.class.getClassLoader());

            if (complete)
            {
	            OVChipIndex ovchipIndex = source.readParcelable(OVChipIndex.class.getClassLoader());
	            OVChipCredit ovchipCredit = source.readParcelable(OVChipCredit.class.getClassLoader());
	            OVChipInfo ovchipInfo = source.readParcelable(OVChipInfo.class.getClassLoader());

	            OVChipSubscription[] ovchipSubscriptions = new OVChipSubscription[source.readInt()];
	            for (int i = 0; i < ovchipSubscriptions.length; i++)
	            	ovchipSubscriptions[i] = (OVChipSubscription) source.readParcelable(OVChipSubscription.class.getClassLoader());

	            OVChipTransaction[] ovchipTransactions = new OVChipTransaction[source.readInt()];
	            for (int i = 0; i < ovchipTransactions.length; i++)
	            	ovchipTransactions[i] = (OVChipTransaction) source.readParcelable(OVChipTransaction.class.getClassLoader());

	            return new OVChipCard(tagId, scannedAt, complete, 
	            		ovchipPreamble, ovchipIndex, ovchipCredit, ovchipInfo,
	            	    ovchipSubscriptions, ovchipTransactions);
            }

            return new OVChipCard(tagId, scannedAt, complete, ovchipPreamble);
        }

        public OVChipCard[] newArray(int size) {
            return new OVChipCard[size];
        }
    };

	public static OVChipCard dumpTag (byte[] tagId, Tag tag, Keys keys) throws Exception {
    	MifareClassic tech = MifareClassic.get(tag);
        tech.connect();

        boolean complete = false;
        OVChipPreamble ovchipPreamble;
        OVChipIndex ovchipIndex;
        OVChipCredit ovchipCredit;
        OVChipInfo ovchipInfo;
        OVChipSubscription[] ovchipSubscriptions;
        OVChipTransaction[] ovchipTransactions = new OVChipTransaction[28];

        try {
	         OVChipProtocol ovchipTag = new OVChipProtocol(tech);

	         // We should always be able to read this (default key)
	         ovchipPreamble = ovchipTag.getPreamble();

	         if (keys != null)
	         {
	        	 complete = true;

	        	 ovchipIndex = ovchipTag.getIndex(keys);
	        	 ovchipCredit = ovchipTag.getCredit(ovchipIndex.getRecentCreditSlot(), keys);
	        	 ovchipInfo = ovchipTag.getInfo(ovchipIndex.getRecentInfoSlot(), keys);
	        	 ovchipSubscriptions = ovchipTag.getSubscriptions(ovchipIndex.getRecentSubscriptionSlot(), ovchipIndex.getSubscriptionIndex(), keys);

		         for (int transactionId = 0; transactionId < ovchipTransactions.length; transactionId++) {
		        	 ovchipTransactions[transactionId] = ovchipTag.getTransaction(transactionId, keys);
		         }

		         if (tech.isConnected())
		        		tech.close();

		         return new OVChipCard(tagId, new Date(), complete,
		        		 ovchipPreamble, ovchipIndex, ovchipCredit, ovchipInfo,
		        		 ovchipSubscriptions, ovchipTransactions);
	         }
        } finally {
        	if (tech.isConnected())
        		tech.close();
        }

        return new OVChipCard(tagId, new Date(), complete, ovchipPreamble);
	}

    public static boolean check (byte[] data) throws Exception {
    	String hex = Utils.getHexString(data);

		String unknownConstant1 = hex.substring(32, 54);
		String unknownConstant2 = hex.substring(59, 68);

        return (unknownConstant1.equals(ovchipConstant1) && unknownConstant2.equals(ovchipConstant2));
    } 

	public OVChipPreamble getOVChipPreamble() {
		return mOVChipPreamble;
	}

	public OVChipIndex getOVChipIndex() {
		return mOVChipIndex;
	}

	public OVChipCredit getOVChipCredit() {
		return mOVChipCredit;
	}

	public OVChipInfo getOVChipInfo() {
		return mOVChipInfo;
	}

	public OVChipSubscription[] getOVChipSubscriptions() {
		return mOVChipSubscriptions;
	}

	public OVChipTransaction[] getOVChipTransactions() {
		return mOVChipTransactions;
	}

	public Element toXML () throws Exception {
        Element root = super.toXML();

        Document doc = root.getOwnerDocument();

        Node card = doc.getFirstChild();
        ((Element) card).setAttribute("subtype", "0");

        Element preamble = mOVChipPreamble.toXML(doc);
        root.appendChild(preamble);

        if (mComplete)
        {
	        Element index = mOVChipIndex.toXML(doc);
	        root.appendChild(index);

	        Element credit = mOVChipCredit.toXML(doc);
	        root.appendChild(credit);

	    	Element info = mOVChipInfo.toXML(doc);
	    	root.appendChild(info);

	    	Element subscriptionsElement = doc.createElement("subscriptions");
	        Element transactionsElement = doc.createElement("transactions");

	    	for (OVChipSubscription ovchipSubscriptions : mOVChipSubscriptions)
	    		subscriptionsElement.appendChild(ovchipSubscriptions.toXML(doc));
	        root.appendChild(subscriptionsElement);

	        for (OVChipTransaction ovchipTransactions : mOVChipTransactions)
	        	transactionsElement.appendChild(ovchipTransactions.toXML(doc));
	        root.appendChild(transactionsElement);
        }

        return root;
    }

	public static Card fromXml(byte[] tagId, Date scannedAt, Element rootElement) {
		Element readElement = (Element)rootElement.getElementsByTagName("read").item(0);
		boolean complete = (Integer.parseInt(readElement.getAttribute("complete")) == 1);

		OVChipPreamble ovchipPreamble = OVChipPreamble.fromXML((Element)rootElement.getElementsByTagName("preamble").item(0));

		if (complete)
		{
			OVChipIndex ovchipIndex = OVChipIndex.fromXML((Element)rootElement.getElementsByTagName("index").item(0));
			OVChipCredit ovchipCredit = OVChipCredit.fromXML((Element)rootElement.getElementsByTagName("credit").item(0));
			OVChipInfo ovchipInfo = OVChipInfo.fromXML((Element)rootElement.getElementsByTagName("info").item(0));

	        NodeList subscriptionsElement = ((Element) rootElement.getElementsByTagName("subscriptions").item(0)).getElementsByTagName("subscription");
	        OVChipSubscription[] ovchipSubscriptions = new OVChipSubscription[subscriptionsElement.getLength()];
	        for(int i = 0; i < subscriptionsElement.getLength(); i++)
	        	ovchipSubscriptions[i] = OVChipSubscription.fromXML((Element)subscriptionsElement.item(i));

	        NodeList transactionsElement = ((Element) rootElement.getElementsByTagName("transactions").item(0)).getElementsByTagName("transaction");
	        OVChipTransaction[] ovchipTransactions = new OVChipTransaction[transactionsElement.getLength()];
	        for(int i = 0; i < transactionsElement.getLength(); i++)
	        	ovchipTransactions[i] = OVChipTransaction.fromXML((Element)transactionsElement.item(i));

	        return new OVChipCard(tagId, scannedAt, complete,
	        		ovchipPreamble, ovchipIndex, ovchipCredit, ovchipInfo,
	        		 ovchipSubscriptions, ovchipTransactions);
		}

		return new OVChipCard(tagId, new Date(), complete, ovchipPreamble);
	}

	@Override
	public TransitIdentity parseTransitIdentity() {
		if (OVChipTransitData.check(this))
            return OVChipTransitData.parseTransitIdentity(this);
        return null;
	}

	@Override
	public TransitData parseTransitData() {
		return new OVChipTransitData(this);
	}

	public void writeToParcel(Parcel parcel, int flags) {
		super.writeToParcel(parcel, flags);

		parcel.writeParcelable(mOVChipPreamble, flags);

		if (mComplete)
		{
			parcel.writeParcelable(mOVChipIndex, flags);
			parcel.writeParcelable(mOVChipCredit, flags);
			parcel.writeParcelable(mOVChipInfo, flags);

	        parcel.writeInt(mOVChipSubscriptions.length);
	        for (int i = 0; i < mOVChipSubscriptions.length; i++)
	        	parcel.writeParcelable(mOVChipSubscriptions[i], flags);

	        parcel.writeInt(mOVChipTransactions.length);
	        for (int i = 0; i < mOVChipTransactions.length; i++)
	        	parcel.writeParcelable(mOVChipTransactions[i], flags);
		}
    }
}