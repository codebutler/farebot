/*
 * ClipperTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2014 Bao-Long Nguyen-Trong <baolong@inkling.com>
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

package com.codebutler.farebot.transit.clipper;

import android.support.annotation.NonNull;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Locale;

@AutoValue
abstract class ClipperTrip extends Trip {

    @NonNull
    public static Builder builder() {
        return new AutoValue_ClipperTrip.Builder();
    }

    @NonNull
    public static Builder builder(@NonNull ClipperTrip source) {
        return new AutoValue_ClipperTrip.Builder(source);
    }

    @Override
    public String getAgencyName() {
        return ClipperTransitData.getAgencyName((int) getAgency());
    }

    @Override
    public String getShortAgencyName() {
        return ClipperTransitData.getShortAgencyName((int) getAgency());
    }

    @Override
    public String getRouteName() {
        if (getAgency() == ClipperData.AGENCY_GG_FERRY) {
            return ClipperData.GG_FERRY_ROUTES.get(getRoute());
        } else {
            // FIXME: Need to find bus route #s
            // return "(Route 0x" + Long.toString(getRoute(), 16) + ")";
            return null;
        }
    }

    @Override
    public String getFareString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getFare() / 100.0);
    }

    @Override
    public boolean hasFare() {
        return true;
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double) getBalance() / 100.0);
    }

    @Override
    public Station getStartStation() {
        if (getAgency() == ClipperData.AGENCY_BART) {
            if (ClipperData.BART_STATIONS.containsKey(getFrom())) {
                return ClipperData.BART_STATIONS.get(getFrom());
            }
        } else if (getAgency() == ClipperData.AGENCY_GG_FERRY) {
            if (ClipperData.GG_FERRY_TERIMINALS.containsKey(getFrom())) {
                return ClipperData.GG_FERRY_TERIMINALS.get(getFrom());
            }
        } else if (getAgency() == ClipperData.AGENCY_SF_BAY_FERRY) {
            if (ClipperData.SF_BAY_FERRY_TERMINALS.containsKey(getFrom())) {
                return ClipperData.SF_BAY_FERRY_TERMINALS.get(getFrom());
            }
        }
        return null;
    }

    @Override
    public Station getEndStation() {
        if (getAgency() == ClipperData.AGENCY_BART) {
            if (ClipperData.BART_STATIONS.containsKey(getTo())) {
                return ClipperData.BART_STATIONS.get(getTo());
            }
        } else if (getAgency() == ClipperData.AGENCY_GG_FERRY) {
            if (ClipperData.GG_FERRY_TERIMINALS.containsKey(getTo())) {
                return ClipperData.GG_FERRY_TERIMINALS.get(getTo());
            }
        } else if (getAgency() == ClipperData.AGENCY_SF_BAY_FERRY) {
            if (ClipperData.SF_BAY_FERRY_TERMINALS.containsKey(getTo())) {
                return ClipperData.SF_BAY_FERRY_TERMINALS.get(getTo());
            }
        }
        return null;
    }

    @Override
    public String getStartStationName() {
        if (getAgency() == ClipperData.AGENCY_BART
                || getAgency() == ClipperData.AGENCY_GG_FERRY
                || getAgency() == ClipperData.AGENCY_SF_BAY_FERRY) {
            Station station = getStartStation();
            if (station != null) {
                return station.getDisplayStationName();
            } else {
                return "Station #0x" + Long.toString(getFrom(), 16);
            }
        } else if (getAgency() == ClipperData.AGENCY_MUNI) {
            return null; // Coach number is not collected
        } else if (getAgency() == ClipperData.AGENCY_GGT || getAgency() == ClipperData.AGENCY_CALTRAIN) {
            return "Zone #" + getFrom();
        } else {
            return "(Unknown Station)";
        }
    }

    @Override
    public String getEndStationName() {
        if (getAgency() == ClipperData.AGENCY_BART
                || getAgency() == ClipperData.AGENCY_GG_FERRY
                || getAgency() == ClipperData.AGENCY_SF_BAY_FERRY) {
            Station station = getEndStation();
            if (station != null) {
                return station.getDisplayStationName();
            } else {
                return "Station #0x" + Long.toString(getTo(), 16);
            }
        } else if (getAgency() == ClipperData.AGENCY_MUNI) {
            return null; // Coach number is not collected
        } else if (getAgency() == ClipperData.AGENCY_GGT || getAgency() == ClipperData.AGENCY_CALTRAIN) {
            if (getTo() == 0xffff) {
                return "(End of line)";
            }
            return "Zone #0x" + Long.toString(getTo(), 16);
        } else {
            return "(Unknown Station)";
        }
    }

    @Override
    public Mode getMode() {
        switch ((int) getAgency()) {
            case ClipperData.AGENCY_ACTRAN:
                return Mode.BUS;
            case ClipperData.AGENCY_BART:
                return Mode.METRO;
            case ClipperData.AGENCY_CALTRAIN:
                return Mode.TRAIN;
            case ClipperData.AGENCY_GGT:
                return Mode.BUS;
            case ClipperData.AGENCY_SAMTRANS:
                return Mode.BUS;
            case ClipperData.AGENCY_VTA:
                return Mode.BUS; // FIXME: or Mode.TRAM for light rail
            case ClipperData.AGENCY_MUNI:
                return Mode.BUS; // FIXME: or Mode.TRAM for "Muni Metro"
            case ClipperData.AGENCY_GG_FERRY:
                return Mode.FERRY;
            case ClipperData.AGENCY_SF_BAY_FERRY:
                return Mode.FERRY;
            default:
                return Mode.OTHER;
        }
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    public abstract long getBalance();

    public abstract long getFare();

    public abstract long getAgency();

    public abstract long getFrom();

    public abstract long getTo();

    public abstract long getRoute();

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder timestamp(long timestamp);

        abstract Builder exitTimestamp(long exitTimestamp);

        abstract Builder balance(long balance);

        abstract Builder fare(long fare);

        abstract Builder agency(long agency);

        abstract Builder from(long from);

        abstract Builder to(long to);

        abstract Builder route(long route);

        abstract ClipperTrip build();
    }
}
