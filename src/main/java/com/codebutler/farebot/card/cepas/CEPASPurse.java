/*
 * CEPASPurse.java
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

import com.codebutler.farebot.xml.HexString;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="purse")
public class CEPASPurse {
    @Attribute(name="auto-load-amount", required=false) private int mAutoLoadAmount;
    @Attribute(name="can", required=false) private HexString mCAN;
    @Attribute(name="cepas-version", required=false) private byte mCepasVersion;
    @Attribute(name="csn", required=false) private HexString mCSN;
    @Attribute(name="error", required=false) private String mErrorMessage;
    @Attribute(name="id", required=false) private int mId;
    @Attribute(name="issuer-data-length", required=false) private int mIssuerDataLength;
    @Attribute(name="issuer-specific-data", required=false) private HexString mIssuerSpecificData;
    @Attribute(name="last-credit-transaction-header", required=false) private HexString mLastCreditTransactionHeader;
    @Attribute(name="last-credit-transaction-trp", required=false) private int mLastCreditTransactionTRP;
    @Attribute(name="last-transaction-debit-options", required=false) private byte mLastTransactionDebitOptionsByte;
    @Attribute(name="last-transaction-trp", required=false) private int mLastTransactionTRP;
    @Attribute(name="logfile-record-count", required=false) private byte mLogfileRecordCount;
    @Attribute(name="purse-balance", required=false) private int mPurseBalance;
    @Attribute(name="purse-expiry-date", required=false) private int mPurseExpiryDate;
    @Attribute(name="purse-status", required=false) private byte mPurseStatus;
    @Attribute(name="purse-creation-date", required=false) private int mPurseCreationDate;
    @Attribute(name="valid", required=false) private boolean mIsValid;
    @Element(name="transaction", required=false) private CEPASTransaction mLastTransactionRecord;

    public static CEPASPurse create(int purseId, byte[] purseData) {
        return new CEPASPurse(purseId, purseData);
    }

    public CEPASPurse(
        int              id,
        byte             cepasVersion,
        byte             purseStatus,
        int              purseBalance,
        int              autoLoadAmount,
        byte[]           can,
        byte[]           csn,
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
        mCAN = new HexString(can);
        mCSN = new HexString(csn);
        mPurseExpiryDate = purseExpiryDate;
        mPurseCreationDate = purseCreationDate;
        mLastCreditTransactionTRP = lastCreditTransactionTRP;
        mLastCreditTransactionHeader = new HexString(lastCreditTransactionHeader);
        mLogfileRecordCount = logfileRecordCount;
        mIssuerDataLength = issuerDataLength;
        mLastTransactionTRP = lastTransactionTRP;
        mLastTransactionRecord = lastTransactionRecord;
        mIssuerSpecificData = new HexString(issuerSpecificData);
        mLastTransactionDebitOptionsByte = lastTransactionDebitOptionsByte;
        mIsValid = true;
        mErrorMessage = "";
    }

    public CEPASPurse(int purseId, String errorMessage) {
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

    public CEPASPurse(int purseId, byte[] purseData) {
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

        byte[] can = new byte[8];
        for (int i=0; i<can.length; i++) {
            can[i] = purseData[8 + i];
        }

        mCAN = new HexString(can);

        byte[] csn = new byte[8];
        for (int i=0; i<csn.length; i++) {
            csn[i] = purseData[16 + i];
        }

        mCSN = new HexString(csn);

        /* Epoch begins January 1, 1995 */
        mPurseExpiryDate   = 788947200 + (86400 * ((0xff00 & (purseData[24] << 8)) | (0x00ff & (purseData[25] << 0))));
        mPurseCreationDate = 788947200 + (86400 * ((0xff00 & (purseData[26] << 8)) | (0x00ff & (purseData[27] << 0))));

        mLastCreditTransactionTRP = ((0xff000000 & (purseData[28] << 24))
                                   | (0x00ff0000 & (purseData[29] << 16))
                                   | (0x0000ff00 & (purseData[30] << 8))
                                   | (0x000000ff & (purseData[31] << 0)));

        byte[] lastCreditTransactionHeader = new byte[8];

        for (int i = 0; i < 8; i++) {
            lastCreditTransactionHeader[i] = purseData[32 + i];
        }

        mLastCreditTransactionHeader = new HexString(lastCreditTransactionHeader);

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

        byte[] issuerSpecificData = new byte[mIssuerDataLength];
        for (int i = 0; i < issuerSpecificData.length; i++) {
            issuerSpecificData[i] = purseData[62+i];
        }
        mIssuerSpecificData = new HexString(issuerSpecificData);

        mLastTransactionDebitOptionsByte = purseData[62+mIssuerDataLength];
    }

    private CEPASPurse() { /* For XML Serializer */ }

    public int getId() {
        return mId;
    }

    public byte getCepasVersion() {
        return mCepasVersion;
    }

    public byte getPurseStatus() {
        return mPurseStatus;
    }

    public int getPurseBalance() {
        return mPurseBalance;
    }

    public int getAutoLoadAmount() {
        return mAutoLoadAmount;
    }

    public byte[] getCAN() {
        return mCAN.getData();
    }

    public byte[] getCSN() {
        return mCSN.getData();
    }

    public int getPurseExpiryDate() {
        return mPurseExpiryDate;
    }

    public int getPurseCreationDate() {
        return mPurseCreationDate;
    }

    public int getLastCreditTransactionTRP() {
        return mLastCreditTransactionTRP;
    }

    public byte[] getLastCreditTransactionHeader() {
        return mLastCreditTransactionHeader.getData();
    }

    public byte getLogfileRecordCount() {
        return mLogfileRecordCount;
    }

    public int getIssuerDataLength() {
        return mIssuerDataLength;
    }

    public int getLastTransactionTRP() {
        return mLastTransactionTRP;
    }

    public CEPASTransaction getLastTransactionRecord() {
        return mLastTransactionRecord;
    }

    public byte[] getIssuerSpecificData() {
        return mIssuerSpecificData.getData();
    }

    public byte getLastTransactionDebitOptionsByte() {
        return mLastTransactionDebitOptionsByte;
    }

    public boolean isValid() {
        return mIsValid;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
