/*
 * CEPASPurse.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CEPASPurse implements Parcelable {

    @NonNull
    public static CEPASPurse create(
            int id,
            byte cepasVersion,
            byte purseStatus,
            int purseBalance,
            int autoLoadAmount,
            byte[] can,
            byte[] csn,
            int purseExpiryDate,
            int purseCreationDate,
            int lastCreditTransactionTRP,
            byte[] lastCreditTransactionHeader,
            byte logfileRecordCount,
            int issuerDataLength,
            int lastTransactionTRP,
            CEPASTransaction lastTransactionRecord,
            byte[] issuerSpecificData,
            byte lastTransactionDebitOptionsByte) {
        return new AutoValue_CEPASPurse(
                id,
                cepasVersion,
                purseStatus,
                purseBalance,
                autoLoadAmount,
                ByteArray.create(can),
                ByteArray.create(csn),
                purseExpiryDate,
                purseCreationDate,
                lastCreditTransactionTRP,
                ByteArray.create(lastCreditTransactionHeader),
                logfileRecordCount,
                issuerDataLength,
                lastTransactionTRP,
                lastTransactionRecord,
                ByteArray.create(issuerSpecificData),
                lastTransactionDebitOptionsByte,
                true,
                null);
    }

    @NonNull
    public static CEPASPurse create(int purseId, String errorMessage) {
        return new AutoValue_CEPASPurse(
                purseId,
                (byte) 0,
                (byte) 0,
                0,
                0,
                null,
                null,
                0,
                0,
                0,
                null,
                (byte) 0,
                0,
                0,
                null,
                null,
                (byte) 0,
                false,
                errorMessage);
    }

    @NonNull
    public static CEPASPurse create(int purseId, @NonNull byte[] purseData) {
        boolean isValid;
        String errorMessage;
        if (purseData == null) {
            purseData = new byte[128];
            isValid = false;
            errorMessage = "";
        } else {
            isValid = true;
            errorMessage = "";
        }

        byte cepasVersion = purseData[0];
        byte purseStatus = purseData[1];

        int tmp = (0x00ff0000 & ((purseData[2])) << 16)
                | (0x0000ff00 & (purseData[3] << 8))
                | (0x000000ff & (purseData[4]));
        /* Sign-extend the value */
        if (0 != (purseData[2] & 0x80)) {
            tmp |= 0xff000000;
        }
        int purseBalance = tmp;

        tmp = (0x00ff0000 & ((purseData[5])) << 16)
                | (0x0000ff00 & (purseData[6] << 8))
                | (0x000000ff & (purseData[7]));
        /* Sign-extend the value */
        if (0 != (purseData[5] & 0x80)) {
            tmp |= 0xff000000;
        }
        int autoLoadAmount = tmp;

        byte[] can = new byte[8];
        System.arraycopy(purseData, 8, can, 0, can.length);

        byte[] csn = new byte[8];
        System.arraycopy(purseData, 16, csn, 0, csn.length);

        /* Epoch begins January 1, 1995 */
        int purseExpiryDate = 788947200 + (86400 * ((0xff00 & (purseData[24] << 8)) | (0x00ff & purseData[25])));
        int purseCreationDate = 788947200 + (86400 * ((0xff00 & (purseData[26] << 8)) | (0x00ff & purseData[27])));

        int lastCreditTransactionTRP = ((0xff000000 & (purseData[28] << 24))
                | (0x00ff0000 & (purseData[29] << 16))
                | (0x0000ff00 & (purseData[30] << 8))
                | (0x000000ff & (purseData[31])));

        byte[] lastCreditTransactionHeader = new byte[8];
        System.arraycopy(purseData, 32, lastCreditTransactionHeader, 0, 8);

        byte logfileRecordCount = purseData[40];

        int issuerDataLength = 0x00ff & purseData[41];

        int lastTransactionTRP = ((0xff000000 & (purseData[42] << 24))
                | (0x00ff0000 & (purseData[43] << 16))
                | (0x0000ff00 & (purseData[44] << 8))
                | (0x000000ff & (purseData[45])));
        byte[] tmpTransaction = new byte[16];
        System.arraycopy(purseData, 46, tmpTransaction, 0, tmpTransaction.length);
        CEPASTransaction lastTransactionRecord = CEPASTransaction.create(tmpTransaction);

        byte[] issuerSpecificData = new byte[issuerDataLength];
        System.arraycopy(purseData, 62, issuerSpecificData, 0, issuerSpecificData.length);

        byte lastTransactionDebitOptionsByte = purseData[62 + issuerDataLength];

        return new AutoValue_CEPASPurse(
                purseId,
                cepasVersion,
                purseStatus,
                purseBalance,
                autoLoadAmount,
                ByteArray.create(can),
                ByteArray.create(csn),
                purseExpiryDate,
                purseCreationDate,
                lastCreditTransactionTRP,
                ByteArray.create(lastCreditTransactionHeader),
                logfileRecordCount,
                issuerDataLength,
                lastTransactionTRP,
                lastTransactionRecord,
                ByteArray.create(issuerSpecificData),
                lastTransactionDebitOptionsByte,
                isValid,
                errorMessage);
    }

    public abstract int getId();

    public abstract byte getCepasVersion();

    public abstract byte getPurseStatus();

    public abstract int getPurseBalance();

    public abstract int getAutoLoadAmount();

    public abstract ByteArray getCAN();

    public abstract ByteArray getCSN();

    public abstract int getPurseExpiryDate();

    public abstract int getPurseCreationDate();

    public abstract int getLastCreditTransactionTRP();

    public abstract ByteArray getLastCreditTransactionHeader();

    public abstract byte getLogfileRecordCount();

    public abstract int getIssuerDataLength();

    public abstract int getLastTransactionTRP();

    public abstract CEPASTransaction getLastTransactionRecord();

    public abstract ByteArray getIssuerSpecificData();

    public abstract byte getLastTransactionDebitOptionsByte();

    public abstract boolean isValid();

    @Nullable
    public abstract String getErrorMessage();
}
