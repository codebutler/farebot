/*
 * OVChipTransitData.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Eric Butler <eric@codebutler.com>
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

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.ImmutableMapBuilder;
import com.codebutler.farebot.util.Utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class OVChipTransitData extends TransitData {
    public static final int  PROCESS_PURCHASE  =  0x00;
    public static final int  PROCESS_CHECKIN   =  0x01;
    public static final int  PROCESS_CHECKOUT  =  0x02;
    public static final int  PROCESS_TRANSFER  =  0x06;
    public static final int  PROCESS_BANNED    =  0x07;
    public static final int  PROCESS_CREDIT    = -0x02;
    public static final int  PROCESS_NODATA    = -0x03;

    public static final int  AGENCY_TLS        = 0x00;
    public static final int  AGENCY_CONNEXXION = 0x01;
    public static final int  AGENCY_GVB        = 0x02;
    public static final int  AGENCY_HTM        = 0x03;
    public static final int  AGENCY_NS         = 0x04;
    public static final int  AGENCY_RET        = 0x05;
    public static final int  AGENCY_VEOLIA     = 0x07;
    public static final int  AGENCY_ARRIVA     = 0x08;
    public static final int  AGENCY_SYNTUS     = 0x09;
    public static final int  AGENCY_QBUZZ      = 0x0A;
    public static final int  AGENCY_DUO        = 0x0C;    // Could also be 2C though... ( http://www.ov-chipkaart.me/forum/viewtopic.php?f=10&t=299 )
    public static final int  AGENCY_STORE      = 0x19;
    public static final int  AGENCY_DUO_ALT    = 0x2C;

    private static final byte[] OVC_MANUFACTURER = { (byte) 0x98, (byte) 0x02, (byte) 0x00 /*, (byte) 0x64, (byte) 0x8E */ };
    private static final byte[] OVC_HEADER = new byte[11];
    static {
        OVC_HEADER[0] = -124;
        OVC_HEADER[4] = 6;
        OVC_HEADER[5] = 3;
        OVC_HEADER[6] = -96;
        OVC_HEADER[8] = 19;
        OVC_HEADER[9] = -82;
        OVC_HEADER[10] = -28;
    }

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
        .put(AGENCY_TLS,        "TLS")
        .put(AGENCY_CONNEXXION, "Connexxion") /* or Breng, Hermes, GVU */
        .put(AGENCY_GVB,        "GVB")
        .put(AGENCY_HTM,        "HTM")
        .put(AGENCY_NS,         "NS")
        .put(AGENCY_RET,        "RET")
        .put(AGENCY_VEOLIA,     "Veolia")
        .put(AGENCY_ARRIVA,     "Arriva")     /* or Aquabus */
        .put(AGENCY_SYNTUS,     "Syntus")
        .put(AGENCY_QBUZZ,      "Qbuzz")
        .put(AGENCY_DUO,        "DUO")
        .put(AGENCY_STORE,      "Reseller")   /* used by Albert Heijn, Primera and Hermes busses and maybe even more */
        .put(AGENCY_DUO_ALT,    "DUO")
        .build();

    private final OVChipIndex          mIndex;
    private final OVChipPreamble       mPreamble;
    private final OVChipInfo           mInfo;
    private final OVChipCredit         mCredit;
    private final OVChipTrip[]         mTrips;
    private final OVChipSubscription[] mSubscriptions;

    public static final Creator<OVChipTransitData> CREATOR = new Creator<OVChipTransitData>() {
        public OVChipTransitData createFromParcel(Parcel parcel) {
            return new OVChipTransitData(parcel);
        }

        public OVChipTransitData[] newArray(int size) {
            return new OVChipTransitData[size];
        }
    };

    public static boolean check(Card card) {
        if (!(card instanceof ClassicCard))
            return false;

        ClassicCard classicCard = (ClassicCard) card;

        if (classicCard.getSectors().size() != 40)
            return false;

        // Starting at 0×010, 8400 0000 0603 a000 13ae e401 xxxx 0e80 80e8 seems to exist on all OVC's (with xxxx different).
        // http://www.ov-chipkaart.de/back-up/3-8-11/www.ov-chipkaart.me/blog/index7e09.html?page_id=132
        byte[] blockData = classicCard.getSector(0).readBlocks(1, 1);
        return Arrays.equals(Arrays.copyOfRange(blockData, 0, 11), OVC_HEADER);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        String hex = Utils.getHexString(((ClassicCard) card).getSector(0).getBlock(0).getData(), null);
        String id = hex.substring(0, 8);
        return new TransitIdentity("OV-chipkaart", id);
    }

    public OVChipTransitData(Parcel parcel) {
        mTrips = new OVChipTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, OVChipTrip.CREATOR);

        mSubscriptions = new OVChipSubscription[parcel.readInt()];
        parcel.readTypedArray(mSubscriptions, OVChipSubscription.CREATOR);

        mIndex    = parcel.readParcelable(OVChipIndex.class.getClassLoader());
        mPreamble = parcel.readParcelable(OVChipPreamble.class.getClassLoader());
        mInfo     = parcel.readParcelable(OVChipInfo.class.getClassLoader());
        mCredit   = parcel.readParcelable(OVChipCredit.class.getClassLoader());
    }

    public OVChipTransitData(ClassicCard card) {
        mIndex = new OVChipIndex(card.getSector(39).readBlocks(11, 4));

        OVChipParser parser = new OVChipParser(card, mIndex);
        mCredit   = parser.getCredit();
        mPreamble = parser.getPreamble();
        mInfo     = parser.getInfo();

        List<OVChipTransaction> transactions = new ArrayList<>(Arrays.asList(parser.getTransactions()));
        Collections.sort(transactions, OVChipTransaction.ID_ORDER);

        List<OVChipTrip> trips = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i++) {
            OVChipTransaction transaction = transactions.get(i);

            if (transaction.getValid() != 1) {
                continue;
            }

            if (i < (transactions.size() - 1)) {
                OVChipTransaction nextTransaction = transactions.get(i + 1);
                if (transaction.getId() == nextTransaction.getId()) { // handle two consecutive (duplicate) logins, skip the first one
                    continue;
                } else if (transaction.isSameTrip(nextTransaction)) {
                    trips.add(new OVChipTrip(transaction, nextTransaction));
                    i++;
                    if (i < (transactions.size() - 2)) { // check for two consecutive (duplicate) logouts, skip the second one
                        OVChipTransaction followingTransaction = transactions.get(i + 1);
                        if (nextTransaction.getId() == followingTransaction.getId()) {
                            i++;
                        }
                    }
                    continue;
                }
            }

            trips.add(new OVChipTrip(transaction));
        }

        Collections.sort(trips, OVChipTrip.ID_ORDER);
        mTrips = trips.toArray(new OVChipTrip[trips.size()]);

        List<OVChipSubscription> subs = new ArrayList<>();
        subs.addAll(Arrays.asList(parser.getSubscriptions()));
        Collections.sort(subs, new Comparator<OVChipSubscription>() {
            @Override public int compare(OVChipSubscription s1, OVChipSubscription s2) {
                return Integer.valueOf(s1.getId()).compareTo(s2.getId());
            }
        });

        mSubscriptions = subs.toArray(new OVChipSubscription[subs.size()]);
    }

    public static Date convertDate(int date) {
        return convertDate(date, 0);
    }

    public static Date convertDate(int date, int time) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1997);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, time / 60);
        calendar.set(Calendar.MINUTE, time % 60);

        calendar.add(Calendar.DATE, date);

        return calendar.getTime();
    }

    public static String convertAmount(int amount) {
        DecimalFormat formatter = (DecimalFormat)NumberFormat.getCurrencyInstance();
        formatter.setCurrency(Currency.getInstance("EUR"));
        String symbol = formatter.getCurrency().getSymbol();
        formatter.setNegativePrefix(symbol + "-");
        formatter.setNegativeSuffix("");

        return formatter.format((double)amount / 100.0);
    }

    @Override public String getCardName() {
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

    @Override public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
        parcel.writeInt(mSubscriptions.length);
        parcel.writeTypedArray(mSubscriptions, flags);
        parcel.writeParcelable(mIndex, flags);
        parcel.writeParcelable(mPreamble, flags);
        parcel.writeParcelable(mInfo, flags);
        parcel.writeParcelable(mCredit, flags);
    }

    @Override public String getBalanceString() {
        return OVChipTransitData.convertAmount(mCredit.getCredit());
    }

    @Override public String getSerialNumber() {
        return null;
    }

    @Override public Trip[] getTrips() {
        return mTrips;
    }

    @Override public Refill[] getRefills() {
        return null;
    }

    public Subscription[] getSubscriptions() {
        return mSubscriptions;
    }

    @Override public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem("Hardware Information"));
        items.add(new ListItem("Manufacturer ID", mPreamble.getManufacturer()));
        items.add(new ListItem("Publisher ID",    mPreamble.getPublisher()));

        items.add(new HeaderListItem("General Information"));
        items.add(new ListItem("Serial Number",   mPreamble.getId()));
        items.add(new ListItem("Expiration Date", Utils.longDateFormat(OVChipTransitData.convertDate(mPreamble.getExpdate()))));
        items.add(new ListItem("Card Type",       (mPreamble.getType() == 2 ? "Personal" : "Anonymous")));
        items.add(new ListItem("Issuer",          OVChipTransitData.getShortAgencyName(mInfo.getCompany())));

        items.add(new ListItem("Banned",          ((mCredit.getBanbits() & (char)0xC0) == (char)0xC0) ? "Yes" : "No"));

         if (mPreamble.getType() == 2) {
              items.add(new HeaderListItem("Personal Information"));
              items.add(new ListItem("Birthdate", Utils.longDateFormat(mInfo.getBirthdate())));
        }

        items.add(new HeaderListItem("Credit Information"));
        items.add(new ListItem("Credit Slot ID",    Integer.toString(mCredit.getId())));
        items.add(new ListItem("Last Credit ID",    Integer.toString(mCredit.getCreditId())));
        items.add(new ListItem("Credit",            OVChipTransitData.convertAmount(mCredit.getCredit())));
        items.add(new ListItem("Autocharge",        (mInfo.getActive() == (byte)0x05 ? "Yes" : "No")));
        items.add(new ListItem("Autocharge Limit",  OVChipTransitData.convertAmount(mInfo.getLimit())));
        items.add(new ListItem("Autocharge Charge", OVChipTransitData.convertAmount(mInfo.getCharge())));

        items.add(new HeaderListItem("Recent Slots"));
        items.add(new ListItem("Transaction Slot",   "0x" + Integer.toHexString((char)mIndex.getRecentTransactionSlot())));
        items.add(new ListItem("Info Slot",          "0x" + Integer.toHexString((char)mIndex.getRecentInfoSlot())));
        items.add(new ListItem("Subscription Slot",  "0x" + Integer.toHexString((char)mIndex.getRecentSubscriptionSlot())));
        items.add(new ListItem("Travelhistory Slot", "0x" + Integer.toHexString((char)mIndex.getRecentTravelhistorySlot())));
        items.add(new ListItem("Credit Slot",        "0x" + Integer.toHexString((char)mIndex.getRecentCreditSlot())));

        return items;
    }
}
