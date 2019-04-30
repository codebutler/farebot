/*
 * OVChipTransitInfo.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitInfo;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.text.DateFormat;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class OVChipTransitInfo extends TransitInfo {

    static final int PROCESS_PURCHASE = 0x00;
    static final int PROCESS_CHECKIN = 0x01;
    static final int PROCESS_CHECKOUT = 0x02;
    static final int PROCESS_TRANSFER = 0x06;
    static final int PROCESS_BANNED = 0x07;
    static final int PROCESS_CREDIT = -0x02;
    static final int PROCESS_NODATA = -0x03;

    static final int AGENCY_TLS = 0x00;
    static final int AGENCY_CONNEXXION = 0x01;
    static final int AGENCY_GVB = 0x02;
    static final int AGENCY_HTM = 0x03;
    static final int AGENCY_NS = 0x04;
    static final int AGENCY_RET = 0x05;
    static final int AGENCY_VEOLIA = 0x07;
    static final int AGENCY_ARRIVA = 0x08;
    static final int AGENCY_SYNTUS = 0x09;
    static final int AGENCY_QBUZZ = 0x0A;

    // Could also be 2C though... ( http://www.ov-chipkaart.me/forum/viewtopic.php?f=10&t=299 )
    static final int AGENCY_DUO = 0x0C;
    static final int AGENCY_STORE = 0x19;
    static final int AGENCY_DUO_ALT = 0x2C;

    private static final byte[] OVC_MANUFACTURER = {
            (byte) 0x98, (byte) 0x02, (byte) 0x00 /*, (byte) 0x64, (byte) 0x8E */
    };

    private static Map<Integer, String> sAgencies = ImmutableMap.<Integer, String>builder()
            .put(AGENCY_TLS, "Trans Link Systems")
            .put(AGENCY_CONNEXXION, "Connexxion")
            .put(AGENCY_GVB, "Gemeentelijk Vervoersbedrijf")
            .put(AGENCY_HTM, "Haagsche Tramweg-Maatschappij")
            .put(AGENCY_NS, "Nederlandse Spoorwegen")
            .put(AGENCY_RET, "Rotterdamse Elektrische Tram")
            .put(AGENCY_VEOLIA, "Veolia")
            .put(AGENCY_ARRIVA, "Arriva")
            .put(AGENCY_SYNTUS, "Syntus")
            .put(AGENCY_QBUZZ, "Qbuzz")
            .put(AGENCY_DUO, "Dienst Uitvoering Onderwijs")
            .put(AGENCY_STORE, "Reseller")
            .put(AGENCY_DUO_ALT, "Dienst Uitvoering Onderwijs")
            .build();

    private static Map<Integer, String> sShortAgencies = ImmutableMap.<Integer, String>builder()
            .put(AGENCY_TLS, "TLS")
            .put(AGENCY_CONNEXXION, "Connexxion") /* or Breng, Hermes, GVU */
            .put(AGENCY_GVB, "GVB")
            .put(AGENCY_HTM, "HTM")
            .put(AGENCY_NS, "NS")
            .put(AGENCY_RET, "RET")
            .put(AGENCY_VEOLIA, "Veolia")
            .put(AGENCY_ARRIVA, "Arriva")     /* or Aquabus */
            .put(AGENCY_SYNTUS, "Syntus")
            .put(AGENCY_QBUZZ, "Qbuzz")
            .put(AGENCY_DUO, "DUO")
            .put(AGENCY_STORE, "Reseller")   /* used by Albert Heijn, Primera and Hermes busses and maybe even more */
            .put(AGENCY_DUO_ALT, "DUO")
            .build();

    @NonNull
    static OVChipTransitInfo create(
            @NonNull ImmutableList<Trip> trips,
            @NonNull ImmutableList<Subscription> subscriptions,
            @NonNull OVChipIndex index,
            @NonNull OVChipPreamble preamble,
            @NonNull OVChipInfo info,
            @NonNull OVChipCredit credit) {
        return new AutoValue_OVChipTransitInfo(trips, subscriptions, index, preamble, info, credit);
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return "OV-Chipkaart";
    }

    public static String getAgencyName(@NonNull Resources resources, int agency) {
        if (sAgencies.containsKey(agency)) {
            return sAgencies.get(agency);
        }
        return resources.getString(R.string.ovc_unknown_format, "0x" + Long.toString(agency, 16));
    }

    public static String getShortAgencyName(@NonNull Resources resources, int agency) {
        if (sShortAgencies.containsKey(agency)) {
            return sShortAgencies.get(agency);
        }
        return resources.getString(R.string.ovc_unknown_format, "0x" + Long.toString(agency, 16));
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        return OVChipUtil.convertAmount(getCredit().getCredit());
    }

    @Nullable
    @Override
    public String getSerialNumber() {
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
        OVChipPreamble preamble = getPreamble();
        OVChipInfo info = getOVCInfo();
        OVChipCredit credit = getCredit();
        OVChipIndex index = getIndex();

        FareBotUiTree.Builder uiBuilder = FareBotUiTree.builder(context);

        FareBotUiTree.Item.Builder hwUiBuilder = uiBuilder.item()
                .title("Hardware Information");
        hwUiBuilder.item("Manufacturer ID", preamble.getManufacturer());
        hwUiBuilder.item("Publisher ID", preamble.getPublisher());

        FareBotUiTree.Item.Builder generalUiBuilder = uiBuilder.item()
                .title("General Information");
        generalUiBuilder.item("Serial Number", preamble.getId());
        generalUiBuilder.item(
                "Expiration Date",
                DateFormat.getDateInstance(DateFormat.LONG).format(OVChipUtil.convertDate(preamble.getExpdate())));
        generalUiBuilder.item("Card Type", (preamble.getType() == 2 ? "Personal" : "Anonymous"));
        generalUiBuilder.item("Issuer",
                OVChipTransitInfo.getShortAgencyName(context.getResources(), info.getCompany()));
        generalUiBuilder.item("Banned", ((credit.getBanbits() & (char) 0xC0) == (char) 0xC0) ? "Yes" : "No");

        if (preamble.getType() == 2) {
            FareBotUiTree.Item.Builder personalUiBuilder = generalUiBuilder.item().title("Personal Information");
            personalUiBuilder.item("Birthdate", DateFormat.getDateInstance(DateFormat.LONG)
                    .format(info.getBirthdate()));
        }

        FareBotUiTree.Item.Builder creditUiBuilder = uiBuilder.item().title("Credit Information");
        creditUiBuilder.item("Credit Slot ID", Integer.toString(credit.getId()));
        creditUiBuilder.item("Last Credit ID", Integer.toString(credit.getCreditId()));
        creditUiBuilder.item("Credit", OVChipUtil.convertAmount(credit.getCredit()));
        creditUiBuilder.item("Autocharge", (info.getActive() == (byte) 0x05 ? "Yes" : "No"));
        creditUiBuilder.item("Autocharge Limit", OVChipUtil.convertAmount(info.getLimit()));
        creditUiBuilder.item("Autocharge Charge", OVChipUtil.convertAmount(info.getCharge()));

        FareBotUiTree.Item.Builder slotsUiBuilder = uiBuilder.item().title("Recent Slots");
        slotsUiBuilder.item("Transaction Slot", "0x" + Integer.toHexString((char) index.getRecentTransactionSlot()));
        slotsUiBuilder.item("Info Slot", "0x" + Integer.toHexString((char) index.getRecentInfoSlot()));
        slotsUiBuilder.item("Subscription Slot",
                "0x" + Integer.toHexString((char) index.getRecentSubscriptionSlot()));
        slotsUiBuilder.item("Travelhistory Slot",
                "0x" + Integer.toHexString((char) index.getRecentTravelhistorySlot()));
        slotsUiBuilder.item("Credit Slot", "0x" + Integer.toHexString((char) index.getRecentCreditSlot()));

        return uiBuilder.build();
    }

    abstract OVChipIndex getIndex();

    abstract OVChipPreamble getPreamble();

    abstract OVChipInfo getOVCInfo();

    abstract OVChipCredit getCredit();
}
