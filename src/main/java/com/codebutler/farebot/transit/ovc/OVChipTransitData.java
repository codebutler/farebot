/*
 * OVChipTransitData.java
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.ImmutableMapBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class OVChipTransitData extends TransitData {

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

    private static Map<Integer, String> sAgencies = new ImmutableMapBuilder<Integer, String>()
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

    private static Map<Integer, String> sShortAgencies = new ImmutableMapBuilder<Integer, String>()
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
    static OVChipTransitData create(
            @NonNull ImmutableList<Trip> trips,
            @NonNull ImmutableList<Subscription> subscriptions,
            @NonNull OVChipIndex index,
            @NonNull OVChipPreamble preamble,
            @NonNull OVChipInfo info,
            @NonNull OVChipCredit credit) {
        return new AutoValue_OVChipTransitData(trips, subscriptions, index, preamble, info, credit);
    }

    @NonNull
    @Override
    public String getCardName() {
        return "OV-Chipkaart";
    }

    public static String getAgencyName(int agency) {
        if (sAgencies.containsKey(agency)) {
            return sAgencies.get(agency);
        }
        return FareBotApplication.getInstance().getString(R.string.unknown_format, "0x" + Long.toString(agency, 16));
    }

    public static String getShortAgencyName(int agency) {
        if (sShortAgencies.containsKey(agency)) {
            return sShortAgencies.get(agency);
        }
        return FareBotApplication.getInstance().getString(R.string.unknown_format, "0x" + Long.toString(agency, 16));
    }

    @NonNull
    @Override
    public String getBalanceString() {
        return OVChipUtil.convertAmount(getCredit().getCredit());
    }

    @NonNull
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
    public List<ListItem> getInfo() {
        OVChipPreamble preamble = getPreamble();
        OVChipInfo info = getOVCInfo();
        OVChipCredit credit = getCredit();
        OVChipIndex index = getIndex();

        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem("Hardware Information"));
        items.add(new ListItem("Manufacturer ID", preamble.getManufacturer()));
        items.add(new ListItem("Publisher ID", preamble.getPublisher()));

        items.add(new HeaderListItem("General Information"));
        items.add(new ListItem("Serial Number", preamble.getId()));
        items.add(new ListItem("Expiration Date", DateFormat.getDateInstance(DateFormat.LONG)
                .format(OVChipUtil.convertDate(preamble.getExpdate()))));
        items.add(new ListItem("Card Type", (preamble.getType() == 2 ? "Personal" : "Anonymous")));
        items.add(new ListItem("Issuer", OVChipTransitData.getShortAgencyName(info.getCompany())));

        items.add(new ListItem("Banned", ((credit.getBanbits() & (char) 0xC0) == (char) 0xC0) ? "Yes" : "No"));

        if (preamble.getType() == 2) {
            items.add(new HeaderListItem("Personal Information"));
            items.add(new ListItem("Birthdate", DateFormat.getDateInstance(DateFormat.LONG)
                    .format(info.getBirthdate())));
        }

        items.add(new HeaderListItem("Credit Information"));
        items.add(new ListItem("Credit Slot ID", Integer.toString(credit.getId())));
        items.add(new ListItem("Last Credit ID", Integer.toString(credit.getCreditId())));
        items.add(new ListItem("Credit", OVChipUtil.convertAmount(credit.getCredit())));
        items.add(new ListItem("Autocharge", (info.getActive() == (byte) 0x05 ? "Yes" : "No")));
        items.add(new ListItem("Autocharge Limit", OVChipUtil.convertAmount(info.getLimit())));
        items.add(new ListItem("Autocharge Charge", OVChipUtil.convertAmount(info.getCharge())));

        items.add(new HeaderListItem("Recent Slots"));
        items.add(new ListItem("Transaction Slot", "0x"
                + Integer.toHexString((char) index.getRecentTransactionSlot())));
        items.add(new ListItem("Info Slot", "0x" + Integer.toHexString((char) index.getRecentInfoSlot())));
        items.add(new ListItem("Subscription Slot", "0x"
                + Integer.toHexString((char) index.getRecentSubscriptionSlot())));
        items.add(new ListItem("Travelhistory Slot", "0x"
                + Integer.toHexString((char) index.getRecentTravelhistorySlot())));
        items.add(new ListItem("Credit Slot", "0x" + Integer.toHexString((char) index.getRecentCreditSlot())));

        return items;
    }

    abstract OVChipIndex getIndex();

    abstract OVChipPreamble getPreamble();

    abstract OVChipInfo getOVCInfo();

    abstract OVChipCredit getCredit();
}
