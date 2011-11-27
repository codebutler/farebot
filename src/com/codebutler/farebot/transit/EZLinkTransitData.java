/*
 * EZLinkTransitData.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Sean Cross <sean@chumby.com>
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
import com.codebutler.farebot.cepas.CEPASCard;
import com.codebutler.farebot.cepas.CEPASTransaction;
import com.codebutler.farebot.mifare.Card;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeMap;

public class EZLinkTransitData extends TransitData
{
    private String mSerialNumber;
    private double mBalance;
    private Trip[] mTrips;

    private static HashSet<String> sbsBuses = new HashSet<String> () {
            /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

			{
                add("CT18");
                add("CT8");
                add("CT18");
                add("CT8");
                add("1N");
                add("2");
                add("2N");
                add("3");
                add("3N");
                add("4N");
                add("5");
                add("5N");
                add("6");
                add("6N");
                add("7");
                add("8");
                add("9");
                add("10");
                add("10e");
                add("11");
                add("12");
                add("13");
                add("14");
                add("14e");
                add("15");
                add("16");
                add("17");
                add("18");
                add("19");
                add("21");
                add("22");
                add("23");
                add("24");
                add("25");
                add("26");
                add("27");
                add("28");
                add("29");
                add("30");
                add("30e");
                add("31");
                add("32");
                add("33");
                add("34");
                add("35");
                add("36");
                add("37");
                add("38");
                add("39");
                add("40");
                add("42");
                add("43");
                add("45");
                add("48");
                add("51");
                add("52");
                add("53");
                add("54");
                add("55");
                add("56");
                add("57");
                add("58");
                add("59");
                add("60");
                add("62");
                add("63");
                add("64");
                add("65");
                add("66");
                add("69");
                add("70");
                add("70M");
                add("72");
                add("73");
                add("74");
                add("74e");
                add("76");
                add("78");
                add("79");
                add("80");
                add("81");
                add("82");
                add("83");
                add("85");
                add("86");
                add("87");
                add("88");
                add("89");
                add("89e");
                add("90");
                add("91");
                add("92");
                add("93");
                add("94");
                add("95");
                add("96");
                add("97");
                add("97e");
                add("98");
                add("98M");
                add("99");
                add("100");
                add("101");
                add("103");
                add("105");
                add("107");
                add("107M");
                add("109");
                add("111");
                add("112");
                add("113");
                add("115");
                add("119");
                add("123");
                add("123M");
                add("124");
                add("125");
                add("128");
                add("130");
                add("131");
                add("132");
                add("133");
                add("133M");
                add("135");
                add("136");
                add("138");
                add("139");
                add("142");
                add("143");
                add("145");
                add("147");
                add("151");
                add("151e");
                add("153");
                add("154");
                add("155");
                add("156");
                add("157");
                add("158");
                add("159");
                add("160");
                add("161");
                add("162");
                add("162M");
                add("163");
                add("163M");
                add("165");
                add("166");
                add("168");
                add("170");
                add("170X");
                add("174");
                add("174e");
                add("175");
                add("179");
                add("179A");
                add("181");
                add("182");
                add("182M");
                add("183");
                add("185");
                add("186");
                add("191");
                add("192");
                add("193");
                add("194");
                add("195");
                add("196");
                add("196e");
                add("197");
                add("198");
                add("199");
                add("200");
                add("222");
                add("225");
                add("228");
                add("229");
                add("231");
                add("232");
                add("235");
                add("238");
                add("240");
                add("241");
                add("242");
                add("243");
                add("246");
                add("249");
                add("251");
                add("252");
                add("254");
                add("255");
                add("257");
                add("261");
                add("262");
                add("265");
                add("268");
                add("269");
                add("272");
                add("273");
                add("275");
                add("282");
                add("284");
                add("284M");
                add("285");
                add("291");
                add("292");
                add("293");
                add("315");
                add("317");
                add("325");
                add("333");
                add("334");
                add("335");
                add("354");
                add("358");
                add("359");
                add("372");
                add("400");
                add("401");
                add("402");
                add("403");
                add("405");
                add("408");
                add("409");
                add("410");
                add("502");
                add("502A");
                add("506");
                add("518");
                add("518A");
                add("532");
                add("533");
                add("534");
                add("535");
                add("536");
                add("538");
                add("539");
                add("542");
                add("543");
                add("544");
                add("545");
                add("548");
                add("549");
                add("550");
                add("552");
                add("553");
                add("554");
                add("555");
                add("556");
                add("557");
                add("558");
                add("559");
                add("560");
                add("561");
                add("563");
                add("564");
                add("565");
                add("566");
                add("569");
                add("585");
                add("761");
            }
        };

    // Data snagged from http://www.sgwiki.com/wiki/North_East_Line
    private static TreeMap<String, MRTStation> mrtStations = new TreeMap<String, MRTStation> () {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
            put("HBF", new MRTStation("HarbourFront", "NE1", "HBF", "1.17", "103.5"));
            put("OTP", new MRTStation("Outram Park", "NE3 / EW16", "OTP", "1.17", "103.5"));
            put("CNT", new MRTStation("Chinatown", "NE4 / DT19", "CNT", "1.17", "103.5"));
            put("CQY", new MRTStation("Clarke Quay", "NE5", "CQY", "1.17", "103.5"));
            put("DBG", new MRTStation("Dhoby Ghaut", "NE6 / NS24 / CC1", "DBG", "1.17", "103.5"));
            put("LTI", new MRTStation("Little India", "NE7 / DT12", "LTI", "1.17", "103.5"));
            put("FRP", new MRTStation("Farrer Park", "NE8", "FRP", "1.17", "103.5"));
            put("BNK", new MRTStation("Boon Keng", "NE9", "BNK", "1.17", "103.5"));
            put("PTP", new MRTStation("Potong Pasir", "NE10", "PTP", "1.17", "103.5"));
            put("WLH", new MRTStation("Woodleigh", "NE11", "WLH", "1.17", "103.5"));
            put("SER", new MRTStation("Serangoon", "NE12 / CC13", "SER", "1.17", "103.5"));
            put("KVN", new MRTStation("Kovan", "NE13", "KVN", "1.17", "103.5"));
            put("HGN", new MRTStation("Hougang", "NE14", "HGN", "1.17", "103.5"));
            put("BGK", new MRTStation("Buangkok", "NE15", "BGK", "1.17", "103.5"));
            put("SKG", new MRTStation("Sengkang", "NE16 / STC", "SKG", "1.17", "103.5"));
            put("PGL", new MRTStation("Punggol", "NE17 / PTC", "PGL", "1.17", "103.5"));

            put("DBG", new MRTStation("Dhoby Ghaut", "CC1 / NS24 / NE6", "DBG", "1.17", "103.5"));
            put("DBN", new MRTStation("Dhoby Ghaut", "CC1 / NS24 / NE6", "DBN", "1.17", "103.5")); // Alternate name (Northeast line entrance)
            put("BBS", new MRTStation("Bras Basah", "CC2", "BBS", "1.17", "103.5"));
            put("EPN", new MRTStation("Esplanade", "CC3", "EPN", "1.17", "103.5"));
            put("PMD", new MRTStation("Promenade", "CC4 / DT15", "PMD", "1.17", "103.5"));
            put("NCH", new MRTStation("Nicoll Highway", "CC5", "NCH", "1.17", "103.5"));
            put("SDM", new MRTStation("Stadium", "CC6", "SDM", "1.17", "103.5"));
            put("MBT", new MRTStation("Mountbatten", "CC7", "MBT", "1.17", "103.5"));
            put("DKT", new MRTStation("Dakota", "CC8", "DKT", "1.17", "103.5"));
            put("PYL", new MRTStation("Paya Lebar", "CC9 / EW8", "PYL", "1.17", "103.5"));
            put("MPS", new MRTStation("MacPherson", "CC10 / DT?", "MPS", "1.17", "103.5"));
            put("TAS", new MRTStation("Tai Seng", "CC11", "TAS", "1.17", "103.5"));
            put("BLY", new MRTStation("Bartley", "CC12", "BLY", "1.17", "103.5"));
            put("SER", new MRTStation("Serangoon", "CC13 / NE12", "SER", "1.17", "103.5"));
            put("LRC", new MRTStation("Lorong Chuan", "CC14", "LRC", "1.17", "103.5"));
            put("BSH", new MRTStation("Bishan", "CC15 / NS17", "BSH", "1.17", "103.5"));
            put("MRM", new MRTStation("Marymount", "CC16", "MRM", "1.17", "103.5"));

            put("TNM", new MRTStation("Tanah Merah", "EW4", "TNM", "1.17", "103.5"));
            put("XPO", new MRTStation("Expo", "CG1 / DT35", "XPO", "1.17", "103.5"));
            put("CGA", new MRTStation("Changi Airport", "CG2", "CGA", "1.17", "103.5"));

            put("PSR", new MRTStation("Pasir Ris", "EW1", "PSR", "1.17", "103.5"));
            put("TAM", new MRTStation("Tampines", "EW2 / DT32", "TAM", "1.17", "103.5"));
            put("SIM", new MRTStation("Simei", "EW3", "SIM", "1.17", "103.5"));
            put("TNM", new MRTStation("Tanah Merah", "EW4", "TNM", "1.17", "103.5"));
            put("BDK", new MRTStation("Bedok", "EW5", "BDK", "1.17", "103.5"));
            put("KEM", new MRTStation("Kembangan", "EW6", "KEM", "1.17", "103.5"));
            put("EUN", new MRTStation("Eunos", "EW7", "EUN", "1.17", "103.5"));
            put("PYL", new MRTStation("Paya Lebar", "EW8 / CC9", "PYL", "1.17", "103.5"));
            put("ALJ", new MRTStation("Aljunied", "EW9", "ALJ", "1.17", "103.5"));
            put("KAL", new MRTStation("Kallang", "EW10", "KAL", "1.17", "103.5"));
            put("LVR", new MRTStation("Lavender", "EW11", "LVR", "1.17", "103.5"));
            put("BGS", new MRTStation("Bugis", "EW12 / DT14", "BGS", "1.17", "103.5"));
            put("CTH", new MRTStation("City Hall", "EW13 / NS25", "CTH", "1.17", "103.5"));
            put("RFP", new MRTStation("Raffles Place", "EW14 / NS26", "RFP", "1.17", "103.5"));
            put("TPG", new MRTStation("Tanjong Pagar", "EW15", "TPG", "1.17", "103.5"));
            put("OTP", new MRTStation("Outram Park", "EW16 / NE3", "OTP", "1.17", "103.5"));
            put("OTN", new MRTStation("Outram Park", "EW16 / NE3", "OTN", "1.17", "103.5")); // Alternate name (Northeast line entrance)
            put("TIB", new MRTStation("Tiong Bahru", "EW17", "TIB", "1.17", "103.5"));
            put("RDH", new MRTStation("Redhill", "EW18", "RDH", "1.17", "103.5"));
            put("QUE", new MRTStation("Queenstown", "EW19", "QUE", "1.17", "103.5"));
            put("COM", new MRTStation("Commonwealth", "EW20", "COM", "1.17", "103.5"));
            put("BNV", new MRTStation("Buona Vista", "EW21 / CC22", "BNV", "1.17", "103.5"));
            put("DVR", new MRTStation("Dover", "EW22", "DVR", "1.17", "103.5"));
            put("CLE", new MRTStation("Clementi", "EW23", "CLE", "1.17", "103.5"));
            put("JUR", new MRTStation("Jurong East", "EW24 / NS1", "JUR", "1.17", "103.5"));
            put("CNG", new MRTStation("Chinese Garden", "EW25", "CNG", "1.17", "103.5"));
            put("LKS", new MRTStation("Lakeside", "EW26", "LKS", "1.17", "103.5"));
            put("BNL", new MRTStation("Boon Lay", "EW27", "BNL", "1.17", "103.5"));
            put("PNR", new MRTStation("Pioneer", "EW28", "PNR", "1.17", "103.5"));
            put("JKN", new MRTStation("Joo Koon", "EW29", "JKN", "1.17", "103.5"));

            put("JUR", new MRTStation("Jurong East", "NS1 / EW24", "JUR", "1.17", "103.5"));
            put("BBT", new MRTStation("Bukit Batok", "NS2", "BBT", "1.17", "103.5"));
            put("BGB", new MRTStation("Bukit Gombak", "NS3", "BGB", "1.17", "103.5"));
            put("CCK", new MRTStation("Choa Chu Kang", "NS4 / BP1", "CCK", "1.17", "103.5"));
            put("YWT", new MRTStation("Yew Tee", "NS5", "YWT", "1.17", "103.5"));
            put("KRJ", new MRTStation("Kranji", "NS7", "KRJ", "1.17", "103.5"));
            put("MSL", new MRTStation("Marsiling", "NS8", "MSL", "1.17", "103.5"));
            put("WDL", new MRTStation("Woodlands", "NS9", "WDL", "1.17", "103.5"));
            put("ADM", new MRTStation("Admiralty", "NS10", "ADM", "1.17", "103.5"));
            put("SBW", new MRTStation("Sembawang", "NS11", "SBW", "1.17", "103.5"));
            put("YIS", new MRTStation("Yishun", "NS13", "YIS", "1.17", "103.5"));
            put("KTB", new MRTStation("Khatib", "NS14", "KTB", "1.17", "103.5"));
            put("YCK", new MRTStation("Yio Chu Kang", "NS15", "YCK", "1.17", "103.5"));
            put("AMK", new MRTStation("Ang Mo Kio", "NS16", "AMK", "1.17", "103.5"));
            put("BSH", new MRTStation("Bishan", "NS17 / CC15", "BSH", "1.17", "103.5"));
            put("BDL", new MRTStation("Braddell", "NS18", "BDL", "1.17", "103.5"));
            put("TAP", new MRTStation("Toa Payoh", "NS19", "TAP", "1.17", "103.5"));
            put("NOV", new MRTStation("Novena", "NS20", "NOV", "1.17", "103.5"));
            put("NEW", new MRTStation("Newton", "NS21 / DT11", "NEW", "1.17", "103.5"));
            put("ORC", new MRTStation("Orchard", "NS22", "ORC", "1.17", "103.5"));
            put("SOM", new MRTStation("Somerset", "NS23", "SOM", "1.17", "103.5"));
            put("DBG", new MRTStation("Dhoby Ghaut", "NS24 / NE6 / CC1", "DBG", "1.17", "103.5"));
            put("CTH", new MRTStation("City Hall", "NS25 / EW13", "CTH", "1.17", "103.5"));
            put("RFP", new MRTStation("Raffles Place", "NS26 / EW14", "RFP", "1.17", "103.5"));
            put("MRB", new MRTStation("Marina Bay", "NS27 / CE2", "MRB", "1.17", "103.5"));
        }
    };

    public static MRTStation getStation (String code) {
    	return mrtStations.get(code);
    }

    public static boolean check (Card card)
    {
        return (card instanceof CEPASCard);
    }

    public static TransitIdentity parseTransitIdentity(Card card)
    {
        return new TransitIdentity("EZ-Link", Utils.getHexString(((CEPASCard) card).getPurse(3).getCAN(), "<Error>"));
    }

    public EZLinkTransitData (Card card)
    {
        CEPASCard cepasCard = (CEPASCard) card;

        mSerialNumber = Utils.getHexString(cepasCard.getPurse(3).getCAN(), "<Error>");
        mBalance      = cepasCard.getPurse(3).getPurseBalance();
        mTrips        = parseTrips(cepasCard);
    }

    @Override
    public String getCardName () {
        return "EZ-Link";
    }

    @Override
    public String getBalanceString () {
		return NumberFormat.getCurrencyInstance(new Locale("en", "SG")).format(mBalance / 100);
    }

    @Override
    public String getSerialNumber () {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips () {
        return mTrips;
    }

    @Override
    public Refill[] getRefills () {
        return null;
    }

    private Trip[] parseTrips (CEPASCard card)
    {
    	CEPASTransaction[] transactions = card.getHistory(3).getTransactions();
    	Trip[] trips = new Trip[transactions.length];
    	
    	for (int i = 0; i < trips.length; i++)
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
            if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
                String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
                if (sbsBuses.contains(routeString))
                	return "SBS";
                return "SMRT";
            }
            if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION) {
            	return "EZLink";
            }
            return "SMRT";
        }

        @Override
        public String getShortAgencyName () {
            if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
                String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
                if (sbsBuses.contains(routeString))
                	return "SBS";
                return "SMRT";
            }
            if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION) {
            	return "EZ";
            }
            return "SMRT";
        }
        
        @Override
        public String getRouteName () {
        	if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
        		if (mTransaction.getUserData().startsWith("SVC"))
        			return "Bus #" + mTransaction.getUserData().substring(3, 7).replace(" ", "");
        		return "(Unknown Bus Route)";
        	}
        	else if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND)
        		return "Bus Refund";
        	else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT
        			|| mTransaction.getType() == CEPASTransaction.TransactionType.OLD_MRT)
        		return "MRT";
        	else if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
        		return "First use";
        	else
        		return "(Unknown Route)";
        }

        @Override
        public String getFareString () {
        	int balance = -mTransaction.getAmount();
        	if (balance < 0)
        		return "Refund " + NumberFormat.getCurrencyInstance(new Locale("en", "SG")).format(-balance / 100.0);
        	else
        		return NumberFormat.getCurrencyInstance(new Locale("en", "SG")).format(balance / 100.0);
        }

        @Override
        public double getFare () {
        	if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
        		return 0.0;
            return mTransaction.getAmount() / 100.0;
        }

        @Override
        public String getBalanceString () {
        	return "(???)";
        }

        @Override
        public Station getStartStation () {
        	return null;
        }

        @Override
        public Station getEndStation () {
            return null;
        }

        @Override
        public String getStartStationName () {
        	if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
        		return "";
            if (mTransaction.getUserData().charAt(3) == '-'
             || mTransaction.getUserData().charAt(3) == ' ') {
                String startStationAbbr = mTransaction.getUserData().substring(0, 3);
                MRTStation startStation = EZLinkTransitData.getStation(startStationAbbr);
                if (startStation != null)
                    return startStation.getStationName();
                else
                    return startStationAbbr;
            }
            return mTransaction.getUserData();
        }

        @Override
        public String getEndStationName () {
        	if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
        		return null;
            if (mTransaction.getUserData().charAt(3) == '-'
             || mTransaction.getUserData().charAt(3) == ' ') {
                String endStationAbbr   = mTransaction.getUserData().substring(4,7);
                MRTStation endStation   = EZLinkTransitData.getStation(endStationAbbr);
                if (endStation != null)
                    return endStation.getStationName();
                else
                    return endStationAbbr;
            }
            return null;
        }
    }
}
