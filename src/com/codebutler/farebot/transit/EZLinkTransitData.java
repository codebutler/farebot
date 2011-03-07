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
import com.codebutler.farebot.mifare.MifareCard;
import com.codebutler.farebot.cepas.CEPASCard;
import com.codebutler.farebot.cepas.CEPASTransaction;

import java.text.NumberFormat;
import java.util.*;

public class EZLinkTransitData extends TransitData
{
    private int      mSerialNumber;
    private double   mBalance;
    private Trip[]   mTrips;

    public static boolean check (MifareCard card)
    {
        return (card instanceof CEPASCard);
    }

    public EZLinkTransitData (MifareCard card)
    {
        CEPASCard cepasCard = (CEPASCard) card;

        mSerialNumber = Utils.byteArrayToInt(cepasCard.getPurse(3).getCSN(), 0, 8);
        mBalance = cepasCard.getPurse(3).getPurseBalance();
        mTrips = parseTrips(cepasCard);
    }

    @Override
    public String getCardName () {
        return "EZ-Link";
    }

    @Override
    public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format(mBalance / 100);
    }

    @Override
    public int getSerialNumber () {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips () {
        return mTrips;
    }

    private Trip[] parseTrips (CEPASCard card)
    {
    	CEPASTransaction[] transactions = card.getHistory(3).getTransactions();
    	Trip[] trips = new Trip[transactions.length];
    	
    	for(int i=0; i<trips.length; i++)
    		trips[i] = createTrip(transactions[i]);
    	return trips;
    }

    private Trip createTrip (CEPASTransaction record)
    {
    	return new EZLinkTrip(record);
    }

    public static class EZLinkTrip extends Trip
    {
    	private final CEPASTransaction mTransaction;
    	
        private static Station[] sLinkStations = new Station[] {
            new Station("Westlake Station",                   "47.6113968", "-122.337502"),
            new Station("University Station",                 "47.6072502", "-122.335754"),
            new Station("Pioneer Square Station",             "47.6021461", "-122.33107"),
            new Station("International District Station",     "47.5976601", "-122.328217"),
            new Station("Stadium Station",                    "47.5918121", "-122.327354"),
            new Station("SODO Station",                       "47.5799484", "-122.327515"),
            new Station("Beacon Hill Station",                "47.5791245", "-122.311287"),
            new Station("Mount Baker Station",                "47.5764389", "-122.297737"),
            new Station("Columbia City Station",              "47.5589523", "-122.292343"),
            new Station("Othello Station",                    "47.5375366", "-122.281471"),
            new Station("Rainier Beach Station",              "47.5222626", "-122.279579"),
            new Station("Tukwila International Blvd Station", "47.4642754", "-122.288391"),
            new Station("Seatac Airport Station",             "47.4445305", "-122.297012")
        };

        public EZLinkTrip (CEPASTransaction transaction)
        {
        	mTransaction = transaction;
        }

        @Override
        public long getTimestamp() {
            return mTransaction.getTimestamp();
        }

        @Override
        public String getAgencyName () {
            return "Unknown Agency";
        }

        @Override
        public String getShortAgencyName () {
            return "??";
        }

        @Override
        public String getRouteName () {
        	return "EW-NS";
        }

        @Override
        public String getStationName () {
        	return mTransaction.getUserData();
        }

        @Override
        public String getFareString () {
            return NumberFormat.getCurrencyInstance(Locale.US).format(mTransaction.getAmount()/100.0);
        }

        @Override
        public double getFare () {
            return mTransaction.getAmount()/100.0;
        }

        @Override
        public String getBalanceString () {
//            return NumberFormat.getCurrencyInstance(Locale.US).format(mNewBalance / 100);
        	return "(Unknown)";
        }

        @Override
        public Station getStation() {
        	return new Station("Outram Park", "1.165016", "103.502300");
        }
    }
}
