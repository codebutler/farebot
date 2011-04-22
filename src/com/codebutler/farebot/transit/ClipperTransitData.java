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

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.DesfireFile;
import com.codebutler.farebot.mifare.MifareCard;

import java.text.NumberFormat;
import java.util.*;

public class ClipperTransitData extends TransitData
{
    private long            mSerialNumber;
    private double          mBalance;
    private Trip[]          mTrips;
    private ClipperRefill[] mRefills;

    private static final int  RECORD_LENGTH   = 32;
    private static final int  AGENCY_ACTRAN   = 0x01;
    private static final int  AGENCY_BART     = 0x04;
    private static final int  AGENCY_CALTRAIN = 0x06;
    private static final int  AGENCY_GGT      = 0x0b;
    private static final int  AGENCY_VTA      = 0x11;
    private static final int  AGENCY_MUNI     = 0x12;
    private static final int  AGENCY_FERRY    = 0x19;

    private static final long EPOCH_OFFSET    = 0x83aa7f18;

    private static Map<Integer, String> sAgencies = new HashMap<Integer, String>() {
        {
            put(AGENCY_ACTRAN, "Alameda-Contra Costa Transit District");
            put(AGENCY_BART, "Bay Area Rapid Transit");
            put(AGENCY_CALTRAIN, "Caltrain");
            put(AGENCY_GGT, "Golden Gate Transit");
            put(AGENCY_VTA, "Santa Clara Valley Transportation Authority");
            put(AGENCY_MUNI, "San Francisco Municipal");
            put(AGENCY_FERRY, "Golden Gate Ferry");
        }
    };
    private static Map<Integer, String> sShortAgencies = new HashMap<Integer, String>() {
        {
            put(AGENCY_ACTRAN, "ACTransit");
            put(AGENCY_BART, "BART");
            put(AGENCY_CALTRAIN, "Caltrain");
            put(AGENCY_GGT, "GGT");
            put(AGENCY_VTA, "VTA");
            put(AGENCY_MUNI, "Muni");
            put(AGENCY_FERRY, "Ferry");
        }
    };
    private static Map<Long, Station> sBartStations = new HashMap<Long, Station>() {
        {
            put((long)0x5, new Station("24th St. Mission Station", "24th St.", "37.75226", "-122.41849"));
            put((long)0x06, new Station("16th St. Mission Station", "16th St.", "37.765228", "-122.419478"));
            put((long)0x07, new Station("Civic Center Station", "Civic Center", "37.779538", "-122.413788"));
            put((long)0x08, new Station("Powell Street Station", "Powell St.", "37.784970", "-122.40701"));
            put((long)0x09, new Station("Montgomery St. Station", "Montgomery", "37.789336", "-122.401486"));
            put((long)0x0a, new Station("Embarcadero Station", "Embarcadero", "37.793086", "-122.396276"));
            put((long)0xd, new Station("19th Street Oakland Station", "19th St.", "37.80762", "-122.26886"));
            put((long)0x18, new Station("Downtown Berkeley Station", "Berkeley", "37.869868", "-122.268051"));
            put((long)0x19, new Station("North Berkeley Station", "North Berkeley", "37.874026", "-122.283882"));
            put((long)0x1a, new Station("El Cerrito Plaza Station", "El Cerrito Plaza", "37.903959", "-122.299271"));
            put((long)0x1b, new Station("El Cerrito Del Norte Station", "El Cerrito Del Norte", "37.925651", "-122.317219"));
            put((long)0x1c, new Station("Richmond Station", "Richmond", "37.93730", "-122.35338"));
            put((long)0x22, new Station("Hayward Station", "Hayward", "37.670387", "-122.088002"));
            put((long)0x23, new Station("South Hayward Station", "South Hayward", "37.634800", "-122.057551"));
            put((long)0x24, new Station("Union City Station", "Union City", "37.591203", "-122.017854"));
            put((long)0x29, new Station("San Bruno Station", "San Bruno", "37.63714", "-122.415622"));
            put((long)0x2a, new Station("San Francisco Int'l Airport Station", "SFO", "37.61590", "-122.39263"));
            put((long)0x2b, new Station("Milbrae Station", "Milbrae", "37.599935", "-122.386478"));
        }
    };

    private static Map<Long, String> sFerryRoutes = new HashMap<Long, String>() {
        {
            put((long)0x03, "Larkspur");
            put((long)0x04, "San Francisco");
        }
    };

    private static Map<Long, Station> sFerryTerminals = new HashMap<Long, Station>() {
        {
            put((long)0x01, new Station("San Francisco Ferry Building", "San Francisco", "37.795873", "-122.391987"));
            put((long)0x03, new Station("Larkspur Ferry Terminal", "Larkspur", "37.945509", "-122.50916"));
        }
    };


