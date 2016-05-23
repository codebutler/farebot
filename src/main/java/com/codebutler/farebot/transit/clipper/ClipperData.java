package com.codebutler.farebot.transit.clipper;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.util.ImmutableMapBuilder;

import java.util.Map;

final class ClipperData {
    static final int AGENCY_ACTRAN       = 0x01;
    static final int AGENCY_BART         = 0x04;
    static final int AGENCY_CALTRAIN     = 0x06;
    static final int AGENCY_GGT          = 0x0b;
    static final int AGENCY_SAMTRANS     = 0x0f;
    static final int AGENCY_VTA          = 0x11;
    static final int AGENCY_MUNI         = 0x12;
    static final int AGENCY_GG_FERRY     = 0x19;
    static final int AGENCY_SF_BAY_FERRY = 0x1b;
    static final int AGENCY_WHOLE_FOODS  = 0x302;

    static final Map<Integer, String> AGENCIES = new ImmutableMapBuilder<Integer, String>()
        .put(AGENCY_ACTRAN,       "Alameda-Contra Costa Transit District")
        .put(AGENCY_BART,         "Bay Area Rapid Transit")
        .put(AGENCY_CALTRAIN,     "Caltrain")
        .put(AGENCY_GGT,          "Golden Gate Transit")
        .put(AGENCY_SAMTRANS,     "San Mateo County Transit District")
        .put(AGENCY_VTA,          "Santa Clara Valley Transportation Authority")
        .put(AGENCY_MUNI,         "San Francisco Municipal")
        .put(AGENCY_GG_FERRY,     "Golden Gate Ferry")
        .put(AGENCY_SF_BAY_FERRY, "San Francisco Bay Ferry")
        .put(AGENCY_WHOLE_FOODS,  "Whole Foods Grocery Store")
        .build();

    static final Map<Integer, String> SHORT_AGENCIES = new ImmutableMapBuilder<Integer, String>()
        .put(AGENCY_ACTRAN,       "ACTransit")
        .put(AGENCY_BART,         "BART")
        .put(AGENCY_CALTRAIN,     "Caltrain")
        .put(AGENCY_GGT,          "GGT")
        .put(AGENCY_SAMTRANS,     "SAMTRANS")
        .put(AGENCY_VTA,          "VTA")
        .put(AGENCY_MUNI,         "Muni")
        .put(AGENCY_GG_FERRY,     "GG Ferry")
        .put(AGENCY_SF_BAY_FERRY, "SF Bay Ferry")
        .put(AGENCY_WHOLE_FOODS,  "Whole Foods")
        .build();

