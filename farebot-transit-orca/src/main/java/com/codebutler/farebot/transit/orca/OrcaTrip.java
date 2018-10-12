/*
 * OrcaTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2014 Kramer Campbell <kramer@kramerc.com>
 * Copyright (C) 2015 Sean CyberKitsune McClenaghan <cyberkitsune09@gmail.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.orca;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.desfire.DesfireRecord;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@AutoValue
public abstract class OrcaTrip extends Trip {

    private static final Map<Long, Station> LINK_STATIONS = ImmutableMap.<Long, Station>builder()
            .put(10352L, Station.create("Capitol Hill Station", "Capitol Hill", "47.6192", "-122.3202"))
            .put(10351L, Station.create("University of Washington Station", "UW Station", "47.6496", "-122.3037"))
            .put(13193L, Station.create("Westlake Station", "Westlake", "47.6113968", "-122.337502"))
            .put(13194L, Station.create("University Street Station", "University Street", "47.6072502", "-122.335754"))
            .put(13195L, Station.create("Pioneer Square Station", "Pioneer Sq", "47.6021461", "-122.33107"))
            .put(13196L, Station.create("International District Station", "ID", "47.5976601", "-122.328217"))
            .put(13197L, Station.create("Stadium Station", "Stadium", "47.5918121", "-122.327354"))
            .put(13198L, Station.create("SODO Station", "SODO", "47.5799484", "-122.327515"))
            .put(13199L, Station.create("Beacon Hill Station", "Beacon Hill", "47.5791245", "-122.311287"))
            .put(13200L, Station.create("Mount Baker Station", "Mount Baker", "47.5764389", "-122.297737"))
            .put(13201L, Station.create("Columbia City Station", "Columbia City", "47.5589523", "-122.292343"))
            .put(13202L, Station.create("Othello Station", "Othello", "47.5375366", "-122.281471"))
            .put(13203L, Station.create("Rainier Beach Station", "Rainier Beach", "47.5222626", "-122.279579"))
            .put(13204L, Station.create("Tukwila International Blvd Station", "Tukwila", "47.4642754", "-122.288391"))
            .put(13205L, Station.create("Seatac Airport Station", "Sea-Tac", "47.4445305", "-122.297012"))
            .put(10353L, Station.create("Angle Lake Station", "Angle Lake", "47.4227143", "-122.2978669"))
            .build();

    private static Map<Integer, Station> sSounderStations = ImmutableMap.<Integer, Station>builder()
            .put(1, Station.create("Everett Station", "Everett", "47.9747155", "-122.1996922"))
            .put(2, Station.create("Edmonds Station", "Edmonds", "47.8109946","-122.3864407"))
            .put(3, Station.create("King Street Station", "King Street", "47.598445", "-122.330161"))
            .put(4, Station.create("Tuwkila Station", "Tukwila", "47.4603283", "-122.2421456"))
            .put(5, Station.create("Kent Station", "Kent", "47.384257", "-122.233151"))
            .put(6, Station.create("Auburn Station", "Auburn", "47.3065191", "-122.2343063"))
            .put(7, Station.create("Sumner Station", "Sumner", "47.2016577", "-122.2467547"))
            .put(8, Station.create("Puyallup Station", "Puyallup", "47.1926213", "-122.2977392"))
            .put(9, Station.create("Tacoma Dome Station", "Tacoma Dome", "47.2408695", "-122.4278904"))
            .put(0x1e01, Station.create("Mukilteo Station", "Mukilteo", "47.9491683", "-122.3010919"))
            .put(0x1e02, Station.create("Lakewood Station", "Lakewood", "47.1529884", "-122.5015344"))
            .put(0x37e5, Station.create("South Tacoma Station", "South Tacoma", "47.2038608", "-122.4877278"))
            .build();

    private static Map<Integer, Station> sWSFTerminals = ImmutableMap.<Integer, Station>builder()
            .put(10101, Station.create("Seattle Terminal", "Seattle", "47.602722", "-122.338512"))
            .put(10103, Station.create("Bainbridge Island Terminal", "Bainbridge", "47.62362", "-122.51082"))
            .put(10104, Station.create("Fauntleroy Terminal", "Seattle", "47.5231", "-122.39602"))
            .put(10115, Station.create("Anacortes Terminal", "Anacortes", "48.5065077", "-122.680434"))
            .build();

    @NonNull
    static OrcaTrip create(@NonNull DesfireRecord record) {
        byte[] useData = record.getData().bytes();
        long[] usefulData = new long[useData.length];

        for (int i = 0; i < useData.length; i++) {
            usefulData[i] = ((long) useData[i]) & 0xFF;
        }

        long timestamp = ((0x0F & usefulData[3]) << 28)
                | (usefulData[4] << 20)
                | (usefulData[5] << 12)
                | (usefulData[6] << 4)
                | (usefulData[7] >> 4);

        long coachNumber = ((usefulData[9] & 0xf) << 12) | (usefulData[10] << 4) | ((usefulData[11] & 0xf0) >> 4);

        long fare;
        if (usefulData[15] == 0x00 || usefulData[15] == 0xFF) {
            // FIXME: This appears to be some sort of special case for transfers and passes.
            fare = 0;
        } else {
            fare = (usefulData[15] << 7) | (usefulData[16] >> 1);
        }

        long newBalance = (usefulData[34] << 8) | usefulData[35];
        long agency = usefulData[3] >> 4;
        long transType = (usefulData[17]);

        return new AutoValue_OrcaTrip(timestamp, agency, transType, coachNumber, fare, newBalance);
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        switch ((int) getAgency()) {
            case OrcaTransitInfo.AGENCY_CT:
                return resources.getString(R.string.transit_orca_agency_ct);
            case OrcaTransitInfo.AGENCY_KCM:
                return resources.getString(R.string.transit_orca_agency_kcm);
            case OrcaTransitInfo.AGENCY_PT:
                return resources.getString(R.string.transit_orca_agency_pt);
            case OrcaTransitInfo.AGENCY_ST:
                return resources.getString(R.string.transit_orca_agency_st);
            case OrcaTransitInfo.AGENCY_WSF:
                return resources.getString(R.string.transit_orca_agency_wsf);
            case OrcaTransitInfo.AGENCY_ET:
                return resources.getString(R.string.transit_orca_agency_et);
        }
        return resources.getString(R.string.transit_orca_agency_unknown, Long.toString(getAgency()));
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        switch ((int) getAgency()) {
            case OrcaTransitInfo.AGENCY_CT:
                return "CT";
            case OrcaTransitInfo.AGENCY_KCM:
                return "KCM";
            case OrcaTransitInfo.AGENCY_PT:
                return "PT";
            case OrcaTransitInfo.AGENCY_ST:
                return "ST";
            case OrcaTransitInfo.AGENCY_WSF:
                return "WSF";
            case OrcaTransitInfo.AGENCY_ET:
                return "ET";
        }
        return resources.getString(R.string.transit_orca_agency_unknown, Long.toString(getAgency()));
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        if (isLink()) {
            return resources.getString(R.string.transit_orca_route_link);
        } else if (isSounder()) {
            return resources.getString(R.string.transit_orca_route_sounder);
        } else {
            // FIXME: Need to find bus route #s
            if (getAgency() == OrcaTransitInfo.AGENCY_ST) {
                return resources.getString(R.string.transit_orca_route_express_bus);
            } else if (getAgency() == OrcaTransitInfo.AGENCY_KCM) {
                return resources.getString(R.string.transit_orca_route_bus);
            }
            return null;
        }
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getFare() / 100.0);
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(getNewBalance() / 100);
    }

    @Override
    public Station getStartStation() {
        if (isLink()) {
            return LINK_STATIONS.get(getCoachNumber());
        } else if (isSounder()) {
            return sSounderStations.get((int) getCoachNumber());
        } else if (getAgency() == OrcaTransitInfo.AGENCY_WSF) {
            return sWSFTerminals.get((int) getCoachNumber());
        }
        return null;
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        if (isLink()) {
            if (LINK_STATIONS.containsKey(getCoachNumber())) {
                return LINK_STATIONS.get(getCoachNumber()).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_station,
                        Long.toString(getCoachNumber()));
            }
        } else if (isSounder()) {
            int stationNumber = (int) getCoachNumber();
            if (sSounderStations.containsKey(stationNumber)) {
                return sSounderStations.get(stationNumber).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_station,
                        Integer.toString(stationNumber));
            }
        } else if (getAgency() == OrcaTransitInfo.AGENCY_WSF) {
            int terminalNumber = (int) getCoachNumber();
            if (sWSFTerminals.containsKey(terminalNumber)) {
                return sWSFTerminals.get(terminalNumber).getStationName();
            } else {
                return resources.getString(R.string.transit_orca_station_unknown_terminal,
                        Integer.toString(terminalNumber));
            }
        } else {
            return resources.getString(R.string.transit_orca_station_coach,
                    Long.toString(getCoachNumber()));
        }
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        // ORCA tracks destination in a separate record
        return null;
    }

    @Override
    public Station getEndStation() {
        // ORCA tracks destination in a separate record
        return null;
    }

    @Override
    public Mode getMode() {
        if (isLink()) {
            return Mode.METRO;
        } else if (isSounder()) {
            return Mode.TRAIN;
        } else if (getAgency() == OrcaTransitInfo.AGENCY_WSF) {
            return Mode.FERRY;
        } else {
            return Mode.BUS;
        }
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    private boolean isLink() {
        return (getAgency() == OrcaTransitInfo.AGENCY_ST && getCoachNumber() > 10000);
    }

    private boolean isSounder() {
        return (getAgency() == OrcaTransitInfo.AGENCY_ST && getCoachNumber() < 20);
    }

    abstract long getAgency();

    abstract long getTransType();

    abstract long getCoachNumber();

    abstract long getFare();

    abstract long getNewBalance();
}