    public static boolean check (MifareCard card)
    {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x9011f2) != null);
    }

    public ClipperTransitData (MifareCard card)
    {
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
            mBalance = Utils.byteArrayToInt(data, 18, 2);
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
        return "Clipper Card";
    }

    @Override
    public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format(mBalance / 100);
    }

    @Override
    public long getSerialNumber () {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips () {
        return mTrips;
    }

    public ClipperRefill[] getRefills () {
        return mRefills;
    }

    private Trip[] parseTrips (DesfireCard card)
    {
        DesfireFile file = card.getApplication(0x9011f2).getFile(0x0e);

        /*
         *  This file reads very much like a record file but it professes to
         *  be only a regular file.  As such, we'll need to extract the records
         *  manually.
         */
        byte [] data = file.getData();
        int pos = data.length - RECORD_LENGTH;
        List<Trip> result = new ArrayList<Trip>();
        while (pos > 0) {
            byte[] slice = Utils.byteArraySlice(data, pos, RECORD_LENGTH);
            Trip trip = createTrip(slice);
            if (trip != null) {
                int idx = Collections.binarySearch(result, trip,
                    new Comparator<Trip>() {
                        public int compare(Trip trip, Trip trip1) {
                            return Long.valueOf(trip1.getTimestamp()).compareTo(Long.valueOf(trip.getTimestamp()));
                        }
                    });
                if (idx >= 0) {
                    /*
                     *  Some transaction types are temporary -- remove previous
                     *  instance if there is an an entry with the same start
                     *  timestamp.
                     */
                    result.remove(idx);
                } else {
                    /* Convert idx back into the insertion point */
                    idx = -(idx + 1);
                }
                result.add(idx, trip);
            }
            pos -= RECORD_LENGTH;
        }
        Trip[] useLog = new Trip[result.size()];
        result.toArray(useLog);
        return useLog;
    }

    private Trip createTrip (byte[] useData)
    {
        long timestamp, fare, agency, from, to, route;

        timestamp = Utils.byteArrayToLong(useData, 0xc, 4);
        fare      = Utils.byteArrayToLong(useData, 0x6, 2);
        agency    = Utils.byteArrayToLong(useData, 0x2, 2);
        from      = Utils.byteArrayToLong(useData, 0x14, 2);
        to        = Utils.byteArrayToLong(useData, 0x16, 2);
        route     = Utils.byteArrayToLong(useData, 0x1c, 2);

        if (timestamp == 0)
            return null;

        // Use a magic number to offset the timestamp
        timestamp -= EPOCH_OFFSET;

        return new ClipperTrip(timestamp, fare, agency, from, to, route);
    }

    private ClipperRefill[] parseRefills (DesfireCard card)
    {
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

    private ClipperRefill createRefill (byte[] useData)
    {
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

    private void setBalances()
    {
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

    public static String getAgencyName(int agency)
    {
        if (sAgencies.containsKey(agency)) {
            return sAgencies.get(agency);
        }
        return "Unknown Agency (0x" + Long.toString(agency, 16) + ")";
    }

    public static String getShortAgencyName (int agency) {
        if (sShortAgencies.containsKey(agency)) {
            return sShortAgencies.get(agency);
        }
        return "UNK(0x" + Long.toString(agency, 16) + ")";
    }

    public static class ClipperTrip extends Trip
    {
        private final long mTimestamp;
        private final long mFare;
        private final long mAgency;
        private final long mFrom;
        private final long mTo;
        private final long mRoute;
        private long mBalance;

        public ClipperTrip (long timestamp, long fare, long agency, long from, long to, long route)
        {
            mTimestamp  = timestamp;
            mFare       = fare;
            mAgency     = agency;
            mFrom       = from;
            mTo         = to;
            mRoute      = route;
            mBalance    = 0;
        }

        @Override
        public long getTimestamp () {
            return mTimestamp;
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
                    return station.getShortName();
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
                    return sBartStations.get(mTo).getShortName();
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
    }

    public static class ClipperRefill extends Refill
    {
        private final long mTimestamp;
        private final long mAmount;
        private final long mMachineID;
        private final long mAgency;

        public ClipperRefill (long timestamp, long amount, long agency, long machineid)
        {
            mTimestamp  = timestamp;
            mAmount     = amount;
            mMachineID  = machineid;
            mAgency     = agency;
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
            return ClipperTransitData.getShortAgencyName((int)mAgency);
        }
    }
}
