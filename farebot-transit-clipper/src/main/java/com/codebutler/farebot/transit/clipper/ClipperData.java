/*
 * ClipperData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2014 Bao-Long Nguyen-Trong <baolong@inkling.com>
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

package com.codebutler.farebot.transit.clipper;

import com.codebutler.farebot.transit.Station;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@SuppressWarnings("checkstyle:linelength")
final class ClipperData {

    static final int AGENCY_ACTRAN = 0x01;
    static final int AGENCY_BART = 0x04;
    static final int AGENCY_CALTRAIN = 0x06;
    static final int AGENCY_GGT = 0x0b;
    static final int AGENCY_SAMTRANS = 0x0f;
    static final int AGENCY_VTA = 0x11;
    static final int AGENCY_MUNI = 0x12;
    static final int AGENCY_GG_FERRY = 0x19;
    static final int AGENCY_SF_BAY_FERRY = 0x1b;
    static final int AGENCY_CALTRAIN_8RIDE = 0x173;

    static final Map<Integer, String> AGENCIES = ImmutableMap.<Integer, String>builder()
            .put(AGENCY_ACTRAN, "Alameda-Contra Costa Transit District")
            .put(AGENCY_BART, "Bay Area Rapid Transit")
            .put(AGENCY_CALTRAIN, "Caltrain")
            .put(AGENCY_GGT, "Golden Gate Transit")
            .put(AGENCY_SAMTRANS, "San Mateo County Transit District")
            .put(AGENCY_VTA, "Santa Clara Valley Transportation Authority")
            .put(AGENCY_MUNI, "San Francisco Municipal")
            .put(AGENCY_GG_FERRY, "Golden Gate Ferry")
            .put(AGENCY_SF_BAY_FERRY, "San Francisco Bay Ferry")
            .put(AGENCY_CALTRAIN_8RIDE, "Caltrain 8-Rides")
            .build();

    static final Map<Integer, String> SHORT_AGENCIES = ImmutableMap.<Integer, String>builder()
            .put(AGENCY_ACTRAN, "ACTransit")
            .put(AGENCY_BART, "BART")
            .put(AGENCY_CALTRAIN, "Caltrain")
            .put(AGENCY_GGT, "GGT")
            .put(AGENCY_SAMTRANS, "SAMTRANS")
            .put(AGENCY_VTA, "VTA")
            .put(AGENCY_MUNI, "Muni")
            .put(AGENCY_GG_FERRY, "GG Ferry")
            .put(AGENCY_SF_BAY_FERRY, "SF Bay Ferry")
            .put(AGENCY_CALTRAIN_8RIDE, "Caltrain")
            .build();

    static final Map<Long, Station> BART_STATIONS = ImmutableMap.<Long, Station>builder()
            .put(0x01L, Station.create("Colma Station", "Colma", "37.68468", "-122.46626"))
            .put(0x02L, Station.create("Daly City Station", "Daly City", "37.70608", "-122.46908"))
            .put(0x03L, Station.create("Balboa Park Station", "Balboa Park", "37.721556", "-122.447503"))
            .put(0x04L, Station.create("Glen Park Station", "Glen Park", "37.733118", "-122.433808"))
            .put(0x05L, Station.create("24th St. Mission Station", "24th St.", "37.75226", "-122.41849"))
            .put(0x06L, Station.create("16th St. Mission Station", "16th St.", "37.765228", "-122.419478"))
            .put(0x07L, Station.create("Civic Center Station", "Civic Center", "37.779538", "-122.413788"))
            .put(0x08L, Station.create("Powell Street Station", "Powell St.", "37.784970", "-122.40701"))
            .put(0x09L, Station.create("Montgomery St. Station", "Montgomery", "37.789336", "-122.401486"))
            .put(0x0aL, Station.create("Embarcadero Station", "Embarcadero", "37.793086", "-122.396276"))
            .put(0x0bL, Station.create("West Oakland Station", "West Oakland", "37.805296", "-122.294938"))
            .put(0x0cL, Station.create("12th Street Oakland City Center", "12th St.", "37.802956", "-122.2720367"))
            .put(0x0dL, Station.create("19th Street Oakland Station", "19th St.", "37.80762", "-122.26886"))
            .put(0x0eL, Station.create("MacArthur Station", "MacArthur", "37.82928", "-122.26661"))
            .put(0x0fL, Station.create("Rockridge Station", "Rockridge", "37.84463", "-122.251825"))
            .put(0x12L, Station.create("Walnut Creek Station", "Walnut Creek", "37.90563", "-122.06744"))
            .put(0x14L, Station.create("Concord Station", "Concord", "37.97376", "-122.02903"))
            .put(0x15L, Station.create("North Concord/Martinez Station", "N. Concord/Martinez", "38.00318", "-122.02463"))
            .put(0x17L, Station.create("Ashby Station", "Ashby", "37.85303", "-122.269965"))
            .put(0x18L, Station.create("Downtown Berkeley Station", "Berkeley", "37.869868", "-122.268051"))
            .put(0x19L, Station.create("North Berkeley Station", "North Berkeley", "37.874026", "-122.283882"))
            .put(0x20L, Station.create("Coliseum Station", "Coliseum", "37.754270", "-122.197757"))
            .put(0x1aL, Station.create("El Cerrito Plaza Station", "El Cerrito Plaza", "37.903959", "-122.299271"))
            .put(0x1bL, Station.create("El Cerrito Del Norte Station", "El Cerrito Del Norte", "37.925651", "-122.317219"))
            .put(0x1cL, Station.create("Richmond Station", "Richmond", "37.93730", "-122.35338"))
            .put(0x1dL, Station.create("Lake Merritt Station", "Lake Merritt", "37.79761", "-122.26564"))
            .put(0x1eL, Station.create("Fruitvale Station", "Fruitvale", "37.77495", "-122.22425"))
            .put(0x1fL, Station.create("Coliseum Station", "Coliseum", "37.75256", "-122.19806"))
            .put(0x22L, Station.create("Hayward Station", "Hayward", "37.670387", "-122.088002"))
            .put(0x23L, Station.create("South Hayward Station", "South Hayward", "37.634800", "-122.057551"))
            .put(0x24L, Station.create("Union City Station", "Union City", "37.591203", "-122.017854"))
            .put(0x25L, Station.create("Fremont Station", "Fremont", "37.557727", "-121.976395"))
            .put(0x26L, Station.create("Daly City Station", "Daly City", "37.7066", "-122.4696"))
            .put(0x28L, Station.create("South San Francisco Station", "South SF", "37.6744", "-122.442"))
            .put(0x29L, Station.create("San Bruno Station", "San Bruno", "37.63714", "-122.415622"))
            .put(0x2aL, Station.create("San Francisco Int'l Airport Station", "SFO", "37.61590", "-122.39263"))
            .put(0x2bL, Station.create("Millbrae Station", "Millbrae", "37.599935", "-122.386478"))
            .put(0x2cL, Station.create("West Dublin/Pleasanton Station", "W. Dublin/Pleasanton", "37.699764", "-121.928118"))
            .put(0x2dL, Station.create("Oakland Airport Station", "OAK Airport", "37.75256", "-122.19806"))
            .build();

    static final Map<Long, String> GG_FERRY_ROUTES = ImmutableMap.<Long, String>builder()
            .put(0x03L, "Larkspur")
            .put(0x04L, "San Francisco")
            .build();

    static final Map<Long, Station> GG_FERRY_TERIMINALS = ImmutableMap.<Long, Station>builder()
            .put(0x01L, Station.create("San Francisco Ferry Building", "San Francisco", "37.795873", "-122.391987"))
            .put(0x03L, Station.create("Larkspur Ferry Terminal", "Larkspur", "37.945509", "-122.50916"))
            .build();

    static final Map<Long, Station> SF_BAY_FERRY_TERMINALS = ImmutableMap.<Long, Station>builder()
            .put(0x01L, Station.create("Alameda Main Street Terminal", "Alameda Main St.", "37.790668", "-122.294036"))
            .put(0x08L, Station.create("San Francisco Ferry Building", "Ferry Building", "37.795873", "-122.391987"))
            .build();

    private ClipperData() { }
}
