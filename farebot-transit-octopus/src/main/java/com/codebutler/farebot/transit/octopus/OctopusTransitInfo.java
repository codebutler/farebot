/*
 * OctopusTransitInfo.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Portions based on FelicaCard.java from nfcard project
 * Copyright 2013 Sinpo Wei <sinpowei@gmail.com>
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

package com.codebutler.farebot.transit.octopus;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Reader for Octopus (Hong Kong)
 * https://github.com/micolous/metrodroid/wiki/Octopus
 */
@AutoValue
public abstract class OctopusTransitInfo extends TransitInfo {

    public static final String OCTOPUS_NAME = "Octopus";
    public static final String SZT_NAME = "Shenzhen Tong";
    public static final String DUAL_NAME = "Hu Tong Xing";

    private static final String TAG = "OctopusTransitInfo";

    @NonNull
    public static OctopusTransitInfo create(
            int octopusBalance,
            int shenzenBalance,
            boolean hasOctopus,
            boolean hasShenzen) {
        return new AutoValue_OctopusTransitInfo(
                octopusBalance,
                shenzenBalance,
                hasOctopus,
                hasShenzen);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        if (hasOctopus()) {
            // Octopus balance takes priority 1
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("zh", "HK"));
            return numberFormat.format((double) getOctopusBalance() / 10.);
        } else if (hasShenzhen()) {
            // Shenzhen Tong balance takes priority 2
            return getSztBalanceString();
        } else {
            // Unhandled.
            Log.d(TAG, "Unhandled balance, could not find Octopus or SZT");
            return null;
        }
    }

    @Nullable
    @Override
    public String getSerialNumber() {
        // TODO: Find out where this is on the card.
        return null;
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        if (hasShenzhen()) {
            if (hasOctopus()) {
                return DUAL_NAME;
            } else {
                return SZT_NAME;
            }
        } else {
            return OCTOPUS_NAME;
        }
    }

    // Stub out things we don't support
    @Nullable
    @Override
    public List<Trip> getTrips() {
        return null;
    }

    @Nullable
    @Override
    public List<Subscription> getSubscriptions() {
        return null;
    }

    @Nullable
    @Override
    public List<Refill> getRefills() {
        return null;
    }

    @Nullable
    @Override
    public FareBotUiTree getAdvancedUi(@NonNull Context context) {
        // Dual-mode card, show the CNY balance here.
        if (hasOctopus() && hasShenzhen()) {
            FareBotUiTree.Builder uiBuilder = FareBotUiTree.builder(context);

            FareBotUiTree.Item.Builder apbUiBuilder = uiBuilder.item()
                    .title(R.string.octopus_alternate_purse_balances);

            apbUiBuilder.item(R.string.octopus_szt, getSztBalanceString());

            return uiBuilder.build();
        }
        return null;
    }

    abstract int getOctopusBalance();

    abstract int getShenzhenBalance();

    abstract boolean hasOctopus();

    abstract boolean hasShenzhen();

    @NonNull
    private String getSztBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format((double) getShenzhenBalance() / 10.);
    }
}
