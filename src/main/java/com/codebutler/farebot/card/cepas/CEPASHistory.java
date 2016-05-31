/*
 * CEPASHistory.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
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

import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "history")
public class CEPASHistory {
    @Attribute(name = "id") private int mId;
    @ElementList(name = "transaction", inline = true, required = false) private List<CEPASTransaction> mTransactions;
    @Attribute(name = "valid") private boolean mIsValid;
    @Attribute(name = "error", required = false) private String mErrorMessage;

    public static CEPASHistory create(int purseId, byte[] purseData) {
        return new CEPASHistory(purseId, purseData);
    }

    CEPASHistory(int purseId, byte[] purseData) {
        mId = purseId;

        if (purseData != null) {
            mIsValid = true;
            mErrorMessage = "";
            int recordSize = 16;
            int purseCount = purseData.length / recordSize;
            CEPASTransaction[] transactions = new CEPASTransaction[purseCount];
            for (int i = 0; i < purseData.length; i += recordSize) {
                byte[] tempData = new byte[recordSize];
                System.arraycopy(purseData, i, tempData, 0, tempData.length);
                transactions[i / tempData.length] = new CEPASTransaction(tempData);
            }
            mTransactions = Utils.arrayAsList(transactions);
        } else {
            mIsValid = false;
            mErrorMessage = "";
            mTransactions = new ArrayList<>();
        }
    }

    CEPASHistory(int purseId, String errorMessage) {
        mId = purseId;
        mErrorMessage = errorMessage;
        mIsValid = false;
    }

    public CEPASHistory(int purseId, CEPASTransaction[] transactions) {
        mTransactions = Utils.arrayAsList(transactions);
        mId = purseId;
        mIsValid = true;
        mErrorMessage = "";
    }

    @SuppressWarnings("unused")
    private CEPASHistory() { /* For XML Serializer */ }

    public int getId() {
        return mId;
    }

    public List<CEPASTransaction> getTransactions() {
        return mTransactions;
    }

    public boolean isValid() {
        return mIsValid;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
