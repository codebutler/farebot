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

import android.os.Parcel;
import com.codebutler.farebot.ListItem;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.cepas.CEPASTransaction;
import com.codebutler.farebot.card.Card;

import java.text.NumberFormat;
import java.util.*;

public class EZLinkTransitData extends TransitData {
    private String       mSerialNumber;
    private double       mBalance;
    private EZLinkTrip[] mTrips;

    private static HashSet<String> sbsBuses = new HashSet<String> () {
        private static final long serialVersionUID = 1L; {
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
    // Coordinates taken from respective Wikipedia MRT pages
    private static TreeMap<String, MRTStation> mrtStations = new TreeMap<String, MRTStation> () {
        private static final long serialVersionUID = 1L; {
            // Transaction Codes
            put("GTM", new MRTStation("GTM Manual Top-up", "GTM", "GTM", null, null));

            // North-East Line (NEL)
            put("HBF", new MRTStation("HarbourFront", "NE1 / CC29",         "HBF", "1.265297", "103.82225"));
            put("HBC", new MRTStation("HarbourFront", "NE1 / CC29",         "HBC", "1.265297", "103.82225"));
            put("OTP", new MRTStation("Outram Park",  "NE3 / EW16",       "OTP", "1.280225", "103.839486"));
            put("CNT", new MRTStation("Chinatown",    "NE4 / DT19",       "CNT", "1.28485",  "103.844006"));
            put("CQY", new MRTStation("Clarke Quay",  "NE5",              "CQY", "1.288708", "103.846606"));
            put("DBG", new MRTStation("Dhoby Ghaut",  "NE6 / NS24 / CC1", "DBG", "1.299156", "103.845736"));
            put("LTI", new MRTStation("Little India", "NE7 / DT12",       "LTI", "1.306725", "103.849175"));
            put("FRP", new MRTStation("Farrer Park",  "NE8",              "FRP", "1.312314", "103.854028"));
            put("BNK", new MRTStation("Boon Keng",    "NE9",              "BNK", "1.319483", "103.861722"));
            put("PTP", new MRTStation("Potong Pasir", "NE10",             "PTP", "1.331161", "103.869058"));
            put("WLH", new MRTStation("Woodleigh",    "NE11",             "WLH", "1.339181", "103.870744"));
            put("SER", new MRTStation("Serangoon",    "NE12 / CC13",      "SER", "1.349944", "103.873092"));
            put("KVN", new MRTStation("Kovan",        "NE13",             "KVN", "1.360214", "103.884864"));
            put("HGN", new MRTStation("Hougang",      "NE14",             "HGN", "1.371292", "103.892161"));
            put("BGK", new MRTStation("Buangkok",     "NE15",             "BGK", "1.382728", "103.892789"));
            put("SKG", new MRTStation("Sengkang",     "NE16 / STC",       "SKG", "1.391653", "103.895133"));
            put("PGL", new MRTStation("Punggol",      "NE17 / PTC",       "PGL", "1.405264", "103.902097"));

            // Circle Line (CCL)
            put("DBG", new MRTStation("Dhoby Ghaut",     "CC1 / NS24 / NE6", "DBG", "1.299156", "103.845736"));
            put("DBN", new MRTStation("Dhoby Ghaut",     "CC1 / NS24 / NE6", "DBN", "1.299156", "103.845736")); // Alternate name (Northeast line entrance)
            put("BBS", new MRTStation("Bras Basah",      "CC2",              "BBS", "1.296931", "103.850631"));
            put("EPN", new MRTStation("Esplanade",       "CC3",              "EPN", "1.293436", "103.855381"));
            put("PMD", new MRTStation("Promenade",       "CC4 / DT15",       "PMD", "1.293131", "103.861064"));
            put("NCH", new MRTStation("Nicoll Highway",  "CC5",              "NCH", "1.299697", "103.863611"));
            put("SDM", new MRTStation("Stadium",         "CC6",              "SDM", "1.302856", "1.302856"));
            put("MBT", new MRTStation("Mountbatten",     "CC7",              "MBT", "1.306306", "103.882531"));
            put("DKT", new MRTStation("Dakota",          "CC8",              "DKT", "1.308289", "103.888253"));
            put("PYL", new MRTStation("Paya Lebar",      "CC9 / EW8",        "PYL", "1.317767", "103.892381"));
            put("MPS", new MRTStation("MacPherson",      "CC10 / DT?",       "MPS", "1.32665",  "103.890019"));
            put("TAS", new MRTStation("Tai Seng",        "CC11",             "TAS", "1.335833", "103.887942"));
            put("BLY", new MRTStation("Bartley",         "CC12",             "BLY", "1.342756", "103.879697"));
            put("SER", new MRTStation("Serangoon",       "CC13 / NE12",      "SER", "1.349944", "103.873092"));
            put("SRC", new MRTStation("Serangoon",       "CC13 / NE12",      "SER", "1.349944", "103.873092"));
            put("LRC", new MRTStation("Lorong Chuan",    "CC14",             "LRC", "1.351636", "103.864064"));
            put("BSH", new MRTStation("Bishan",          "CC15 / NS17",      "BSH", "1.351236", "103.848456"));
            put("BSC", new MRTStation("Bishan",          "CC15 / NS17",      "BSC", "1.351236", "103.848456")); // Alternate name (Circle line entrance)
            put("MRM", new MRTStation("Marymount",       "CC16",             "MRM", "1.349078", "103.839492"));
            put("CDT", new MRTStation("Caldecott",       "CC17",             "CDT", "1.337761", "103.839447"));
            put("BTN", new MRTStation("Botanic Gardens", "CC19 / DT9",       "BTN", "1.322519", "103.815406"));
            put("FRR", new MRTStation("Farrer Road",     "CC20",             "FRR", "1.317319", "103.807431"));
            put("HLV", new MRTStation("Holland Village", "CC21",             "HLV", "1.312078", "103.796208"));
            //put("", new MRTStation("Buona Vista", "EW21 / CC22", "", "1.17", "103.5")); // Reserved for Alternate name (Circle line entrance)
            put("ONH", new MRTStation("one-north",     "CC23", "ONH", "1.299331", "103.787067"));
            put("KRG", new MRTStation("Kent Ridge",    "CC24", "KRG", "1.293383", "103.784394"));
            put("HPV", new MRTStation("Haw Par Villa", "CC25", "HPV", "1.282386", "103.781867"));
            put("PPJ", new MRTStation("Pasir Panjang", "CC26", "PPJ", "1.276167", "103.791358"));
            put("LBD", new MRTStation("Labrador Park", "CC27", "LBD", "1.272267", "103.802908"));
            put("TLB", new MRTStation("Telok Blangah", "CC28", "TLB", "1.270572", "103.809678"));
            //put("", new MRTStation("HarbourFront", "CC20", "", "1.265297", "103.82225")); // Reserved for Alternate name (Circle line entrance)

            // Marina Bay Extension (CCL)
            put("BFT", new MRTStation("Bayfront", "CE1 / DT16", "BFT", "1.282347", "103.859317"));

            // Changi Airport Extension (EWL)
            put("TNM", new MRTStation("Tanah Merah",    "EW4",        "TNM", "1.327358", "103.946344"));
            put("XPO", new MRTStation("Expo",           "CG1 / DT35", "XPO", "1.335469", "103.961767"));
            put("CGA", new MRTStation("Changi Airport", "CG2",        "CGA", "1.357372", "103.988836"));

            // East-West Line (EWL)
            put("PSR", new MRTStation("Pasir Ris",      "EW1",         "PSR", "1.372411", "103.949369"));
            put("TAM", new MRTStation("Tampines",       "EW2 / DT32",  "TAM", "1.352528", "103.945322"));
            put("SIM", new MRTStation("Simei",          "EW3",         "SIM", "1.343444", "103.953172"));
            put("TNM", new MRTStation("Tanah Merah",    "EW4",         "TNM", "1.327358", "103.946344"));
            put("BDK", new MRTStation("Bedok",          "EW5",         "BDK", "1.324039", "103.930036"));
            put("KEM", new MRTStation("Kembangan",      "EW6",         "KEM", "1.320983", "103.912842"));
            put("EUN", new MRTStation("Eunos",          "EW7",         "EUN", "1.319725", "103.903108"));
            put("PYL", new MRTStation("Paya Lebar",     "EW8 / CC9",   "PYL", "1.317767", "103.892381"));
            put("ALJ", new MRTStation("Aljunied",       "EW9",         "ALJ", "1.316442", "103.882981"));
            put("KAL", new MRTStation("Kallang",        "EW10",        "KAL", "1.311469", "103.8714"));
            put("LVR", new MRTStation("Lavender",       "EW11",        "LVR", "1.307167", "103.863008"));
            put("BGS", new MRTStation("Bugis",          "EW12 / DT14", "BGS", "1.300194", "103.85615"));
            put("CTH", new MRTStation("City Hall",      "EW13 / NS25", "CTH", "1.293239", "103.852219"));
            put("RFP", new MRTStation("Raffles Place",  "EW14 / NS26", "RFP", "1.283881", "103.851533"));
            put("TPG", new MRTStation("Tanjong Pagar",  "EW15",        "TPG", "1.276439", "103.845711"));
            put("OTP", new MRTStation("Outram Park",    "EW16 / NE3",  "OTP", "1.280225", "103.839486"));
            put("OTN", new MRTStation("Outram Park",    "EW16 / NE3",  "OTN", "1.280225", "103.839486")); // Alternate name (Northeast line entrance)
            put("TIB", new MRTStation("Tiong Bahru",    "EW17",        "TIB", "1.286081", "103.826958"));
            put("RDH", new MRTStation("Redhill",        "EW18",        "RDH", "1.289733", "103.81675"));
            put("QUE", new MRTStation("Queenstown",     "EW19",        "QUE", "1.294442", "103.806114"));
            put("COM", new MRTStation("Commonwealth",   "EW20",        "COM", "1.302558", "103.798225"));
            put("BNV", new MRTStation("Buona Vista",    "EW21 / CC22", "BNV", "1.306817", "103.790428"));
            put("DVR", new MRTStation("Dover",          "EW22",        "DVR", "1.311314", "103.778658"));
            put("CLE", new MRTStation("Clementi",       "EW23",        "CLE", "1.315303", "103.765244"));
            put("JUR", new MRTStation("Jurong East",    "EW24 / NS1",  "JUR", "1.333415", "103.742119"));
            put("CNG", new MRTStation("Chinese Garden", "EW25",        "CNG", "1.342711", "103.732467"));
            put("LKS", new MRTStation("Lakeside",       "EW26",        "LKS", "1.344589", "103.721139"));
            put("BNL", new MRTStation("Boon Lay",       "EW27",        "BNL", "1.338883", "103.706208"));
            put("PNR", new MRTStation("Pioneer",        "EW28",        "PNR", "1.337578", "103.697217"));
            put("JKN", new MRTStation("Joo Koon",       "EW29",        "JKN", "1.327739", "103.678486"));

            // North-South Line (NSL)
            put("JUR", new MRTStation("Jurong East",   "NS1 / EW24",       "JUR", "1.333415", "103.742119"));
            put("BBT", new MRTStation("Bukit Batok",   "NS2",              "BBT", "1.349073", "103.749664"));
            put("BGB", new MRTStation("Bukit Gombak",  "NS3",              "BGB", "1.358702", "103.751787"));
            put("CCK", new MRTStation("Choa Chu Kang", "NS4 / BP1",        "CCK", "1.385092", "103.744322"));
            put("YWT", new MRTStation("Yew Tee",       "NS5",              "YWT", "1.396986", "103.747239"));
            put("KRJ", new MRTStation("Kranji",        "NS7",              "KRJ", "1.425047", "103.761853"));
            put("MSL", new MRTStation("Marsiling",     "NS8",              "MSL", "1.432636", "103.774283"));
            put("WDL", new MRTStation("Woodlands",     "NS9",              "WDL", "1.437094", "103.786483"));
            put("ADM", new MRTStation("Admiralty",     "NS10",             "ADM", "1.440689", "103.800933"));
            put("SBW", new MRTStation("Sembawang",     "NS11",             "SBW", "1.449025", "103.820153"));
            put("YIS", new MRTStation("Yishun",        "NS13",             "YIS", "1.429464", "103.835239"));
            put("KTB", new MRTStation("Khatib",        "NS14",             "KTB", "1.417167", "103.8329"));
            put("YCK", new MRTStation("Yio Chu Kang",  "NS15",             "YCK", "1.381906", "103.844817"));
            put("AMK", new MRTStation("Ang Mo Kio",    "NS16",             "AMK", "1.370017", "103.84945"));
            put("BSH", new MRTStation("Bishan",        "NS17 / CC15",      "BSH", "1.351236", "103.848456"));
            put("BDL", new MRTStation("Braddell",      "NS18",             "BDL", "1.340339", "103.846725"));
            put("TAP", new MRTStation("Toa Payoh",     "NS19",             "TAP", "1.332703", "103.847808"));
            put("NOV", new MRTStation("Novena",        "NS20",             "NOV", "1.320394", "103.843689"));
            put("NEW", new MRTStation("Newton",        "NS21 / DT11",      "NEW", "1.312956", "103.838442"));
            put("ORC", new MRTStation("Orchard",       "NS22",             "ORC", "1.304314", "103.831939"));
            put("SOM", new MRTStation("Somerset",      "NS23",             "SOM", "1.300514", "103.839028"));
            put("DBG", new MRTStation("Dhoby Ghaut",   "NS24 / NE6 / CC1", "DBG", "1.299156", "103.845736"));
            put("CTH", new MRTStation("City Hall",     "NS25 / EW13",      "CTH", "1.293239", "103.852219"));
            put("RFP", new MRTStation("Raffles Place", "NS26 / EW14",      "RFP", "1.283881", "103.851533"));
            put("MRB", new MRTStation("Marina Bay",    "NS27 / CE2",       "MRB", "1.276097", "103.854675"));
        }
    };

    private static String getCardIssuer(String canNo) {
        int issuerId = Integer.parseInt(canNo.substring(0,3));
        switch (issuerId) {
            case 100: return "EZ-Link";
            case 111: return "NETS";
            default: return "CEPAS";
        }
    }
    
    public static MRTStation getStation (String code) {
        return mrtStations.get(code);
    }

    public static boolean check (Card card) {
        if (card instanceof CEPASCard) {
            CEPASCard cepasCard = (CEPASCard) card;
            return cepasCard.getHistory(3) != null &&
                cepasCard.getHistory(3).isValid() &&
                cepasCard.getPurse(3) != null &&
                cepasCard.getPurse(3).isValid();
        }

        return false;
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        String canNo = Utils.getHexString(((CEPASCard) card).getPurse(3).getCAN(), "<Error>");
        return new TransitIdentity(getCardIssuer(canNo), canNo);
    }

    public Creator<EZLinkTransitData> CREATOR = new Creator<EZLinkTransitData>() {
        public EZLinkTransitData createFromParcel(Parcel parcel) {
            return new EZLinkTransitData(parcel);
        }

        public EZLinkTransitData[] newArray(int size) {
            return new EZLinkTransitData[size];
        }
    };

    public EZLinkTransitData(Parcel parcel) {
        mSerialNumber = parcel.readString();
        mBalance      = parcel.readDouble();

        mTrips = new EZLinkTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, EZLinkTrip.CREATOR);
    }

    public EZLinkTransitData (Card card) {
        CEPASCard cepasCard = (CEPASCard) card;

        mSerialNumber = Utils.getHexString(cepasCard.getPurse(3).getCAN(), "<Error>");
        mBalance      = cepasCard.getPurse(3).getPurseBalance();
        mTrips        = parseTrips(cepasCard);
    }

    @Override
    public String getCardName () {
        return getCardIssuer(mSerialNumber);
    }

    @Override
    public String getBalanceString () {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance("SGD"));
        return numberFormat.format(mBalance / 100);
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

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    private EZLinkTrip[] parseTrips (CEPASCard card) {
        CEPASTransaction[] transactions = card.getHistory(3).getTransactions();
        if (transactions != null) {
            EZLinkTrip[] trips = new EZLinkTrip[transactions.length];

            for (int i = 0; i < trips.length; i++)
                trips[i] = new EZLinkTrip(transactions[i], getCardName());

            return trips;
        }
        return new EZLinkTrip[0];
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeDouble(mBalance);

        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }

    public static class EZLinkTrip extends Trip {
        private final CEPASTransaction mTransaction;
        private final String mCardName;
        
        public EZLinkTrip (CEPASTransaction transaction, String cardName) {
            mTransaction = transaction;
            mCardName = cardName;
        }

        private EZLinkTrip (Parcel parcel) {
            mTransaction = parcel.readParcelable(CEPASTransaction.class.getClassLoader());
            mCardName = parcel.readString();
        }

        public static Creator<EZLinkTrip> CREATOR = new Creator<EZLinkTrip>() {
            public EZLinkTrip createFromParcel(Parcel parcel) {
                return new EZLinkTrip(parcel);
            }
            public EZLinkTrip[] newArray(int size) {
                return new EZLinkTrip[size];
            }
        };

        @Override
        public long getTimestamp() {
            return mTransaction.getTimestamp();
        }

        @Override
        public long getExitTimestamp() {
            return 0;
        }

        @Override
        public String getAgencyName () {
            if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
                String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
                if (sbsBuses.contains(routeString))
                    return "SBS";
                return "SMRT";
            }
            if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION || mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
                return mCardName;
            }
            if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
                return "POS";
            }
            return "SMRT";
        }

        @Override
        public String getShortAgencyName () {
            if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
                String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
                if (sbsBuses.contains(routeString))
                    return "SBS";
                return "SMRT";
            }
            if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION || mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
                if (mCardName.equals("EZ-Link")) return "EZ"; else return mCardName;
            }
            if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
                return "POS";
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
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT)
                return "MRT";
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP)
                return "Top-up";
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
                return "First use";
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL)
                return "Retail Purchase";
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE)
                return "Service Charge";
            return "(Unknown Route)";
        }

        @Override
        public String getFareString () {
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
            numberFormat.setCurrency(Currency.getInstance("SGD"));

            int balance = -mTransaction.getAmount();
            if (balance < 0)
                return "Credit " + numberFormat.format(-balance / 100.0);
            else
                return numberFormat.format(balance / 100.0);
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
            if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
                return null;
            if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
                String startStationAbbr = mTransaction.getUserData().substring(0, 3);
                return EZLinkTransitData.getStation(startStationAbbr);
            }
            return null;
        }

        @Override
        public Station getEndStation () {
            if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION)
                return null;
            if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
                String endStationAbbr = mTransaction.getUserData().substring(4, 7);
                return EZLinkTransitData.getStation(endStationAbbr);
            }
            return null;
        }

        @Override
        public String getStartStationName() {
            Station startStation = getStartStation();
            if (startStation != null)
                return startStation.getStationName();
            else if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
                return mTransaction.getUserData().substring(0, 3); // extract startStationAbbr
            }
            return mTransaction.getUserData();
        }

        @Override
        public String getEndStationName () {
            Station endStation = getEndStation();
            if (endStation != null)
                return endStation.getStationName();
            else if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
                return mTransaction.getUserData().substring(4, 7); // extract endStationAbbr
            }
            return null;
        }

        @Override
        public Mode getMode() {
            if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND)
                return Mode.BUS;
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT)
                return Mode.METRO;
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP)
                return Mode.TICKET_MACHINE;
            else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE)
                return Mode.POS;
            return Mode.OTHER;
        }

        @Override
        public boolean hasTime() {
            return true;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeParcelable(mTransaction, flags);
        }

        public int describeContents() {
            return 0;
        }
    }
}
