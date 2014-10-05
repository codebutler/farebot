package com.codebutler.farebot.transit.clipper;

import com.codebutler.farebot.transit.Station;

import java.util.HashMap;
import java.util.Map;

final class ClipperData {
    static final int AGENCY_ACTRAN   = 0x01;
    static final int AGENCY_BART     = 0x04;
    static final int AGENCY_CALTRAIN = 0x06;
    static final int AGENCY_GGT      = 0x0b;
    static final int AGENCY_SAMTRANS = 0x0f;
    static final int AGENCY_VTA      = 0x11;
    static final int AGENCY_MUNI     = 0x12;
    static final int AGENCY_FERRY    = 0x19;

    static final Map<Integer, String> AGENCIES = new HashMap<Integer, String>() {{
        put(AGENCY_ACTRAN,   "Alameda-Contra Costa Transit District");
        put(AGENCY_BART,     "Bay Area Rapid Transit");
        put(AGENCY_CALTRAIN, "Caltrain");
        put(AGENCY_GGT,      "Golden Gate Transit");
        put(AGENCY_SAMTRANS, "San Mateo County Transit District");
        put(AGENCY_VTA,      "Santa Clara Valley Transportation Authority");
        put(AGENCY_MUNI,     "San Francisco Municipal");
        put(AGENCY_FERRY,    "Golden Gate Ferry");
    }};

    static final Map<Integer, String> SHORT_AGENCIES = new HashMap<Integer, String>() {{
        put(AGENCY_ACTRAN,   "ACTransit");
        put(AGENCY_BART,     "BART");
        put(AGENCY_CALTRAIN, "Caltrain");
        put(AGENCY_GGT,      "GGT");
        put(AGENCY_SAMTRANS, "SAMTRANS");
        put(AGENCY_VTA,      "VTA");
        put(AGENCY_MUNI,     "Muni");
        put(AGENCY_FERRY,    "Ferry");
    }};

    static final Map<Long, Station> BART_STATIONS = new HashMap<Long, Station>() {{
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
        put((long)0x0b, new Station("West Oakland Station",                      "West Oakland",         "37.805296", "-122.294938"));
        put((long)0x0c, new Station("12th Street Oakland City Center",           "12th St.",             "37.802956", "-122.2720367"));
        put((long)0x0d, new Station("19th Street Oakland Station",               "19th St.",             "37.80762",  "-122.26886"));
        put((long)0x0e, new Station("MacArthur Station",                         "MacArthur",            "37.82928",  "-122.26661"));
        put((long)0x0f, new Station("Rockridge Station",                         "Rockridge",            "37.84463",  "-122.251825"));
        put((long)0x13, new Station("Walnut Creek Station",                      "Walnut Creek",         "37.90563",  "-122.06744"));
        put((long)0x14, new Station("Concord Station",                           "Concord",              "37.97376",  "-122.02903"));
        put((long)0x15, new Station("North Concord/Martinez Station",            "N. Concord/Martinez",  "38.00318",  "-122.02463"));
        put((long)0x17, new Station("Ashby Station",                             "Ashby",                "37.85303",  "-122.269965"));
        put((long)0x18, new Station("Downtown Berkeley Station",                 "Berkeley",             "37.869868", "-122.268051"));
        put((long)0x19, new Station("North Berkeley Station",                    "North Berkeley",       "37.874026", "-122.283882"));
        put((long)0x20, new Station("Coliseum/Oakland Airport BART",             "Coliseum/OAK",         "37.754270", "-122.197757"));
        put((long)0x1a, new Station("El Cerrito Plaza Station",                  "El Cerrito Plaza",     "37.903959", "-122.299271"));
        put((long)0x1b, new Station("El Cerrito Del Norte Station",              "El Cerrito Del Norte", "37.925651", "-122.317219"));
        put((long)0x1c, new Station("Richmond Station",                          "Richmond",             "37.93730",  "-122.35338"));
        put((long)0x1d, new Station("Lake Merritt Station",                      "Lake Merritt",         "37.79761",  "-122.26564"));
        put((long)0x1e, new Station("Fruitvale Station",                         "Fruitvale",            "37.77495",  "-122.22425"));
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
        put((long)0x2c, new Station("West Dublin/Pleasanton Station",            "W. Dublin/Pleasanton", "37.699764", "-121.928118"));
    }};

    static Map<Long, String> FERRY_ROUTES = new HashMap<Long, String>() {{
        put((long)0x03, "Larkspur");
        put((long)0x04, "San Francisco");
    }};

    static Map<Long, Station> FERRY_TERMINALS = new HashMap<Long, Station>() {{
        put((long)0x01, new Station("San Francisco Ferry Building", "San Francisco", "37.795873", "-122.391987"));
        put((long)0x03, new Station("Larkspur Ferry Terminal", "Larkspur", "37.945509", "-122.50916"));
    }};

    private ClipperData() { }
}
