/*
 * CEPASHistory.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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

package com.codebutler.farebot.card.cepas;

import android.os.Parcel;
import android.os.Parcelable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CEPASHistory implements Parcelable {
    private static CEPASTransaction[] sEmptyTransaction = new CEPASTransaction[0];

    private int                 mId;
    private CEPASTransaction[]  mTransactions;
    private final boolean       mIsValid;
    private final String        mErrorMessage;

    public static CEPASHistory create (int purseId, byte[] purseData) {
        return new CEPASHistory(purseId, purseData);
    }

    public CEPASHistory (int purseId, byte[] purseData) {
        mId = purseId;

        if (purseData != null) {
            mIsValid = true;
            mErrorMessage = "";
            int recordSize = 16;
            int purseCount = purseData.length / recordSize;
            mTransactions = new CEPASTransaction[purseCount];

            for (int i = 0; i < purseData.length; i += recordSize) {
                byte[] tempData = new byte[recordSize];
                for (int j = 0; j < tempData.length; j++)
                    tempData[j] = purseData[i+j];
                mTransactions[i/tempData.length] = new CEPASTransaction(tempData);
            }
        }
        else {
            mIsValid      = false;
            mErrorMessage = "";
            mTransactions = sEmptyTransaction;
        }
    }

    public CEPASHistory (int purseId, String errorMessage) {
        mId           = purseId;
        mErrorMessage = errorMessage;
        mIsValid      = false;
    }

    public CEPASHistory (int purseId, CEPASTransaction[] transactions) {
        mTransactions = transactions;
        mId           = purseId;
        mIsValid      = true;
        mErrorMessage = "";
    }

    public int getId () {
        return mId;
    }

    public CEPASTransaction[] getTransactions () {
        return mTransactions;
    }

    public boolean isValid () {
        return mIsValid;
    }

    public String getErrorMessage () {
        return mErrorMessage;
    }

    public static final Parcelable.Creator<CEPASHistory> CREATOR = new Parcelable.Creator<CEPASHistory>() {
        public CEPASHistory createFromParcel(Parcel source) {
            int purseId = source.readInt();

            if(source.readInt() == 1) {
                CEPASTransaction[] transactions = new CEPASTransaction[source.readInt()];
                for(int i=0; i<transactions.length; i++)
                    transactions[i] = source.readParcelable(CEPASTransaction.class.getClassLoader());
                return new CEPASHistory(purseId, transactions);
            }
            else
                return new CEPASHistory(purseId, source.readString());
        }

        public CEPASHistory[] newArray (int size) {
            return new CEPASHistory[size];
        }
    };

    public void writeToParcel (Parcel parcel, int flags) {
        parcel.writeInt(mId);
        if (mIsValid) {
            parcel.writeInt(1);
            parcel.writeInt(mTransactions.length);
            for (int i = 0; i < mTransactions.length; i++)
                parcel.writeParcelable(mTransactions[i], flags);
        } else {
            parcel.writeInt(0);
            parcel.writeString(mErrorMessage);
        }
    }

    public int describeContents () {
        return 0;
    }

    public static CEPASHistory fromXML (Element element) {
        int id = Integer.parseInt(element.getAttribute("id"));
        if (element.getAttribute("valid").equals("false"))
            return new CEPASHistory(id, element.getAttribute("error"));

        NodeList transactionElements = element.getElementsByTagName("transaction");

        CEPASTransaction[] transactions = new CEPASTransaction[transactionElements.getLength()];
        for (int i = 0; i < transactionElements.getLength(); i++)
            transactions[i] = CEPASTransaction.fromXML((Element)transactionElements.item(i));

        return new CEPASHistory(id, transactions);
    }

    public Element toXML(Document doc) throws Exception {
        Element history = doc.createElement("history");
        history.setAttribute("id", Integer.toString(mId));
        if (!mIsValid) {
            history.setAttribute("valid", "false");
            history.setAttribute("error", getErrorMessage());
        } else {
            history.setAttribute("valid", "true");
            for(CEPASTransaction transaction : mTransactions)
                history.appendChild(transaction.toXML(doc));
        }
        return history;
    }
}