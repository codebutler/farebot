package com.codebutler.farebot.cepas;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.os.Parcel;
import android.os.Parcelable;

public class CEPASTransaction implements Parcelable {
	private final byte mType;
	private final int mAmount;
	private final int mDate;
	private final String mUserData;
	
	
	public CEPASTransaction(byte[] rawData) {
		int tmp;

		mType = rawData[0];
		
		tmp = (0x00ff0000 & ((rawData[1])) << 16) | (0x0000ff00 & (rawData[2] << 8)) | (0x000000ff & (rawData[3]));
		/* Sign-extend the value */
		if(0 != (rawData[1] & 0x80))
			tmp |= 0xff000000;
		mAmount = tmp;
		
		/* Date is expressed "in seconds", but the epoch is January 1 1995, SGT */
		mDate = ((0xff000000 & (rawData[4] << 24))
			   | (0x00ff0000 & (rawData[5] << 16))
			   | (0x0000ff00 & (rawData[6] << 8))
			   | (0x000000ff & (rawData[7] << 0)))
			   + 788947200 - (16*3600);
		
		byte[] userData = new byte[9];
		for(int i=0; i<8; i++)
			userData[i] = rawData[i+8];
		userData[8] = '\0';
		mUserData = new String(userData);
	}
	
	public CEPASTransaction(byte type, int amount, int date, String userData) {
		mType = type;
		mAmount = amount;
		mDate = date;
		mUserData = userData;
	}
	
	public byte getType() {
		return mType;
	}
	public int getAmount() {
		return mAmount;
	}
	public int getTimestamp() {
		return mDate;
	}
	public String getUserData() {
		return mUserData;
	}
	
    public static final Parcelable.Creator<CEPASTransaction> CREATOR = new Parcelable.Creator<CEPASTransaction>() {
        public CEPASTransaction createFromParcel(Parcel source) {
        	byte type = source.readByte();
        	int amount = source.readInt();
        	int date = source.readInt();
        	String userData = source.readString();
        	return new CEPASTransaction(type, amount, date, userData);
        }

		@Override
		public CEPASTransaction[] newArray(int size) {
			return new CEPASTransaction[size];
		}
    };
    
    public void writeToParcel (Parcel parcel, int flags)
    {
    	parcel.writeByte(mType);
    	parcel.writeInt(mAmount);
    	parcel.writeInt(mDate);
    	parcel.writeString(mUserData);
    }

	@Override
	public int describeContents() {
		return 0;
	}
	
	
    public static CEPASTransaction fromXML(Element element)
    {
    	return new CEPASTransaction(
    			Byte.parseByte(element.getAttribute("type")),
    			Integer.parseInt(element.getAttribute("amount")),
    			Integer.parseInt(element.getAttribute("date")),
    			element.getAttribute("user-data")
    	);
	}

    public Element toXML(Document doc) throws Exception
    {
    	Element transaction = doc.createElement("transaction");
    	transaction.setAttribute("type", Byte.toString(mType));
    	transaction.setAttribute("amount", Integer.toString(mAmount));
    	transaction.setAttribute("date", Integer.toString(mDate));
    	transaction.setAttribute("user-data", mUserData);
    	
    	return transaction;
    }
}