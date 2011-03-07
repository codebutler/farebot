/*
 * DesfireFile.java
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

package com.codebutler.farebot.cepas;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.codebutler.farebot.cepas.CEPASPurse.InvalidCEPASPurse;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class CEPASHistory implements Parcelable
{
    private int                 mId;
    private CEPASTransaction[] mTransactions;
    private static CEPASTransaction[] emptyTransaction = new CEPASTransaction[0];
    
    public static CEPASHistory create (int purseId, byte[] purseData)
    {
        return new CEPASHistory(purseId, purseData);
    }

    public CEPASHistory (int purseId, byte[] purseData)
    {
        mId       = purseId;
        
        if(purseData != null) {
        	int recordSize = 16;
        	int purseCount = purseData.length / recordSize;
        	mTransactions = new CEPASTransaction[purseCount];
        
        	for(int i=0; i<purseData.length; i+=recordSize) {
        		byte[] tempData = new byte[recordSize];
        		for(int j=0; j<tempData.length; j++)
        			tempData[j] = purseData[i+j];
        		mTransactions[i/tempData.length] = new CEPASTransaction(tempData);
        	}
        }
        else
        	mTransactions = emptyTransaction;
        Log.d("CEPASHistory", "There are " + mTransactions.length + " record in purse " + mId);
    }
    
    public CEPASHistory (int purseId, CEPASTransaction[] transactions) {
    	mTransactions = transactions;
    	mId = purseId;
    }

    public int getId () {
        return mId;
    }

    public CEPASTransaction[] getTransactions () {
        return mTransactions;
    }

    
    
    public static final Parcelable.Creator<CEPASHistory> CREATOR = new Parcelable.Creator<CEPASHistory>() {
        public CEPASHistory createFromParcel(Parcel source) {
            int purseId = source.readInt();
            CEPASTransaction[] transactions = (CEPASTransaction[]) source.readParcelableArray(CEPASTransaction.class.getClassLoader()); 

            return new CEPASHistory(purseId, transactions);
        }

        public CEPASHistory[] newArray (int size) {
            return new CEPASHistory[size];
        }
    };

    public void writeToParcel (Parcel parcel, int flags)
    {
    	parcel.writeInt(mId);
    	parcel.writeParcelableArray((Parcelable[])mTransactions, flags);
    }

    public int describeContents ()
    {
        return 0;
    }
    
    
    public static CEPASHistory fromXML(Element element)
    {
    	int id = Integer.parseInt(element.getAttribute("id"));
    	if(element.getAttribute("valid").equals("false"))
    		return new InvalidCEPASHistory(id, element.getAttribute("error"));

        NodeList historyElements = ((Element) element.getElementsByTagName("history").item(0)).getElementsByTagName("transaction");

        CEPASTransaction[] transactions = new CEPASTransaction[historyElements.getLength()];
        for(int i=0; i<historyElements.getLength(); i++)
        	transactions[i] = CEPASTransaction.fromXML((Element)historyElements.item(i));
        
        return new CEPASHistory(id, transactions);
    }
    
    public Element toXML(Document doc) throws Exception
    {
    	Element history = doc.createElement("history");
    	history.setAttribute("id", Integer.toString(mId));
    	if(this instanceof InvalidCEPASHistory) {
    		history.setAttribute("valid", "false");
    		history.setAttribute("error", ((InvalidCEPASHistory)this).getErrorMessage());
    	}
    	else {
    		history.setAttribute("valid", "true");
    		for(CEPASTransaction transaction : mTransactions)
    			history.appendChild(transaction.toXML(doc));
    	}
    	return history;
    }
    
    
    
    public static class InvalidCEPASHistory extends CEPASHistory
    {
        private String mErrorMessage;

        public InvalidCEPASHistory (int fileId, String errorMessage)
        {
            super(fileId, new byte[0]);
            mErrorMessage = errorMessage;
        }

        public String getErrorMessage () {
            return mErrorMessage;
        }
    }
}