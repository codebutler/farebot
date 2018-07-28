/*
 * KMTTransitInfo.java
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codebutler.farebot.transit.kmt;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class KMTTransitInfo extends TransitInfo {


    @NonNull
    public static KMTTransitInfo create(
            @NonNull List<Trip> trips,
            @NonNull ByteArray serialNumberData,
            int currentBalance) {
        return new AutoValue_KMTTransitInfo(trips, serialNumberData, currentBalance);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        Locale localeID=new Locale("in","ID");
        NumberFormat format = NumberFormat.getCurrencyInstance(localeID);
        format.setMaximumFractionDigits(0);
        return format.format(getCurrentBalance());
    }

    @Nullable
    @Override
    public String getSerialNumber() {
        String serial=new String(getSerialNumberData().bytes());
//        byte[] serialNumber = getSerialNumberData().bytes();
//
//        StringBuilder str = new StringBuilder(20);
//
        return serial;
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return "Kartu Multi Trip";
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
    }

    @NonNull
    abstract ByteArray getSerialNumberData();

    abstract int getCurrentBalance();
}

