/*
 * EdyTransitData.java
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.google.auto.value.AutoValue;

import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class EdyTransitData extends TransitData {

    // defines
    static final int FELICA_MODE_EDY_DEBIT = 0x20;
    static final int FELICA_MODE_EDY_CHARGE = 0x02;
    static final int FELICA_MODE_EDY_GIFT = 0x04;

    private static final int FELICA_SERVICE_EDY_ID = 0x110B;
    private static final int FELICA_SERVICE_EDY_BALANCE = 0x1317;
    private static final int FELICA_SERVICE_EDY_HISTORY = 0x170F;

    @NonNull
    public static EdyTransitData create(@NonNull FelicaCard card) {
        // card ID is in block 0, bytes 2-9, big-endian ordering
        byte[] serialNumber = new byte[8];
        FelicaService serviceID = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_ID);
        List<FelicaBlock> blocksID = serviceID.getBlocks();
        FelicaBlock blockID = blocksID.get(0);
        byte[] dataID = blockID.getData().bytes();
        for (int i = 2; i < 10; i++) {
            serialNumber[i - 2] = dataID[i];
        }

        // current balance info in block 0, bytes 0-3, little-endian ordering
        FelicaService serviceBalance = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_BALANCE);
        List<FelicaBlock> blocksBalance = serviceBalance.getBlocks();
        FelicaBlock blockBalance = blocksBalance.get(0);
        byte[] dataBalance = blockBalance.getData().bytes();
        int currentBalance = Util.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0]);

        // now read the transaction history
        FelicaService serviceHistory = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_HISTORY);
        List<Trip> trips = new ArrayList<>();

        // Read blocks in order
        List<FelicaBlock> blocks = serviceHistory.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            FelicaBlock block = blocks.get(i);
            EdyTrip trip = EdyTrip.create(block);
            trips.add(trip);
        }

        return new AutoValue_EdyTransitData(trips, ByteArray.create(serialNumber), currentBalance);
    }

    public static boolean check(@NonNull FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_EDY) != null);
    }

    @NonNull
    public static TransitIdentity parseTransitIdentity(@NonNull FelicaCard card) {
        return new TransitIdentity("Edy", null);
    }

    @NonNull
    @Override
    public String getBalanceString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(getCurrentBalance());
    }

    @NonNull
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

    @Nullable
    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @NonNull
    @Override
    public String getCardName() {
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

