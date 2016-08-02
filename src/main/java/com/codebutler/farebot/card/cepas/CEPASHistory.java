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

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class CEPASHistory implements Parcelable {

    @NonNull
    public static CEPASHistory create(int id, @NonNull List<CEPASTransaction> transactions) {
        return new AutoValue_CEPASHistory(id, transactions, true, null);
    }

    @NonNull
    public static CEPASHistory create(int purseId, @NonNull String errorMessage) {
        return new AutoValue_CEPASHistory(purseId, null, false, errorMessage);
    }

    @NonNull
    public static CEPASHistory create(int purseId, @Nullable byte[] historyData) {
        if (historyData == null) {
            return new AutoValue_CEPASHistory(purseId, Collections.<CEPASTransaction>emptyList(), false, null);
        }
        int recordSize = 16;
        int purseCount = historyData.length / recordSize;
        List<CEPASTransaction> transactions = new ArrayList<>(purseCount);
        for (int i = 0; i < historyData.length; i += recordSize) {
            byte[] tempData = new byte[recordSize];
            System.arraycopy(historyData, i, tempData, 0, tempData.length);
            transactions.add(CEPASTransaction.create(tempData));
        }
        return new AutoValue_CEPASHistory(purseId, transactions, true, null);
    }

    public abstract int getId();

    @Nullable
    public abstract List<CEPASTransaction> getTransactions();

    public abstract boolean isValid();

    @Nullable
    public abstract String getErrorMessage();
}
