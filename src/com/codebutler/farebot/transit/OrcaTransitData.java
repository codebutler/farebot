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

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.DesfireFile;
import com.codebutler.farebot.mifare.DesfireFile.RecordDesfireFile;
import com.codebutler.farebot.mifare.DesfireRecord;
import com.codebutler.farebot.mifare.Card;

import java.text.NumberFormat;
import java.util.*;

public class OrcaTransitData extends TransitData
{
    private int      mSerialNumber;
    private double   mBalance;
    private Trip[]   mTrips;

    public static boolean check (Card card)
    {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x3010f2) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card)
    {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0xffffff).getFile(0x0f).getData();
            return new TransitIdentity("ORCA Card", String.valueOf(Utils.byteArrayToInt(data, 5, 3)));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing ORCA serial", ex);
        }
    }

    public OrcaTransitData (Card card)
    {
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
        return "ORCA Card";
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

    private Trip[] parseTrips (DesfireCard card)
    {
        DesfireFile file = card.getApplication(0x3010f2).getFile(0x02);

        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) card.getApplication(0x3010f2).getFile(0x02);

            List<Trip> result = new ArrayList<Trip>();

            Trip[] useLog = new Trip[recordFile.getRecords().length];
            for (int i = 0; i < useLog.length; i++) {
                useLog[i] = new OrcaTrip(recordFile.getRecords()[i]);
            }

            Arrays.sort(useLog, new Comparator<Trip>() {
                public int compare(Trip trip, Trip trip1) {
                    return Long.valueOf(trip1.getTimestamp()).compareTo(Long.valueOf(trip.getTimestamp()));
                }
            });

            return useLog;
        }
        return null;
    }

    public static class OrcaTrip extends Trip
    {
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
        
        public OrcaTrip (DesfireRecord record)
        {
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
    
            mCoachNum   = ((usefulData[9] & 0xf) << 12) | (usefulData[10] << 4) | ((usefulData[11] & 0xf0) >> 4);
            mFare       = (usefulData[15] << 7) | (usefulData[16] >> 1);
            mNewBalance = (usefulData[34] << 8) | usefulData[35];
            mAgency     = usefulData[3] >> 4;
            mTransType  = (usefulData[17]);            
        }

        @Override
        public long getTimestamp() {
            return mTimestamp;
        }

        @Override
        public String getAgencyName () {
            switch ((int) mAgency) {
                case 0x02:
                    return "Community Transit";
                case 0x04:
                    return "King County Metro Transit";
                case 0x06:
                    return "Pierce Transit";
                case 0x07:
                    return "Sound Transit";
            }
            return "Unknown Agency";
        }

        @Override
        public String getShortAgencyName () {
            switch ((int) mAgency) {
                case 0x02:
                    return "CT";
                case 0x04:
                    return "KCM";
                case 0x06:
                    return "PT";
                case 0x07:
                    return "ST";
            }
            return "Unknown";
        }

        @Override
        public String getRouteName () {
            if (mAgency == 0x07 && mCoachNum > 10000) {
                return "Link Light Rail";
            } else {
                // FIXME: Need to find bus route #s
                //return "(Unknown Route)";
                return null;
            }
        }

        @Override
        public String getFareString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format(mFare / 100);
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
            if (mAgency == 0x07 && mCoachNum > 1000) {
                return sLinkStations[(((int) mCoachNum) % 1000) - 193];
            }
            return null;
        }

        @Override
        public String getStartStationName () {
            if (mAgency == 0x07 && mCoachNum > 10000) {
                return sLinkStations[(((int) mCoachNum) % 1000) - 193].getStationName();
            } else {
                return "Coach #" + String.valueOf(mCoachNum);
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

        public long getCoachNumber() {
            return mCoachNum;
        }
    }
}
