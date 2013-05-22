/*
 * ClipperTransitData.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Thanks to:
 * An anonymous contributor for reverse engineering Clipper data and providing
 * most of the code here.
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

import java.text.NumberFormat;
import java.util.*;

public class ClipperTransitData extends TransitData {
    private long            mSerialNumber;
    private short           mBalance;
    private ClipperTrip[]   mTrips;
    private ClipperRefill[] mRefills;

    private static final int  RECORD_LENGTH   = 32;
    private static final int  AGENCY_ACTRAN   = 0x01;
    private static final int  AGENCY_BART     = 0x04;
    private static final int  AGENCY_CALTRAIN = 0x06;
    private static final int  AGENCY_GGT      = 0x0b;
    private static final int  AGENCY_SAMTRANS = 0x0f;
    private static final int  AGENCY_VTA      = 0x11;
    private static final int  AGENCY_MUNI     = 0x12;
    private static final int  AGENCY_FERRY    = 0x19;

    private static final long EPOCH_OFFSET    = 0x83aa7f18;

    private static Map<Integer, String> sAgencies = new HashMap<Integer, String>() {{
        put(AGENCY_ACTRAN,   "Alameda-Contra Costa Transit District");
        put(AGENCY_BART,     "Bay Area Rapid Transit");
        put(AGENCY_CALTRAIN, "Caltrain");
        put(AGENCY_GGT,      "Golden Gate Transit");
        put(AGENCY_SAMTRANS, "San Mateo County Transit District");
        put(AGENCY_VTA,      "Santa Clara Valley Transportation Authority");
        put(AGENCY_MUNI,     "San Francisco Municipal");
        put(AGENCY_FERRY,    "Golden Gate Ferry");
    }};
    
    private static Map<Integer, String> sShortAgencies = new HashMap<Integer, String>() {{
        put(AGENCY_ACTRAN,   "ACTransit");
        put(AGENCY_BART,     "BART");
        put(AGENCY_CALTRAIN, "Caltrain");
        put(AGENCY_GGT,      "GGT");
        put(AGENCY_SAMTRANS, "SAMTRANS");
        put(AGENCY_VTA,      "VTA");
        put(AGENCY_MUNI,     "Muni");
        put(AGENCY_FERRY,    "Ferry");
    }};

    private static Map<Long, Station> sBartStations = new HashMap<Long, Station>() {{
        put((long)0x01, new Station("Colma Station",                             "Colma",                "37.68468",  "-122.46626"));
        put((long)0x02, new Station("Daly City Station",                         "Daly City",            "37.70608",  "-122.46908"));
        put((long)0x03, new Station("Balboa Park Station",                       "Balboa Park",          "37.721556", "-122.447503"));
        put((long)0x04, new Station("Glen Park Station",                         "Glen Park",            "37.733118", "-122.433808"));
        put((long)0x05, new Station("24th St. Mission Station",                  "24th St.",             "37.75226",  "-122.41849"));
        put((long)0x06, new Station("16th St. Mission Station",                  "16th St.",             "37.765228", "-122.419478"));
        put((long)0x07, new Station("Civic Center Station",                      "Civic Center",         "37.779538", "-122.413788"));
        put((long)0x08, new Station("Powell Street Station",                     "Powell St.",           "37.784970", "-122.40701"));
        put((long)0x09, new Station("Montgomery St. Station",                    "Montgomery",           "37.789336", "-122.401486"));
        put((long)0x0a, new Station("Embarcadero Station",                       "Embarcadero",          "37.793086", "-122.396276"));
        put((long)0x0c, new Station("12th Street Oakland City Center",           "12th St.",             "37.802956", "-122.2720367"));
        put((long)0x0d, new Station("19th Street Oakland Station",               "19th St.",             "37.80762",  "-122.26886"));
        put((long)0x0f, new Station("Rockridge Station",                         "Rockridge",            "37.84463",  "-122.251825"));
        put((long)0x13, new Station("Walnut Creek Station",                      "Walnut Creek",         "37.90563",  "-122.06744"));
        put((long)0x14, new Station("Concord Station",                           "Concord",              "37.97376",  "-122.02903"));
        put((long)0x15, new Station("North Concord/Martinez Station",            "N. Concord/Martinez",  "38.00318",  "-122.02463"));
        put((long)0x17, new Station("Pittsburg/Bay Point Station",               "Pittsburg/Bay Pt",     "38.01892",  "-121.94240"));
        put((long)0x18, new Station("Downtown Berkeley Station",                 "Berkeley",             "37.869868", "-122.268051"));
        put((long)0x19, new Station("North Berkeley Station",                    "North Berkeley",       "37.874026", "-122.283882"));
        put((long)0x20, new Station("Coliseum/Oakland Airport BART",             "Coliseum/OAK",         "37.754270", "-122.197757"));
        put((long)0x1a, new Station("El Cerrito Plaza Station",                  "El Cerrito Plaza",     "37.903959", "-122.299271"));
        put((long)0x1b, new Station("El Cerrito Del Norte Station",              "El Cerrito Del Norte", "37.925651", "-122.317219"));
        put((long)0x1c, new Station("Richmond Station",                          "Richmond",             "37.93730",  "-122.35338"));
        put((long)0x1d, new Station("Lake Merritt Station",                      "Lake Merritt",         "37.79761",  "-122.26564"));
        put((long)0x1f, new Station("Coliseum/Oakland Airport Station",          "Coliseum/OAK",         "37.75256",  "-122.19806"));
        put((long)0x22, new Station("Hayward Station",                           "Hayward",              "37.670387", "-122.088002"));
        put((long)0x23, new Station("South Hayward Station",                     "South Hayward",        "37.634800", "-122.057551"));
        put((long)0x24, new Station("Union City Station",                        "Union City",           "37.591203", "-122.017854"));
        put((long)0x25, new Station("Fremont Station",                           "Fremont",              "37.557727", "-121.976395"));
        put((long)0x26, new Station("Daly City Station",                         "Daly City",            "37.7066",   "-122.4696"));
        put((long)0x28, new Station("South San Francisco Station",               "South SF",             "37.6744",   "-122.442"));
        put((long)0x29, new Station("San Bruno Station",                         "San Bruno",            "37.63714",  "-122.415622"));
        put((long)0x2a, new Station("San Francisco Int'l Airport Station",       "SFO",                  "37.61590",  "-122.39263"));
        put((long)0x2b, new Station("Millbrae Station",                          "Millbrae",             "37.599935", "-122.386478"));
    }};

    private static Map<Long, String> sFerryRoutes = new HashMap<Long, String>() {{
        put((long)0x03, "Larkspur");
        put((long)0x04, "San Francisco");
    }};

    private static Map<Long, Station> sFerryTerminals = new HashMap<Long, Station>() {{
        put((long)0x01, new Station("San Francisco Ferry Building", "San Francisco", "37.795873", "-122.391987"));
        put((long)0x03, new Station("Larkspur Ferry Terminal", "Larkspur", "37.945509", "-122.50916"));
    }};

    public static boolean check (Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x9011f2) != null);
    }
    
    public static Creator<ClipperTransitData> CREATOR = new Creator<ClipperTransitData>() {
        public ClipperTransitData createFromParcel(Parcel parcel) {
            return new ClipperTransitData(parcel);
        }

        public ClipperTransitData[] newArray(int size) {
            return new ClipperTransitData[size];
        }
    };
        
    public static TransitIdentity parseTransitIdentity (Card card) {
        try {
           byte[] data = ((DesfireCard) card).getApplication(0x9011f2).getFile(0x08).getData();
           return new TransitIdentity("Clipper", String.valueOf(Utils.byteArrayToLong(data, 1, 4)));
       } catch (Exception ex) {
           throw new RuntimeException("Error parsing Clipper serial", ex);
       }
    }


    public ClipperTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
        mBalance      = (short) parcel.readLong();
                
        mTrips = new ClipperTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, ClipperTrip.CREATOR);
        
        mRefills = new ClipperRefill[parcel.readInt()];
        parcel.readTypedArray(mRefills, ClipperRefill.CREATOR);
    }
    
    public ClipperTransitData (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data;

        try {
            data = desfireCard.getApplication(0x9011f2).getFile(0x08).getData();
            mSerialNumber = Utils.byteArrayToLong(data, 1, 4);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper serial", ex);
        }

        try {
            data = desfireCard.getApplication(0x9011f2).getFile(0x02).getData();
            mBalance = (short) (((0xFF & data[18]) << 8) | (0xFF & data[19]));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper balance", ex);
        }

        try {
            mTrips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper trips", ex);
        }

        try {
            mRefills = parseRefills(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing Clipper refills", ex);
        }

        setBalances();
    }

    @Override
    public String getCardName () {
        return "Clipper";
    }

    @Override
    public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format(mBalance / 100.0);
    }

    @Override
    public String getSerialNumber () {
        return Long.toString(mSerialNumber);
    }

    @Override
    public Trip[] getTrips () {
        return mTrips;
    }

    public ClipperRefill[] getRefills () {
        return mRefills;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    private ClipperTrip[] parseTrips (DesfireCard card) {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x0e);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte [] data = file.getData();
        int pos = data.length - RECORD_LENGTH;
        List<ClipperTrip> result = new ArrayList<ClipperTrip>();
        while (pos > 0) {
            byte[] slice = Utils.byteArraySlice(data, pos, RECORD_LENGTH);
            final ClipperTrip trip = createTrip(slice);
            if (trip != null) {
                // Some transaction types are temporary -- remove previous trip with the same timestamp.
                ClipperTrip existingTrip = Utils.findInList(result, new Utils.Matcher<ClipperTrip>() {
                    @Override
                    public boolean matches(ClipperTrip otherTrip) {
                        return trip.getTimestamp() == otherTrip.getTimestamp();
                    }
                });
                if (existingTrip != null) {
                    if (existingTrip.getExitTimestamp() != 0) {
                        // Old trip has exit timestamp, and is therefore better.
                        pos -= RECORD_LENGTH;
                        continue;
                    } else {
                        result.remove(existingTrip);
                    }
                }
                result.add(trip);
            }
            pos -= RECORD_LENGTH;
        }
        ClipperTrip[] useLog = new ClipperTrip[result.size()];
        result.toArray(useLog);

        Arrays.sort(useLog, new Trip.Comparator());

        return useLog;
    }

    private ClipperTrip createTrip (byte[] useData) {
        long timestamp, exitTimestamp, fare, agency, from, to, route;

        timestamp     = Utils.byteArrayToLong(useData,  0xc, 4);
        exitTimestamp = Utils.byteArrayToLong(useData, 0x10, 4);
        fare          = Utils.byteArrayToLong(useData,  0x6, 2);
        agency        = Utils.byteArrayToLong(useData,  0x2, 2);
        from          = Utils.byteArrayToLong(useData, 0x14, 2);
        to            = Utils.byteArrayToLong(useData, 0x16, 2);
        route         = Utils.byteArrayToLong(useData, 0x1c, 2);

        if (agency == 0)
            return null;

        // Use a magic number to offset the timestamp
        timestamp -= EPOCH_OFFSET;

        return new ClipperTrip(timestamp, exitTimestamp, fare, agency, from, to, route);
    }

    private ClipperRefill[] parseRefills (DesfireCard card) {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x04);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte [] data = file.getData();
        int pos = data.length - RECORD_LENGTH;
        List<ClipperRefill> result = new ArrayList<ClipperRefill>();
        while (pos > 0) {
            byte[] slice = Utils.byteArraySlice(data, pos, RECORD_LENGTH);
            ClipperRefill refill = createRefill(slice);
            if (refill != null)
                result.add(refill);
            pos -= RECORD_LENGTH;
        }
        ClipperRefill[] useLog = new ClipperRefill[result.size()];
        useLog = result.toArray(useLog);
        Arrays.sort(useLog, new Comparator<ClipperRefill>() {
            public int compare(ClipperRefill r, ClipperRefill r1) {
                return Long.valueOf(r1.getTimestamp()).compareTo(Long.valueOf(r.getTimestamp()));
            }
        });
        return useLog;
    }

    private ClipperRefill createRefill (byte[] useData) {
        long timestamp, amount, agency, machineid;

        timestamp = Utils.byteArrayToLong(useData, 0x4, 4);
        agency    = Utils.byteArrayToLong(useData, 0x2, 2);
        machineid = Utils.byteArrayToLong(useData, 0x8, 4);
        amount    = Utils.byteArrayToLong(useData, 0xe, 2);

        if (timestamp == 0)
            return null;

        timestamp -= EPOCH_OFFSET;
        return new ClipperRefill(timestamp, amount, agency, machineid);
    }

    private void setBalances() {
        int trip_idx = 0;
        int refill_idx = 0;
        long balance = (long) mBalance;

        while (trip_idx < mTrips.length) {
            while (refill_idx < mRefills.length &&
                    mRefills[refill_idx].getTimestamp() >
                        mTrips[trip_idx].getTimestamp()) {
                balance -= mRefills[refill_idx].mAmount;
                refill_idx++;
            }
            ((ClipperTrip)mTrips[trip_idx]).mBalance = balance;
            balance += ((ClipperTrip)mTrips[trip_idx]).mFare;
            trip_idx++;
        }
    }

    public static String getAgencyName(int agency) {
        if (sAgencies.containsKey(agency)) {
            return sAgencies.get(agency);
        }
        return FareBotApplication.getInstance().getString(R.string.unknown_format, "0x" + Long.toString(agency, 16));
    }

    public static String getShortAgencyName (int agency) {
        if (sShortAgencies.containsKey(agency)) {
            return sShortAgencies.get(agency);
        }
        return FareBotApplication.getInstance().getString(R.string.unknown_format, "0x" + Long.toString(agency, 16));
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mSerialNumber);
        parcel.writeLong(mBalance);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips,  flags);

        parcel.writeInt(mRefills.length);
        parcel.writeTypedArray(mRefills, flags);
    }

    public static class ClipperTrip extends Trip {
        private final long mTimestamp;
        private final long mExitTimestamp;
        private final long mFare;
        private final long mAgency;
        private final long mFrom;
        private final long mTo;
        private final long mRoute;
        private long mBalance;

        public ClipperTrip (long timestamp, long exitTimestamp, long fare, long agency, long from, long to, long route) {
            mTimestamp      = timestamp;
            mExitTimestamp  = exitTimestamp;
            mFare           = fare;
            mAgency         = agency;
            mFrom           = from;
            mTo             = to;
            mRoute          = route;
            mBalance        = 0;
        }

        public static Creator<ClipperTrip> CREATOR = new Creator<ClipperTrip>() {
            public ClipperTrip createFromParcel(Parcel parcel) {
                return new ClipperTrip(parcel);
            }

            public ClipperTrip[] newArray(int size) {
                return new ClipperTrip[size];
            }
        };

        private ClipperTrip (Parcel parcel) {
            mTimestamp     = parcel.readLong();
            mExitTimestamp = parcel.readLong();
            mFare          = parcel.readLong();
            mAgency        = parcel.readLong();
            mFrom          = parcel.readLong();
            mTo            = parcel.readLong();
            mRoute         = parcel.readLong();
            mBalance       = parcel.readLong();
        }

        @Override
        public long getTimestamp () {
            return mTimestamp;
        }

        @Override
        public long getExitTimestamp () {
            return mExitTimestamp;
        }

        @Override
        public String getAgencyName () {
            return ClipperTransitData.getAgencyName((int)mAgency);
        }

        @Override
        public String getShortAgencyName () {
            return ClipperTransitData.getShortAgencyName((int)mAgency);
        }

        @Override
        public String getRouteName () {
            if (mAgency == AGENCY_FERRY &&
                sFerryRoutes.containsKey(mRoute)) {
                return sFerryRoutes.get(mRoute);
            } else {
                // FIXME: Need to find bus route #s
                // return "(Route 0x" + Long.toString(mRoute, 16) + ")";
                return null;
            }
        }

        @Override
        public String getFareString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format((double)mFare / 100.0);
        }

        @Override
        public double getFare () {
            return mFare;
        }

        @Override
        public String getBalanceString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.0);
        }

        @Override
        public Station getStartStation() {
            if (mAgency == AGENCY_BART) {
                if (sBartStations.containsKey(mFrom)) {
                    return sBartStations.get(mFrom);
                }
            } else if (mAgency == AGENCY_FERRY) {
                if (sFerryTerminals.containsKey(mFrom)) {
                    return sFerryTerminals.get(mFrom);
                }
            }
            return null;
        }

        @Override
        public Station getEndStation() {
            if (mAgency == AGENCY_BART) {
                if (sBartStations.containsKey(mTo)) {
                    return sBartStations.get(mTo);
                }
            } else if (mAgency == AGENCY_FERRY) {
                if (sFerryTerminals.containsKey(mTo)) {
                    return sFerryTerminals.get(mTo);
                }
            }
            return null;
        }

        @Override
        public String getStartStationName () {
            if (mAgency == AGENCY_BART || mAgency == AGENCY_FERRY) {
                Station station = getStartStation();
                if (station != null)
                    return station.getShortStationName();
                else
                    return "Station #0x" + Long.toString(mFrom, 16);
            } else if (mAgency == AGENCY_MUNI) {
                return null; // Coach number is not collected
            } else if (mAgency == AGENCY_GGT || mAgency == AGENCY_CALTRAIN) {
                return "Zone #" + mFrom;
            } else {
                return "(Unknown Station)";
            }
        }

        @Override
        public String getEndStationName () {
            if (mAgency == AGENCY_BART) {
                Station station = getEndStation();
                if (station != null)
                    return sBartStations.get(mTo).getShortStationName();
                else
                    return "Station #0x" + Long.toString(mTo, 16);
            } else if (mAgency == AGENCY_MUNI) {
                return null; // Coach number is not collected
            } else if (mAgency == AGENCY_GGT || mAgency == AGENCY_CALTRAIN ||
                       mAgency == AGENCY_FERRY) {
                if (mTo == 0xffff)
                    return "(End of line)";
                return "Zone #" + mTo;
            } else {
                return "(Unknown Station)";
            }
        }

        @Override
        public Mode getMode() {
            if (mAgency == AGENCY_ACTRAN)
                return Mode.BUS;
            if (mAgency == AGENCY_BART)
                return Mode.METRO;
            if (mAgency == AGENCY_CALTRAIN)
                return Mode.TRAIN;
            if (mAgency == AGENCY_GGT)
                return Mode.BUS;
            if (mAgency == AGENCY_SAMTRANS)
                return Mode.BUS;
            if (mAgency == AGENCY_VTA)
                return Mode.BUS; // FIXME: or Mode.TRAM for light rail
            if (mAgency == AGENCY_MUNI)
                return Mode.BUS; // FIXME: or Mode.TRAM for "Muni Metro"
            if (mAgency == AGENCY_FERRY)
                return Mode.FERRY;
            return Mode.OTHER;
        }

        @Override
        public boolean hasTime() {
            return true;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeLong(mTimestamp);
            parcel.writeLong(mExitTimestamp);
            parcel.writeLong(mFare);
            parcel.writeLong(mAgency);
            parcel.writeLong(mFrom);
            parcel.writeLong(mTo);
            parcel.writeLong(mRoute);
            parcel.writeLong(mBalance);
        }

        public int describeContents() {
            return 0;
        }
    }

    public static class ClipperRefill extends Refill {
        private final long mTimestamp;
        private final long mAmount;
        private final long mMachineID;
        private final long mAgency;

        public static Creator<ClipperRefill> CREATOR = new Creator<ClipperRefill>() {
            public ClipperRefill createFromParcel(Parcel parcel) {
                return new ClipperRefill(parcel);
            }

            public ClipperRefill[] newArray(int size) {
                return new ClipperRefill[size];
            }
        };

        public ClipperRefill (long timestamp, long amount, long agency, long machineid) {
            mTimestamp  = timestamp;
            mAmount     = amount;
            mMachineID  = machineid;
            mAgency     = agency;
        }

        public ClipperRefill(Parcel parcel) {
            mTimestamp = parcel.readLong();
            mAmount    = parcel.readLong();
            mMachineID = parcel.readLong();
            mAgency    = parcel.readLong();
        }

        @Override
        public long getTimestamp () {
            return mTimestamp;
        }

        @Override
        public long getAmount () {
            return mAmount;
        }

        @Override
        public String getAmountString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format((double)mAmount / 100.0);
        }

        public long getMachineID () {
            return mMachineID;
        }

        @Override
        public String getAgencyName () {
            return ClipperTransitData.getAgencyName((int)mAgency);
        }

        @Override
        public String getShortAgencyName () {
            return ClipperTransitData.getShortAgencyName((int) mAgency);
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeLong(mTimestamp);
            parcel.writeLong(mAmount);
            parcel.writeLong(mMachineID);
            parcel.writeLong(mAgency);
        }
    }
}
