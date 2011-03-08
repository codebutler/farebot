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

import android.util.Log;

import com.codebutler.farebot.Utils;
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
    private static TreeMap<String, String> sbsBuses;
    private static TreeMap<String, MRTStation> mrtStations;
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
    
    public static boolean isSbsBus(String routeName) {
    	if(sbsBuses == null) {
    		sbsBuses = new TreeMap<String, String>();
			sbsBuses.put("CT18", "yes");
			sbsBuses.put("CT8", "yes");
			sbsBuses.put("1N", "yes");
			sbsBuses.put("2", "yes");
			sbsBuses.put("2N", "yes");
			sbsBuses.put("3", "yes");
			sbsBuses.put("3N", "yes");
			sbsBuses.put("4N", "yes");
			sbsBuses.put("5", "yes");
			sbsBuses.put("5N", "yes");
			sbsBuses.put("6", "yes");
			sbsBuses.put("6N", "yes");
			sbsBuses.put("7", "yes");
			sbsBuses.put("8", "yes");
			sbsBuses.put("9", "yes");
			sbsBuses.put("10", "yes");
			sbsBuses.put("10e", "yes");
			sbsBuses.put("11", "yes");
			sbsBuses.put("12", "yes");
			sbsBuses.put("13", "yes");
			sbsBuses.put("14", "yes");
			sbsBuses.put("14e", "yes");
			sbsBuses.put("15", "yes");
			sbsBuses.put("16", "yes");
			sbsBuses.put("17", "yes");
			sbsBuses.put("18", "yes");
			sbsBuses.put("19", "yes");
			sbsBuses.put("21", "yes");
			sbsBuses.put("22", "yes");
			sbsBuses.put("23", "yes");
			sbsBuses.put("24", "yes");
			sbsBuses.put("25", "yes");
			sbsBuses.put("26", "yes");
			sbsBuses.put("27", "yes");
			sbsBuses.put("28", "yes");
			sbsBuses.put("29", "yes");
			sbsBuses.put("30", "yes");
			sbsBuses.put("30e", "yes");
			sbsBuses.put("31", "yes");
			sbsBuses.put("32", "yes");
			sbsBuses.put("33", "yes");
			sbsBuses.put("34", "yes");
			sbsBuses.put("35", "yes");
			sbsBuses.put("36", "yes");
			sbsBuses.put("37", "yes");
			sbsBuses.put("38", "yes");
			sbsBuses.put("39", "yes");
			sbsBuses.put("40", "yes");
			sbsBuses.put("42", "yes");
			sbsBuses.put("43", "yes");
			sbsBuses.put("45", "yes");
			sbsBuses.put("48", "yes");
			sbsBuses.put("51", "yes");
			sbsBuses.put("52", "yes");
			sbsBuses.put("53", "yes");
			sbsBuses.put("54", "yes");
			sbsBuses.put("55", "yes");
			sbsBuses.put("56", "yes");
			sbsBuses.put("57", "yes");
			sbsBuses.put("58", "yes");
			sbsBuses.put("59", "yes");
			sbsBuses.put("60", "yes");
			sbsBuses.put("62", "yes");
			sbsBuses.put("63", "yes");
			sbsBuses.put("64", "yes");
			sbsBuses.put("65", "yes");
			sbsBuses.put("66", "yes");
			sbsBuses.put("69", "yes");
			sbsBuses.put("70", "yes");
			sbsBuses.put("70M", "yes");
			sbsBuses.put("72", "yes");
			sbsBuses.put("73", "yes");
			sbsBuses.put("74", "yes");
			sbsBuses.put("74e", "yes");
			sbsBuses.put("76", "yes");
			sbsBuses.put("78", "yes");
			sbsBuses.put("79", "yes");
			sbsBuses.put("80", "yes");
			sbsBuses.put("81", "yes");
			sbsBuses.put("82", "yes");
			sbsBuses.put("83", "yes");
			sbsBuses.put("85", "yes");
			sbsBuses.put("86", "yes");
			sbsBuses.put("87", "yes");
			sbsBuses.put("88", "yes");
			sbsBuses.put("89", "yes");
			sbsBuses.put("89e", "yes");
			sbsBuses.put("90", "yes");
			sbsBuses.put("91", "yes");
			sbsBuses.put("92", "yes");
			sbsBuses.put("93", "yes");
			sbsBuses.put("94", "yes");
			sbsBuses.put("95", "yes");
			sbsBuses.put("96", "yes");
			sbsBuses.put("97", "yes");
			sbsBuses.put("97e", "yes");
			sbsBuses.put("98", "yes");
			sbsBuses.put("98M", "yes");
			sbsBuses.put("99", "yes");
			sbsBuses.put("100", "yes");
			sbsBuses.put("101", "yes");
			sbsBuses.put("103", "yes");
			sbsBuses.put("105", "yes");
			sbsBuses.put("107", "yes");
			sbsBuses.put("107M", "yes");
			sbsBuses.put("109", "yes");
			sbsBuses.put("111", "yes");
			sbsBuses.put("112", "yes");
			sbsBuses.put("113", "yes");
			sbsBuses.put("115", "yes");
			sbsBuses.put("119", "yes");
			sbsBuses.put("123", "yes");
			sbsBuses.put("123M", "yes");
			sbsBuses.put("124", "yes");
			sbsBuses.put("125", "yes");
			sbsBuses.put("128", "yes");
			sbsBuses.put("130", "yes");
			sbsBuses.put("131", "yes");
			sbsBuses.put("132", "yes");
			sbsBuses.put("133", "yes");
			sbsBuses.put("133M", "yes");
			sbsBuses.put("135", "yes");
			sbsBuses.put("136", "yes");
			sbsBuses.put("138", "yes");
			sbsBuses.put("139", "yes");
			sbsBuses.put("142", "yes");
			sbsBuses.put("143", "yes");
			sbsBuses.put("145", "yes");
			sbsBuses.put("147", "yes");
			sbsBuses.put("151", "yes");
			sbsBuses.put("151e", "yes");
			sbsBuses.put("153", "yes");
			sbsBuses.put("154", "yes");
			sbsBuses.put("155", "yes");
			sbsBuses.put("156", "yes");
			sbsBuses.put("157", "yes");
			sbsBuses.put("158", "yes");
			sbsBuses.put("159", "yes");
			sbsBuses.put("160", "yes");
			sbsBuses.put("161", "yes");
			sbsBuses.put("162", "yes");
			sbsBuses.put("162M", "yes");
			sbsBuses.put("163", "yes");
			sbsBuses.put("163M", "yes");
			sbsBuses.put("165", "yes");
			sbsBuses.put("166", "yes");
			sbsBuses.put("168", "yes");
			sbsBuses.put("170", "yes");
			sbsBuses.put("170X", "yes");
			sbsBuses.put("174", "yes");
			sbsBuses.put("174e", "yes");
			sbsBuses.put("175", "yes");
			sbsBuses.put("179", "yes");
			sbsBuses.put("179A", "yes");
			sbsBuses.put("181", "yes");
			sbsBuses.put("182", "yes");
			sbsBuses.put("182M", "yes");
			sbsBuses.put("183", "yes");
			sbsBuses.put("185", "yes");
			sbsBuses.put("186", "yes");
			sbsBuses.put("191", "yes");
			sbsBuses.put("192", "yes");
			sbsBuses.put("193", "yes");
			sbsBuses.put("194", "yes");
			sbsBuses.put("195", "yes");
			sbsBuses.put("196", "yes");
			sbsBuses.put("196e", "yes");
			sbsBuses.put("197", "yes");
			sbsBuses.put("198", "yes");
			sbsBuses.put("199", "yes");
			sbsBuses.put("200", "yes");
			sbsBuses.put("222", "yes");
			sbsBuses.put("225", "yes");
			sbsBuses.put("228", "yes");
			sbsBuses.put("229", "yes");
			sbsBuses.put("231", "yes");
			sbsBuses.put("232", "yes");
			sbsBuses.put("235", "yes");
			sbsBuses.put("238", "yes");
			sbsBuses.put("240", "yes");
			sbsBuses.put("241", "yes");
			sbsBuses.put("242", "yes");
			sbsBuses.put("243", "yes");
			sbsBuses.put("246", "yes");
			sbsBuses.put("249", "yes");
			sbsBuses.put("251", "yes");
			sbsBuses.put("252", "yes");
			sbsBuses.put("254", "yes");
			sbsBuses.put("255", "yes");
			sbsBuses.put("257", "yes");
			sbsBuses.put("261", "yes");
			sbsBuses.put("262", "yes");
			sbsBuses.put("265", "yes");
			sbsBuses.put("268", "yes");
			sbsBuses.put("269", "yes");
			sbsBuses.put("272", "yes");
			sbsBuses.put("273", "yes");
			sbsBuses.put("275", "yes");
			sbsBuses.put("282", "yes");
			sbsBuses.put("284", "yes");
			sbsBuses.put("284M", "yes");
			sbsBuses.put("285", "yes");
			sbsBuses.put("291", "yes");
			sbsBuses.put("292", "yes");
			sbsBuses.put("293", "yes");
			sbsBuses.put("315", "yes");
			sbsBuses.put("317", "yes");
			sbsBuses.put("325", "yes");
			sbsBuses.put("333", "yes");
			sbsBuses.put("334", "yes");
			sbsBuses.put("335", "yes");
			sbsBuses.put("354", "yes");
			sbsBuses.put("358", "yes");
			sbsBuses.put("359", "yes");
			sbsBuses.put("372", "yes");
			sbsBuses.put("400", "yes");
			sbsBuses.put("401", "yes");
			sbsBuses.put("402", "yes");
			sbsBuses.put("403", "yes");
			sbsBuses.put("405", "yes");
			sbsBuses.put("408", "yes");
			sbsBuses.put("409", "yes");
			sbsBuses.put("410", "yes");
			sbsBuses.put("502", "yes");
			sbsBuses.put("502A", "yes");
			sbsBuses.put("506", "yes");
			sbsBuses.put("518", "yes");
			sbsBuses.put("518A", "yes");
			sbsBuses.put("532", "yes");
			sbsBuses.put("533", "yes");
			sbsBuses.put("534", "yes");
			sbsBuses.put("535", "yes");
			sbsBuses.put("536", "yes");
			sbsBuses.put("538", "yes");
			sbsBuses.put("539", "yes");
			sbsBuses.put("542", "yes");
			sbsBuses.put("543", "yes");
			sbsBuses.put("544", "yes");
			sbsBuses.put("545", "yes");
			sbsBuses.put("548", "yes");
			sbsBuses.put("549", "yes");
			sbsBuses.put("550", "yes");
			sbsBuses.put("552", "yes");
			sbsBuses.put("553", "yes");
			sbsBuses.put("554", "yes");
			sbsBuses.put("555", "yes");
			sbsBuses.put("556", "yes");
			sbsBuses.put("557", "yes");
			sbsBuses.put("558", "yes");
			sbsBuses.put("559", "yes");
			sbsBuses.put("560", "yes");
			sbsBuses.put("561", "yes");
			sbsBuses.put("563", "yes");
			sbsBuses.put("564", "yes");
			sbsBuses.put("565", "yes");
			sbsBuses.put("566", "yes");
			sbsBuses.put("569", "yes");
			sbsBuses.put("585", "yes");
			sbsBuses.put("761", "yes");
    	}
    	return sbsBuses.containsKey(routeName);
    }
    
    // Data snagged from http://www.sgwiki.com/wiki/North_East_Line
    public static MRTStation getStation(String code) {
    	if(null == mrtStations) {
    		mrtStations = new TreeMap<String, MRTStation>();
    		mrtStations.put("HBF", new MRTStation("HarbourFront", "NE1", "HBF", "1.17", "103.5"));
    		mrtStations.put("OTP", new MRTStation("Outram Park", "NE3 / EW16", "OTP", "1.17", "103.5"));
    		mrtStations.put("CNT", new MRTStation("Chinatown", "NE4 / DT19", "CNT", "1.17", "103.5"));
    		mrtStations.put("CQY", new MRTStation("Clarke Quay", "NE5", "CQY", "1.17", "103.5"));
    		mrtStations.put("DBG", new MRTStation("Dhoby Ghaut", "NE6 / NS24 / CC1", "DBG", "1.17", "103.5"));
    		mrtStations.put("LTI", new MRTStation("Little India", "NE7 / DT12", "LTI", "1.17", "103.5"));
    		mrtStations.put("FRP", new MRTStation("Farrer Park", "NE8", "FRP", "1.17", "103.5"));
    		mrtStations.put("BNK", new MRTStation("Boon Keng", "NE9", "BNK", "1.17", "103.5"));
    		mrtStations.put("PTP", new MRTStation("Potong Pasir", "NE10", "PTP", "1.17", "103.5"));
    		mrtStations.put("WLH", new MRTStation("Woodleigh", "NE11", "WLH", "1.17", "103.5"));
    		mrtStations.put("SER", new MRTStation("Serangoon", "NE12 / CC13", "SER", "1.17", "103.5"));
    		mrtStations.put("KVN", new MRTStation("Kovan", "NE13", "KVN", "1.17", "103.5"));
    		mrtStations.put("HGN", new MRTStation("Hougang", "NE14", "HGN", "1.17", "103.5"));
    		mrtStations.put("BGK", new MRTStation("Buangkok", "NE15", "BGK", "1.17", "103.5"));
    		mrtStations.put("SKG", new MRTStation("Sengkang", "NE16 / STC", "SKG", "1.17", "103.5"));
    		mrtStations.put("PGL", new MRTStation("Punggol", "NE17 / PTC", "PGL", "1.17", "103.5"));

    		mrtStations.put("DBG", new MRTStation("Dhoby Ghaut", "CC1 / NS24 / NE6", "DBG", "1.17", "103.5"));
    		mrtStations.put("BBS", new MRTStation("Bras Basah", "CC2", "BBS", "1.17", "103.5"));
    		mrtStations.put("EPN", new MRTStation("Esplanade", "CC3", "EPN", "1.17", "103.5"));
    		mrtStations.put("PMD", new MRTStation("Promenade", "CC4 / DT15", "PMD", "1.17", "103.5"));
    		mrtStations.put("NCH", new MRTStation("Nicoll Highway", "CC5", "NCH", "1.17", "103.5"));
    		mrtStations.put("SDM", new MRTStation("Stadium", "CC6", "SDM", "1.17", "103.5"));
    		mrtStations.put("MBT", new MRTStation("Mountbatten", "CC7", "MBT", "1.17", "103.5"));
    		mrtStations.put("DKT", new MRTStation("Dakota", "CC8", "DKT", "1.17", "103.5"));
    		mrtStations.put("PYL", new MRTStation("Paya Lebar", "CC9 / EW8", "PYL", "1.17", "103.5"));
    		mrtStations.put("MPS", new MRTStation("MacPherson", "CC10 / DT?", "MPS", "1.17", "103.5"));
    		mrtStations.put("TAS", new MRTStation("Tai Seng", "CC11", "TAS", "1.17", "103.5"));
    		mrtStations.put("BLY", new MRTStation("Bartley", "CC12", "BLY", "1.17", "103.5"));
    		mrtStations.put("SER", new MRTStation("Serangoon", "CC13 / NE12", "SER", "1.17", "103.5"));
    		mrtStations.put("LRC", new MRTStation("Lorong Chuan", "CC14", "LRC", "1.17", "103.5"));
    		mrtStations.put("BSH", new MRTStation("Bishan", "CC15 / NS17", "BSH", "1.17", "103.5"));
    		mrtStations.put("MRM", new MRTStation("Marymount", "CC16", "MRM", "1.17", "103.5"));

    		mrtStations.put("TNM", new MRTStation("Tanah Merah", "EW4", "TNM", "1.17", "103.5"));
    		mrtStations.put("XPO", new MRTStation("Expo", "CG1 / DT35", "XPO", "1.17", "103.5"));
    		mrtStations.put("CGA", new MRTStation("Changi Airport", "CG2", "CGA", "1.17", "103.5"));

    		mrtStations.put("PSR", new MRTStation("Pasir Ris", "EW1", "PSR", "1.17", "103.5"));
    		mrtStations.put("TAM", new MRTStation("Tampines", "EW2 / DT32", "TAM", "1.17", "103.5"));
    		mrtStations.put("SIM", new MRTStation("Simei", "EW3", "SIM", "1.17", "103.5"));
    		mrtStations.put("TNM", new MRTStation("Tanah Merah", "EW4", "TNM", "1.17", "103.5"));
    		mrtStations.put("BDK", new MRTStation("Bedok", "EW5", "BDK", "1.17", "103.5"));
    		mrtStations.put("KEM", new MRTStation("Kembangan", "EW6", "KEM", "1.17", "103.5"));
    		mrtStations.put("EUN", new MRTStation("Eunos", "EW7", "EUN", "1.17", "103.5"));
    		mrtStations.put("PYL", new MRTStation("Paya Lebar", "EW8 / CC9", "PYL", "1.17", "103.5"));
    		mrtStations.put("ALJ", new MRTStation("Aljunied", "EW9", "ALJ", "1.17", "103.5"));
    		mrtStations.put("KAL", new MRTStation("Kallang", "EW10", "KAL", "1.17", "103.5"));
    		mrtStations.put("LVR", new MRTStation("Lavender", "EW11", "LVR", "1.17", "103.5"));
    		mrtStations.put("BGS", new MRTStation("Bugis", "EW12 / DT14", "BGS", "1.17", "103.5"));
    		mrtStations.put("CTH", new MRTStation("City Hall", "EW13 / NS25", "CTH", "1.17", "103.5"));
    		mrtStations.put("RFP", new MRTStation("Raffles Place", "EW14 / NS26", "RFP", "1.17", "103.5"));
    		mrtStations.put("TPG", new MRTStation("Tanjong Pagar", "EW15", "TPG", "1.17", "103.5"));
    		mrtStations.put("OTP", new MRTStation("Outram Park", "EW16 / NE3", "OTP", "1.17", "103.5"));
    		mrtStations.put("TIB", new MRTStation("Tiong Bahru", "EW17", "TIB", "1.17", "103.5"));
    		mrtStations.put("RDH", new MRTStation("Redhill", "EW18", "RDH", "1.17", "103.5"));
    		mrtStations.put("QUE", new MRTStation("Queenstown", "EW19", "QUE", "1.17", "103.5"));
    		mrtStations.put("COM", new MRTStation("Commonwealth", "EW20", "COM", "1.17", "103.5"));
    		mrtStations.put("BNV", new MRTStation("Buona Vista", "EW21 / CC22", "BNV", "1.17", "103.5"));
    		mrtStations.put("DVR", new MRTStation("Dover", "EW22", "DVR", "1.17", "103.5"));
    		mrtStations.put("CLE", new MRTStation("Clementi", "EW23", "CLE", "1.17", "103.5"));
    		mrtStations.put("JUR", new MRTStation("Jurong East", "EW24 / NS1", "JUR", "1.17", "103.5"));
    		mrtStations.put("CNG", new MRTStation("Chinese Garden", "EW25", "CNG", "1.17", "103.5"));
    		mrtStations.put("LKS", new MRTStation("Lakeside", "EW26", "LKS", "1.17", "103.5"));
    		mrtStations.put("BNL", new MRTStation("Boon Lay", "EW27", "BNL", "1.17", "103.5"));
    		mrtStations.put("PNR", new MRTStation("Pioneer", "EW28", "PNR", "1.17", "103.5"));
    		mrtStations.put("JKN", new MRTStation("Joo Koon", "EW29", "JKN", "1.17", "103.5"));

    		mrtStations.put("JUR", new MRTStation("Jurong East", "NS1 / EW24", "JUR", "1.17", "103.5"));
    		mrtStations.put("BBT", new MRTStation("Bukit Batok", "NS2", "BBT", "1.17", "103.5"));
    		mrtStations.put("BGB", new MRTStation("Bukit Gombak", "NS3", "BGB", "1.17", "103.5"));
    		mrtStations.put("CCK", new MRTStation("Choa Chu Kang", "NS4 / BP1", "CCK", "1.17", "103.5"));
    		mrtStations.put("YWT", new MRTStation("Yew Tee", "NS5", "YWT", "1.17", "103.5"));
    		mrtStations.put("KRJ", new MRTStation("Kranji", "NS7", "KRJ", "1.17", "103.5"));
    		mrtStations.put("MSL", new MRTStation("Marsiling", "NS8", "MSL", "1.17", "103.5"));
    		mrtStations.put("WDL", new MRTStation("Woodlands", "NS9", "WDL", "1.17", "103.5"));
    		mrtStations.put("ADM", new MRTStation("Admiralty", "NS10", "ADM", "1.17", "103.5"));
    		mrtStations.put("SBW", new MRTStation("Sembawang", "NS11", "SBW", "1.17", "103.5"));
    		mrtStations.put("YIS", new MRTStation("Yishun", "NS13", "YIS", "1.17", "103.5"));
    		mrtStations.put("KTB", new MRTStation("Khatib", "NS14", "KTB", "1.17", "103.5"));
    		mrtStations.put("YCK", new MRTStation("Yio Chu Kang", "NS15", "YCK", "1.17", "103.5"));
    		mrtStations.put("AMK", new MRTStation("Ang Mo Kio", "NS16", "AMK", "1.17", "103.5"));
    		mrtStations.put("BSH", new MRTStation("Bishan", "NS17 / CC15", "BSH", "1.17", "103.5"));
    		mrtStations.put("BDL", new MRTStation("Braddell", "NS18", "BDL", "1.17", "103.5"));
    		mrtStations.put("TAP", new MRTStation("Toa Payoh", "NS19", "TAP", "1.17", "103.5"));
    		mrtStations.put("NOV", new MRTStation("Novena", "NS20", "NOV", "1.17", "103.5"));
    		mrtStations.put("NEW", new MRTStation("Newton", "NS21 / DT11", "NEW", "1.17", "103.5"));
    		mrtStations.put("ORC", new MRTStation("Orchard", "NS22", "ORC", "1.17", "103.5"));
    		mrtStations.put("SOM", new MRTStation("Somerset", "NS23", "SOM", "1.17", "103.5"));
    		mrtStations.put("DBG", new MRTStation("Dhoby Ghaut", "NS24 / NE6 / CC1", "DBG", "1.17", "103.5"));
    		mrtStations.put("CTH", new MRTStation("City Hall", "NS25 / EW13", "CTH", "1.17", "103.5"));
    		mrtStations.put("RFP", new MRTStation("Raffles Place", "NS26 / EW14", "RFP", "1.17", "103.5"));
    		mrtStations.put("MRB", new MRTStation("Marina Bay", "NS27 / CE2", "MRB", "1.17", "103.5"));

    	}
    	return mrtStations.get(code);
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
        	if(mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
        		if(mTransaction.getUserData().startsWith("SVC")) {
        			String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
        			if(isSbsBus(routeString))
        				return "SBS Transit";
        			else
        				return "SMRT";
        		}
        		return "?ROUTE";
        	}
        	else if(mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND)
        		return "Bus Refund";
        	else if(mTransaction.getType() == CEPASTransaction.TransactionType.MRT)
        		return "MRT";
        	else
        		return "Unknown Agency";
        }

        @Override
        public String getShortAgencyName () {
            return "";
        }
        
        @Override
        public String getRouteName () {
    		if(mTransaction.getUserData().startsWith("SVC")) {
    			String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
    			return "Bus " + routeString;
    		}
    		if(mTransaction.getUserData().charAt(3) == '-') {
    			String startStationAbbr = mTransaction.getUserData().substring(0,3);
    			String endStationAbbr = mTransaction.getUserData().substring(4,7);
    			
    			MRTStation startStation = EZLinkTransitData.getStation(startStationAbbr);
    			MRTStation endStation = EZLinkTransitData.getStation(endStationAbbr);
    			
    			if(startStation != null)
    				startStationAbbr = startStation.getName();
    			if(endStation != null)
    				endStationAbbr = endStation.getName();
    			
    			return startStationAbbr + " Ð " + endStationAbbr;
    		}
        	return mTransaction.getUserData();

        }

        @Override
        public String getStationName () {
        	return getAgencyName();
        }

        @Override
        public String getFareString () {
        	String result;
            result = NumberFormat.getCurrencyInstance(Locale.US).format(Math.abs(mTransaction.getAmount()/100.0));
            if(mTransaction.getAmount() < 0)
            	result = "-" + result;
            return result;
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
        	return null;
        }
    }
        
}
