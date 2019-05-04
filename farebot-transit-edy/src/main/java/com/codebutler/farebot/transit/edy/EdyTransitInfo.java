/*
 * EdyTransitInfo.java
 *
 * Authors:
 * Chris Norden
 * Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.edy;

import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class EdyTransitInfo extends TransitInfo {

    // defines
    static final int FELICA_MODE_EDY_DEBIT = 0x20;
    static final int FELICA_MODE_EDY_CHARGE = 0x02;
    static final int FELICA_MODE_EDY_GIFT = 0x04;

    @NonNull
    public static EdyTransitInfo create(
            @NonNull List<Trip> trips,
            @NonNull ByteArray serialNumberData,
            int currentBalance) {
        return new AutoValue_EdyTransitInfo(trips, serialNumberData, currentBalance);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(getCurrentBalance());
    }

    @Nullable
    @Override
    public String getSerialNumber() {
        byte[] serialNumber = getSerialNumberData().bytes();
        StringBuilder str = new StringBuilder(20);
        for (int i = 0; i < 8; i += 2) {
            str.append(String.format("%02X", serialNumber[i]));
            str.append(String.format("%02X", serialNumber[i + 1]));
            if (i < 6) {
                str.append(" ");
            }
        }
        return str.toString();
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return "Edy";
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

