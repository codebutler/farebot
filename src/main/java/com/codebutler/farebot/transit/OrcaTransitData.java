/*
 * OrcaTransitData.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit;

import android.os.Parcel;
import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.ListItem;
import com.codebutler.farebot.R;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.DesfireFile.RecordDesfireFile;
import com.codebutler.farebot.card.desfire.DesfireRecord;
import org.apache.commons.lang3.ArrayUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrcaTransitData extends TransitData {
    private static final int AGENCY_KCM = 0x04;
    private static final int AGENCY_PT  = 0x06;
    private static final int AGENCY_ST  = 0x07;
    private static final int AGENCY_CT  = 0x02;
    private static final int AGENCY_WSF = 0x08;

    // For future use.
    private static final int TRANS_TYPE_PURSE_USE   = 0x0c;
    private static final int TRANS_TYPE_CANCEL_TRIP = 0x01;
    private static final int TRANS_TYPE_TAP_IN      = 0x03;
    private static final int TRANS_TYPE_TAP_OUT     = 0x07;
    private static final int TRANS_TYPE_PASS_USE    = 0x60;

    private int    mSerialNumber;
    private double mBalance;
    private Trip[] mTrips;

    public static boolean check (Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x3010f2) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0xffffff).getFile(0x0f).getData();
            return new TransitIdentity("ORCA", String.valueOf(Utils.byteArrayToInt(data, 4, 4)));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }
    }

    public OrcaTransitData (Parcel parcel) {
        mSerialNumber = parcel.readInt();
        mBalance      = parcel.readDouble();

        parcel.readInt();
        mTrips = (Trip[]) parcel.readParcelableArray(null);
    }
    
    public OrcaTransitData (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data;

        try {
            data = desfireCard.getApplication(0xffffff).getFile(0x0f).getData();
            mSerialNumber = Utils.byteArrayToInt(data, 5, 3);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }

        try {
            data = desfireCard.getApplication(0x3010f2).getFile(0x04).getData();
            mBalance = Utils.byteArrayToInt(data, 41, 2);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA balance", ex);
        }

        try {
            mTrips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA trips", ex);
        }
    }

    @Override
    public String getCardName () {
        return "ORCA";
    }

    @Override
    public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format(mBalance / 100);
    }

    @Override
    public String getSerialNumber () {
        return Integer.toString(mSerialNumber);
    }

    @Override
    public Trip[] getTrips () {
        return mTrips;
    }

    @Override
    public Refill[] getRefills () {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    private Trip[] parseTrips(DesfireCard card) {
        List<Trip> trips = new ArrayList<Trip>();

        DesfireFile file = card.getApplication(0x3010f2).getFile(0x02);
        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) card.getApplication(0x3010f2).getFile(0x02);

            OrcaTrip[] useLog = new OrcaTrip[recordFile.getRecords().length];
            for (int i = 0; i < useLog.length; i++) {
                useLog[i] = new OrcaTrip(recordFile.getRecords()[i]);
            }
            Arrays.sort(useLog, new Trip.Comparator());
            ArrayUtils.reverse(useLog);

            for (int i = 0; i < useLog.length; i++) {
                OrcaTrip trip = useLog[i];
                OrcaTrip nextTrip = (i+1 < useLog.length) ? useLog[i+1] : null;

                if (isSameTrip(trip, nextTrip)) {
                    trips.add(new MergedOrcaTrip(trip, nextTrip));
                    i++;
                    continue;
                }

                trips.add(trip);
            }
        }
        Collections.sort(trips, new Trip.Comparator());
        return trips.toArray(new Trip[trips.size()]);
    }

    private boolean isSameTrip(OrcaTrip firstTrip, OrcaTrip secondTrip) {
        return firstTrip != null && secondTrip != null &&
            firstTrip.mTransType == TRANS_TYPE_TAP_IN &&
            (secondTrip.mTransType ==TRANS_TYPE_TAP_OUT || secondTrip.mTransType == TRANS_TYPE_CANCEL_TRIP) &&
            firstTrip.mAgency == secondTrip.mAgency;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerialNumber);
        parcel.writeDouble(mBalance);

        if (mTrips != null) {
            parcel.writeInt(mTrips.length);
            parcel.writeParcelableArray(mTrips, flags);
        } else {
            parcel.writeInt(0);
        }
    }

    public static class OrcaTrip extends Trip {
        private final long mTimestamp;
        private final long mCoachNum;
        private final long mFare;
        private final long mNewBalance;
        private final long mAgency;
        private final long mTransType;

        private static Station[] sLinkStations = new Station[] {
            new Station("Westlake Station",                   "Westlake",      "47.6113968", "-122.337502"),
            new Station("University Station",                 "University",    "47.6072502", "-122.335754"),
            new Station("Pioneer Square Station",             "Pioneer Sq",    "47.6021461", "-122.33107"),
            new Station("International District Station",     "ID",            "47.5976601", "-122.328217"),
            new Station("Stadium Station",                    "Stadium",       "47.5918121", "-122.327354"),
            new Station("SODO Station",                       "SODO",          "47.5799484", "-122.327515"),
            new Station("Beacon Hill Station",                "Beacon Hill",   "47.5791245", "-122.311287"),
            new Station("Mount Baker Station",                "Mount Baker",   "47.5764389", "-122.297737"),
            new Station("Columbia City Station",              "Columbia City", "47.5589523", "-122.292343"),
            new Station("Othello Station",                    "Othello",       "47.5375366", "-122.281471"),
            new Station("Rainier Beach Station",              "Rainier Beach", "47.5222626", "-122.279579"),
            new Station("Tukwila International Blvd Station", "Tukwila",       "47.4642754", "-122.288391"),
            new Station("Seatac Airport Station",             "Sea-Tac",       "47.4445305", "-122.297012")
        };

        private static Map<Integer, Station> sWSFTerminals = new HashMap<Integer, Station>() {{
            put(10101, new Station("Seattle Terminal",           "Seattle",    "47.602722", "-122.338512"));
            put(10103, new Station("Bainbridge Island Terminal", "Bainbridge", "47.62362",  "-122.51082" ));
        }};

        public OrcaTrip (DesfireRecord record) {
            byte[] useData = record.getData();
            long[] usefulData = new long[useData.length];
    
            for (int i = 0; i < useData.length; i++) {
                usefulData[i] = ((long)useData[i]) & 0xFF;
            }
    
            mTimestamp =
                ((0x0F & usefulData[3]) << 28) |
                (usefulData[4] << 20) |
                (usefulData[5] << 12) |
                (usefulData[6] << 4)  |
                (usefulData[7] >> 4);
    
            mCoachNum = ((usefulData[9] & 0xf) << 12) | (usefulData[10] << 4) | ((usefulData[11] & 0xf0) >> 4);

            if (usefulData[15] == 0x00 || usefulData[15] == 0xFF) {
                // FIXME: This appears to be some sort of special case for transfers and passes.
                mFare = 0;
            } else {
                mFare = (usefulData[15] << 7) | (usefulData[16] >> 1);
            }

            mNewBalance = (usefulData[34] << 8) | usefulData[35];
            mAgency     = usefulData[3] >> 4;
            mTransType  = (usefulData[17]);
        }
        
        public static Creator<OrcaTrip> CREATOR = new Creator<OrcaTrip>() {
            public OrcaTrip createFromParcel(Parcel parcel) {
                return new OrcaTrip(parcel);
            }

            public OrcaTrip[] newArray(int size) {
                return new OrcaTrip[size];
            }
        };

        private OrcaTrip (Parcel parcel) {
            mTimestamp  = parcel.readLong();
            mCoachNum   = parcel.readLong();
            mFare       = parcel.readLong();
            mNewBalance = parcel.readLong();
            mAgency     = parcel.readLong();
            mTransType  = parcel.readLong();
        }

        @Override
        public long getTimestamp() {
            return mTimestamp;
        }

        @Override
        public long getExitTimestamp() {
            return 0;
        }

        @Override
        public String getAgencyName () {
            switch ((int) mAgency) {
                case AGENCY_CT:
                    return "Community Transit";
                case AGENCY_KCM:
                    return "King County Metro Transit";
                case AGENCY_PT:
                    return "Pierce Transit";
                case AGENCY_ST:
                    return "Sound Transit";
                case AGENCY_WSF:
                    return "Washington State Ferries";
            }
            return String.format("Unknown Agency: %s", mAgency);
        }

        @Override
        public String getShortAgencyName () {
            switch ((int) mAgency) {
                case AGENCY_CT:
                    return "CT";
                case AGENCY_KCM:
                    return "KCM";
                case AGENCY_PT:
                    return "PT";
                case AGENCY_ST:
                    return "ST";
                case AGENCY_WSF:
                    return "WSF";
            }
            return String.format("Unknown Agency: %s", mAgency);
        }

        @Override
        public String getRouteName () {
            if (isLink()) {
                return "Link Light Rail";
            } else {
                // FIXME: Need to find bus route #s
                if (mAgency == AGENCY_ST) {
                    return "Express Bus";
                } else if(mAgency == AGENCY_KCM) {
                    return "Bus";
                }
                return null;
            }
        }

        @Override
        public String getFareString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format(mFare / 100.0);
        }

        @Override
        public double getFare () {
            return mFare;
        }

        @Override
        public String getBalanceString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format(mNewBalance / 100);
        }

        @Override
        public Station getStartStation() {
            if (isLink()) {
                int stationNumber = (((int) mCoachNum) % 1000) - 193;
                if (stationNumber < sLinkStations.length) {
                    return sLinkStations[stationNumber];
                }
            } else if (mAgency == AGENCY_WSF) {
                return sWSFTerminals.get((int)mCoachNum);
            }
            return null;
        }

        @Override
        public String getStartStationName () {
            if (isLink()) {
                int stationNumber = (((int) mCoachNum) % 1000) - 193;
                if (stationNumber < sLinkStations.length) {
                    return sLinkStations[stationNumber].getStationName();
                } else {
                    return String.format("Unknown Station #%s", stationNumber);
                }
            } else if (mAgency == AGENCY_WSF) {
                int terminalNumber = (int) mCoachNum;
                if (sWSFTerminals.containsKey(terminalNumber)) {
                    return sWSFTerminals.get(terminalNumber).getStationName();
                } else {
                    return String.format("Unknown Terminal #%s", terminalNumber);
                }
            } else {
                return String.format("Coach #%s", String.valueOf(mCoachNum));
            }
        }

        @Override
        public String getEndStationName () {
            // ORCA tracks destination in a separate record
            return null;
        }

        @Override
        public Station getEndStation () {
            // ORCA tracks destination in a separate record
            return null;
        }

        @Override
        public Mode getMode() {
            if (isLink()) {
                return Mode.METRO;
            } else if (mAgency == AGENCY_WSF) {
                return Mode.FERRY;
            } else {
                return Mode.BUS;
            }
        }

        @Override
        public boolean hasTime() {
            return true;
        }

        public long getCoachNumber() {
            return mCoachNum;
        }

        public long getTransType() {
            return mTransType;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeLong(mTimestamp);
            parcel.writeLong(mCoachNum);
            parcel.writeLong(mFare);
            parcel.writeLong(mNewBalance);
            parcel.writeLong(mAgency);
            parcel.writeLong(mTransType);
        }

        public int describeContents() {
            return 0;
        }

        private boolean isLink () {
            return (mAgency == OrcaTransitData.AGENCY_ST && mCoachNum > 10000);
        }
    }

    public static class MergedOrcaTrip extends Trip {
        private final OrcaTrip mStartTrip;
        private final OrcaTrip mEndTrip;

        public static Creator<MergedOrcaTrip> CREATOR = new Creator<MergedOrcaTrip>() {
            public MergedOrcaTrip createFromParcel(Parcel parcel) {
                return new MergedOrcaTrip(
                    (OrcaTrip) parcel.readParcelable(OrcaTrip.class.getClassLoader()),
                    (OrcaTrip) parcel.readParcelable(OrcaTrip.class.getClassLoader())
                );
            }

            public MergedOrcaTrip[] newArray(int size) {
                return new MergedOrcaTrip[size];
            }
        };

        public MergedOrcaTrip(OrcaTrip startTrip, OrcaTrip endTrip) {
            mStartTrip = startTrip;
            mEndTrip = endTrip;
        }

        @Override
        public long getTimestamp() {
            return mStartTrip.getTimestamp();
        }

        @Override
        public long getExitTimestamp() {
            return mEndTrip.getTimestamp();
        }

        @Override
        public String getRouteName() {
            return mStartTrip.getRouteName();
        }

        @Override
        public String getAgencyName() {
            return mStartTrip.getAgencyName();
        }

        @Override
        public String getShortAgencyName() {
            return mStartTrip.getShortAgencyName();
        }

        @Override
        public String getFareString() {
            if (mEndTrip.mTransType == TRANS_TYPE_CANCEL_TRIP) {
                return FareBotApplication.getInstance().getString(R.string.fare_cancelled_format, mStartTrip.getFareString());
            }
            return mStartTrip.getFareString();
        }

        @Override
        public String getBalanceString() {
            return mEndTrip.getBalanceString();
        }

        @Override
        public String getStartStationName() {
            return mStartTrip.getStartStationName();
        }

        @Override
        public Station getStartStation() {
            return mStartTrip.getStartStation();
        }

        @Override
        public String getEndStationName() {
            return mEndTrip.getStartStationName();
        }

        @Override
        public Station getEndStation() {
            return mEndTrip.getStartStation();
        }

        @Override
        public double getFare() {
            return mStartTrip.getFare();
        }

        @Override
        public Mode getMode() {
            return mStartTrip.getMode();
        }

        @Override
        public boolean hasTime() {
            return mStartTrip.hasTime();
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            mStartTrip.writeToParcel(parcel, flags);
            mEndTrip.writeToParcel(parcel, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
