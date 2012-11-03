/*
 * DesfireFile.java
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
import com.codebutler.farebot.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CEPASPurse implements Parcelable {
    private final int              mId;
    private final byte             mCepasVersion;
    private final byte             mPurseStatus;
    private final int              mPurseBalance;
    private final int              mAutoLoadAmount;
    private final byte[]           mCAN;
    private final byte[]           mCSN;
    private final int              mPurseExpiryDate;
    private final int              mPurseCreationDate;
    private final int              mLastCreditTransactionTRP;
    private final byte[]           mLastCreditTransactionHeader;
    private final byte             mLogfileRecordCount;
    private final int              mIssuerDataLength;
    private final int              mLastTransactionTRP;
    private final CEPASTransaction mLastTransactionRecord;
    private final byte[]           mIssuerSpecificData;
    private final byte             mLastTransactionDebitOptionsByte;

    private final boolean          mIsValid;
    private final String           mErrorMessage;

    public static CEPASPurse create (int purseId, byte[] purseData) {
        return new CEPASPurse(purseId, purseData);
    }

    public CEPASPurse(
        int              id,
        byte             cepasVersion,
        byte             purseStatus,
        int              purseBalance,
        int              autoLoadAmount,
        byte[]           CAN,
        byte[]           CSN,
        int              purseExpiryDate,
        int              purseCreationDate,
        int              lastCreditTransactionTRP,
        byte[]           lastCreditTransactionHeader,
        byte             logfileRecordCount,
        int              issuerDataLength,
        int              lastTransactionTRP,
        CEPASTransaction lastTransactionRecord,
        byte[]           issuerSpecificData,
        byte             lastTransactionDebitOptionsByte
    ) {
        mId = id;
        mCepasVersion = cepasVersion;
        mPurseStatus = purseStatus;
        mPurseBalance = purseBalance;
        mAutoLoadAmount = autoLoadAmount;
        mCAN = CAN;
        mCSN = CSN;
        mPurseExpiryDate = purseExpiryDate;
        mPurseCreationDate = purseCreationDate;
        mLastCreditTransactionTRP = lastCreditTransactionTRP;
        mLastCreditTransactionHeader = lastCreditTransactionHeader;
        mLogfileRecordCount = logfileRecordCount;
        mIssuerDataLength = issuerDataLength;
        mLastTransactionTRP = lastTransactionTRP;
        mLastTransactionRecord = lastTransactionRecord;
        mIssuerSpecificData = issuerSpecificData;
        mLastTransactionDebitOptionsByte = lastTransactionDebitOptionsByte;
        mIsValid = true;
        mErrorMessage = "";
    }

    public CEPASPurse (int purseId, String errorMessage) {
        mId = purseId;
        mCepasVersion = 0;
        mPurseStatus = 0;
        mPurseBalance = 0;
        mAutoLoadAmount = 0;
        mCAN = null;
        mCSN = null;
        mPurseExpiryDate = 0;
        mPurseCreationDate = 0;
        mLastCreditTransactionTRP = 0;
        mLastCreditTransactionHeader = null;
        mLogfileRecordCount = 0;
        mIssuerDataLength = 0;
        mLastTransactionTRP = 0;
        mLastTransactionRecord = null;
        mIssuerSpecificData = null;
        mLastTransactionDebitOptionsByte = 0;
        mIsValid = false;
        mErrorMessage = errorMessage;
    }

    public CEPASPurse (int purseId, byte[] purseData) {
        int tmp;
        if (purseData == null) {
            purseData = new byte[128];
            mIsValid = false;
            mErrorMessage = "";
        } else {
            mIsValid = true;
            mErrorMessage = "";
        }

        mId           = purseId;
        mCepasVersion = purseData[0];
        mPurseStatus  = purseData[1];

        tmp = (0x00ff0000 & ((purseData[2])) << 16) | (0x0000ff00 & (purseData[3] << 8)) | (0x000000ff & (purseData[4]));
        /* Sign-extend the value */
        if (0 != (purseData[2] & 0x80))
            tmp |= 0xff000000;
        mPurseBalance = tmp;

        tmp = (0x00ff0000 & ((purseData[5])) << 16) | (0x0000ff00 & (purseData[6] << 8)) | (0x000000ff & (purseData[7]));
        /* Sign-extend the value */
        if (0 != (purseData[5] & 0x80))
            tmp |= 0xff000000;
        mAutoLoadAmount = tmp;

        mCAN = new byte[8];
        for (int i=0; i<mCAN.length; i++)
            mCAN[i] = purseData[8+i];

        mCSN = new byte[8];
        for (int i=0; i<mCSN.length; i++)
            mCSN[i] = purseData[16+i];

        /* Epoch begins January 1, 1995 */
        mPurseExpiryDate   = 788947200 + (86400 * ((0xff00 & (purseData[24] << 8)) | (0x00ff & (purseData[25] << 0))));
        mPurseCreationDate = 788947200 + (86400 * ((0xff00 & (purseData[26] << 8)) | (0x00ff & (purseData[27] << 0))));

        mLastCreditTransactionTRP = ((0xff000000 & (purseData[28] << 24))
                                   | (0x00ff0000 & (purseData[29] << 16))
                                   | (0x0000ff00 & (purseData[30] << 8))
                                   | (0x000000ff & (purseData[31] << 0)));

        mLastCreditTransactionHeader = new byte[8];

        for (int i = 0; i < 8; i++)
            mLastCreditTransactionHeader[i] = purseData[32+i];

        mLogfileRecordCount = purseData[40];

        mIssuerDataLength = 0x00ff & purseData[41];

        mLastTransactionTRP = ((0xff000000 & (purseData[42] << 24))
                             | (0x00ff0000 & (purseData[43] << 16))
                             | (0x0000ff00 & (purseData[44] << 8))
                             | (0x000000ff & (purseData[45] << 0))); {
            byte[] tmpTransaction = new byte[16];
            for (int i = 0; i < tmpTransaction.length; i++)
                tmpTransaction[i] = purseData[46+i];
            mLastTransactionRecord = new CEPASTransaction(tmpTransaction);
        }

        mIssuerSpecificData = new byte[mIssuerDataLength];
        for (int i = 0; i < mIssuerSpecificData.length; i++)
            mIssuerSpecificData[i] = purseData[62+i];

        mLastTransactionDebitOptionsByte = purseData[62+mIssuerDataLength];
    }

    public int getId () {
        return mId;
    }

    public byte getCepasVersion () {
        return mCepasVersion;
    }

    public byte getPurseStatus () {
        return mPurseStatus;
    }

    public int getPurseBalance () {
        return mPurseBalance;
    }

    public int getAutoLoadAmount () {
        return mAutoLoadAmount;
    }

    public byte[] getCAN () {
        return mCAN;
    }

    public byte[] getCSN () {
        return mCSN;
    }

    public int getPurseExpiryDate () {
        return mPurseExpiryDate;
    }

    public int getPurseCreationDate () {
        return mPurseCreationDate;
    }

    public int getLastCreditTransactionTRP () {
        return mLastCreditTransactionTRP;
    }

    public byte[] getLastCreditTransactionHeader () {
        return mLastCreditTransactionHeader;
    }

    public byte getLogfileRecordCount () {
        return mLogfileRecordCount;
    }

    public int getIssuerDataLength () {
        return mIssuerDataLength;
    }

    public int getLastTransactionTRP () {
        return mLastTransactionTRP;
    }

    public CEPASTransaction getLastTransactionRecord () {
        return mLastTransactionRecord;
    }

    public byte[] getIssuerSpecificData () {
        return mIssuerSpecificData;
    }

    public byte getLastTransactionDebitOptionsByte () {
        return mLastTransactionDebitOptionsByte;
    }

    public boolean isValid () {
        return mIsValid;
    }

    public String getErrorMessage () {
        return mErrorMessage;
    }

    public static final Parcelable.Creator<CEPASPurse> CREATOR = new Parcelable.Creator<CEPASPurse>() {
        public CEPASPurse createFromParcel(Parcel source) {
            int              purseId = source.readInt();
            byte             cepasVersion;
            byte             purseStatus;
            int              purseBalance;
            int              autoLoadAmount;
            byte[]           CAN;
            byte[]           CSN;
            int              purseExpiryDate;
            int              purseCreationDate;
            int              lastCreditTransactionTRP;
            byte[]           lastCreditTransactionHeader;
            byte             logfileRecordCount;
            int              issuerDataLength;
            int              lastTransactionTRP;
            CEPASTransaction lastTransactionRecord;
            byte[]           issuerSpecificData;
            byte             lastTransactionDebitOptionsByte;

            if (source.readInt() == 0) {
                return new CEPASPurse(purseId, source.readString());
            }

            cepasVersion = source.readByte();
            purseStatus = source.readByte();
            purseBalance = source.readInt();
            autoLoadAmount = source.readInt();
            CAN = new byte[source.readInt()];
            source.readByteArray(CAN);
            CSN = new byte[source.readInt()];
            source.readByteArray(CSN);
            purseExpiryDate = source.readInt();
            purseCreationDate = source.readInt();
            lastCreditTransactionTRP = source.readInt();
            lastCreditTransactionHeader = new byte[source.readInt()];
            source.readByteArray(lastCreditTransactionHeader);
            logfileRecordCount = source.readByte();
            issuerDataLength = source.readInt();
            lastTransactionTRP = source.readInt();
            lastTransactionRecord = (CEPASTransaction) source.readParcelable(CEPASTransaction.class.getClassLoader());
            issuerSpecificData = new byte[source.readInt()];
            source.readByteArray(issuerSpecificData);
            lastTransactionDebitOptionsByte = source.readByte();
            return new CEPASPurse(purseId, cepasVersion, purseStatus, purseBalance,
                autoLoadAmount, CAN, CSN, purseExpiryDate,
                purseCreationDate,
                lastCreditTransactionTRP, lastCreditTransactionHeader,
                logfileRecordCount, issuerDataLength,
                lastTransactionTRP, lastTransactionRecord,
                issuerSpecificData, lastTransactionDebitOptionsByte);
        }

        public CEPASPurse[] newArray (int size) {
            return new CEPASPurse[size];
        }
    };

    public void writeToParcel (Parcel parcel, int flags) {
        parcel.writeInt(mId);
        if (!mIsValid) {
            parcel.writeInt(0);
            parcel.writeString(mErrorMessage);
        } else {
            parcel.writeInt(1);
            parcel.writeByte(mCepasVersion);
            parcel.writeByte(mPurseStatus);
            parcel.writeInt(mPurseBalance);
            parcel.writeInt(mAutoLoadAmount);
            parcel.writeInt(mCAN.length);
            parcel.writeByteArray(mCAN);
            parcel.writeInt(mCSN.length);
            parcel.writeByteArray(mCSN);
            parcel.writeInt(mPurseExpiryDate);
            parcel.writeInt(mPurseCreationDate);
            parcel.writeInt(mLastCreditTransactionTRP);
            parcel.writeInt(mLastCreditTransactionHeader.length);
            parcel.writeByteArray(mLastCreditTransactionHeader);
            parcel.writeByte(mLogfileRecordCount);
            parcel.writeInt(mIssuerDataLength);
            parcel.writeInt(mLastTransactionTRP);
            parcel.writeParcelable(mLastTransactionRecord, flags);
            parcel.writeInt(mIssuerSpecificData.length);
            parcel.writeByteArray(mIssuerSpecificData);
            parcel.writeByte(mLastTransactionDebitOptionsByte);
        }
    }

    public int describeContents () {
        return 0;
    }

    public static CEPASPurse fromXML (Element element) {
        int              id;
        byte             cepasVersion;
        byte             purseStatus;
        int              purseBalance;
        int              autoLoadAmount;
        byte[]           CAN;
        byte[]           CSN;
        int              purseExpiryDate;
        int              purseCreationDate;
        int              lastCreditTransactionTRP;
        byte[]           lastCreditTransactionHeader;
        byte             logfileRecordCount;
        int              issuerDataLength;
        int              lastTransactionTRP;
        CEPASTransaction lastTransactionRecord;
        byte[]           issuerSpecificData;
        byte             lastTransactionDebitOptionsByte;

        id = Integer.parseInt(element.getAttribute("id"));
        if (element.getAttribute("valid").equals("false"))
            return new CEPASPurse(id, element.getAttribute("error"));

        cepasVersion = Byte.parseByte(element.getAttribute("cepas-version"));
        purseStatus = Byte.parseByte(element.getAttribute("purse-status"));
        purseBalance = Integer.parseInt(element.getAttribute("purse-balance"));
        autoLoadAmount = Integer.parseInt(element.getAttribute("auto-load-amount"));
        CAN = Utils.hexStringToByteArray(element.getAttribute("can"));
        CSN = Utils.hexStringToByteArray(element.getAttribute("csn"));
        purseExpiryDate = Integer.parseInt(element.getAttribute("purse-expiry-date"));
        purseCreationDate = Integer.parseInt(element.getAttribute("purse-creation-date"));
        lastCreditTransactionTRP = Integer.parseInt(element.getAttribute("last-credit-transaction-trp"));
        lastCreditTransactionHeader = Utils.hexStringToByteArray(element.getAttribute("last-credit-transaction-header"));
        logfileRecordCount = Byte.parseByte(element.getAttribute("logfile-record-count"));
        issuerDataLength = Integer.parseInt(element.getAttribute("issuer-data-length"));
        lastTransactionTRP= Integer.parseInt(element.getAttribute("last-transaction-trp"));
        issuerSpecificData = Utils.hexStringToByteArray(element.getAttribute("issuer-specific-data"));
        lastTransactionDebitOptionsByte = Byte.parseByte(element.getAttribute("last-transaction-debit-options"));

        lastTransactionRecord = CEPASTransaction.fromXML((Element)element.getElementsByTagName("transaction").item(0));

        return new CEPASPurse(id, cepasVersion, purseStatus, purseBalance,
                              autoLoadAmount, CAN, CSN, purseExpiryDate,
                              purseCreationDate,
                              lastCreditTransactionTRP, lastCreditTransactionHeader,
                              logfileRecordCount, issuerDataLength,
                              lastTransactionTRP, lastTransactionRecord,
                              issuerSpecificData, lastTransactionDebitOptionsByte);
    }

    public Element toXML (Document doc) throws Exception {
        Element purse = doc.createElement("purse");
        if (!mIsValid) {
            purse.setAttribute("id", Integer.toString(mId));
            purse.setAttribute("valid", "false");
            purse.setAttribute("error", getErrorMessage());
        } else {
            purse.setAttribute("valid", "true");
            purse.setAttribute("id", Integer.toString(mId));
            purse.setAttribute("cepas-version", Byte.toString(mCepasVersion));
            purse.setAttribute("purse-status", Byte.toString(mPurseStatus));
            purse.setAttribute("purse-balance", Integer.toString(mPurseBalance));
            purse.setAttribute("auto-load-amount", Integer.toString(mAutoLoadAmount));
            purse.setAttribute("can", Utils.getHexString(mCAN));
            purse.setAttribute("csn", Utils.getHexString(mCSN));
            purse.setAttribute("purse-creation-date", Integer.toString(mPurseCreationDate));
            purse.setAttribute("purse-expiry-date", Integer.toString(mPurseExpiryDate));
            purse.setAttribute("last-credit-transaction-trp", Integer.toString(mLastCreditTransactionTRP));
            purse.setAttribute("last-credit-transaction-header", Utils.getHexString(mLastCreditTransactionHeader));
            purse.setAttribute("logfile-record-count", Byte.toString(mLogfileRecordCount));
            purse.setAttribute("issuer-data-length", Integer.toString(mIssuerDataLength));
            purse.setAttribute("last-transaction-trp", Integer.toString(mLastTransactionTRP));
            purse.setAttribute("issuer-specific-data", Utils.getHexString(mIssuerSpecificData));
            purse.setAttribute("last-transaction-debit-options", Byte.toString(mLastTransactionDebitOptionsByte));
            purse.appendChild(mLastTransactionRecord.toXML(doc));
        }

        return purse;
    }    
}