    static final Map<Long, Station> BART_STATIONS = new ImmutableMapBuilder<Long, Station>()
        .put(0x01L, new Station("Colma Station",                       "Colma",                "37.68468",  "-122.46626"))
        .put(0x02L, new Station("Daly City Station",                   "Daly City",            "37.70608",  "-122.46908"))
        .put(0x03L, new Station("Balboa Park Station",                 "Balboa Park",          "37.721556", "-122.447503"))
        .put(0x04L, new Station("Glen Park Station",                   "Glen Park",            "37.733118", "-122.433808"))
        .put(0x05L, new Station("24th St. Mission Station",            "24th St.",             "37.75226",  "-122.41849"))
        .put(0x06L, new Station("16th St. Mission Station",            "16th St.",             "37.765228", "-122.419478"))
        .put(0x07L, new Station("Civic Center Station",                "Civic Center",         "37.779538", "-122.413788"))
        .put(0x08L, new Station("Powell Street Station",               "Powell St.",           "37.784970", "-122.40701"))
        .put(0x09L, new Station("Montgomery St. Station",              "Montgomery",           "37.789336", "-122.401486"))
        .put(0x0aL, new Station("Embarcadero Station",                 "Embarcadero",          "37.793086", "-122.396276"))
        .put(0x0bL, new Station("West Oakland Station",                "West Oakland",         "37.805296", "-122.294938"))
        .put(0x0cL, new Station("12th Street Oakland City Center",     "12th St.",             "37.802956", "-122.2720367"))
        .put(0x0dL, new Station("19th Street Oakland Station",         "19th St.",             "37.80762",  "-122.26886"))
        .put(0x0eL, new Station("MacArthur Station",                   "MacArthur",            "37.82928",  "-122.26661"))
        .put(0x0fL, new Station("Rockridge Station",                   "Rockridge",            "37.84463",  "-122.251825"))
        .put(0x13L, new Station("Walnut Creek Station",                "Walnut Creek",         "37.90563",  "-122.06744"))
        .put(0x14L, new Station("Concord Station",                     "Concord",              "37.97376",  "-122.02903"))
        .put(0x15L, new Station("North Concord/Martinez Station",      "N. Concord/Martinez",  "38.00318",  "-122.02463"))
        .put(0x17L, new Station("Ashby Station",                       "Ashby",                "37.85303",  "-122.269965"))
        .put(0x18L, new Station("Downtown Berkeley Station",           "Berkeley",             "37.869868", "-122.268051"))
        .put(0x19L, new Station("North Berkeley Station",              "North Berkeley",       "37.874026", "-122.283882"))
        .put(0x20L, new Station("Coliseum/Oakland Airport BART",       "Coliseum/OAK",         "37.754270", "-122.197757"))
        .put(0x1aL, new Station("El Cerrito Plaza Station",            "El Cerrito Plaza",     "37.903959", "-122.299271"))
        .put(0x1bL, new Station("El Cerrito Del Norte Station",        "El Cerrito Del Norte", "37.925651", "-122.317219"))
        .put(0x1cL, new Station("Richmond Station",                    "Richmond",             "37.93730",  "-122.35338"))
        .put(0x1dL, new Station("Lake Merritt Station",                "Lake Merritt",         "37.79761",  "-122.26564"))
        .put(0x1eL, new Station("Fruitvale Station",                   "Fruitvale",            "37.77495",  "-122.22425"))
        .put(0x1fL, new Station("Coliseum/Oakland Airport Station",    "Coliseum/OAK",         "37.75256",  "-122.19806"))
        .put(0x22L, new Station("Hayward Station",                     "Hayward",              "37.670387", "-122.088002"))
        .put(0x23L, new Station("South Hayward Station",               "South Hayward",        "37.634800", "-122.057551"))
        .put(0x24L, new Station("Union City Station",                  "Union City",           "37.591203", "-122.017854"))
        .put(0x25L, new Station("Fremont Station",                     "Fremont",              "37.557727", "-121.976395"))
        .put(0x26L, new Station("Daly City Station",                   "Daly City",            "37.7066",   "-122.4696"))
        .put(0x28L, new Station("South San Francisco Station",         "South SF",             "37.6744",   "-122.442"))
        .put(0x29L, new Station("San Bruno Station",                   "San Bruno",            "37.63714",  "-122.415622"))
        .put(0x2aL, new Station("San Francisco Int'l Airport Station", "SFO",                  "37.61590",  "-122.39263"))
        .put(0x2bL, new Station("Millbrae Station",                    "Millbrae",             "37.599935", "-122.386478"))
        .put(0x2cL, new Station("West Dublin/Pleasanton Station",      "W. Dublin/Pleasanton", "37.699764", "-121.928118"))
        .build();

    static final Map<Long, String> GG_FERRY_ROUTES = new ImmutableMapBuilder<Long, String>()
        .put(0x03L, "Larkspur")
        .put(0x04L, "San Francisco")
        .build();

    static final Map<Long, Station> GG_FERRY_TERIMINALS = new ImmutableMapBuilder<Long, Station>()
        .put(0x01L, new Station("San Francisco Ferry Building", "San Francisco", "37.795873", "-122.391987"))
        .put(0x03L, new Station("Larkspur Ferry Terminal", "Larkspur", "37.945509", "-122.50916"))
        .build();

    static final Map<Long, Station> SF_BAY_FERRY_TERMINALS = new ImmutableMapBuilder<Long, Station>()
        .put(0x01L, new Station("Alameda Main Street Terminal", "Alameda Main St.", "37.790668", "-122.294036"))
        .put(0x08L, new Station("San Francisco Ferry Building", "Ferry Building", "37.795873", "-122.391987"))
        .build();

    private ClipperData() { }
}
