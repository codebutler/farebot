/*
 * EZLinkTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ezlink;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.cepas.CEPASTransaction;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.text.NumberFormat;
import java.util.Currency;

@AutoValue
abstract class EZLinkTrip extends Trip {

    @NonNull
    static EZLinkTrip create(CEPASTransaction transaction, String cardName) {
        return new AutoValue_EZLinkTrip(transaction, cardName);
    }

    @Override
    public long getTimestamp() {
        return getTransaction().getTimestamp();
    }

    @Override
    public long getExitTimestamp() {
        return 0;
    }

    @Override
    public String getAgencyName(@NonNull Resources resources) {
        if (getTransaction().getType() == CEPASTransaction.TransactionType.BUS
                || getTransaction().getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            String routeString = getTransaction().getUserData().substring(3, 7).replace(" ", "");
            if (EZLinkData.SBS_BUSES.contains(routeString)) {
                return "SBS";
            } else if (EZLinkData.CS_BUSES.contains(routeString)) {
                return "Commute Solutions";
            }
            return "SMRT";
        }
        if (getTransaction().getType() == CEPASTransaction.TransactionType.CREATION
                || getTransaction().getType() == CEPASTransaction.TransactionType.TOP_UP
                || getTransaction().getType() == CEPASTransaction.TransactionType.SERVICE) {
            return getCardName();
        }
        if (getTransaction().getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    @Override
    public String getShortAgencyName(@NonNull Resources resources) {
        if (getTransaction().getType() == CEPASTransaction.TransactionType.BUS
                || getTransaction().getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            String routeString = getTransaction().getUserData().substring(3, 7).replace(" ", "");
            if (EZLinkData.SBS_BUSES.contains(routeString)) {
                return "SBS";
            } else if (EZLinkData.CS_BUSES.contains(routeString)) {
                return "CS";
            }
            return "SMRT";
        }
        if (getTransaction().getType() == CEPASTransaction.TransactionType.CREATION
                || getTransaction().getType() == CEPASTransaction.TransactionType.TOP_UP
                || getTransaction().getType() == CEPASTransaction.TransactionType.SERVICE) {
            if (getCardName().equals("EZ-Link")) {
                return "EZ";
            } else {
                return getCardName();
            }
        }
        if (getTransaction().getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    @Override
    public String getRouteName(@NonNull Resources resources) {
        if (getTransaction().getType() == CEPASTransaction.TransactionType.BUS) {
            if (getTransaction().getUserData().startsWith("SVC")) {
                return "Bus #" + getTransaction().getUserData().substring(3, 7).replace(" ", "");
            }
            return "(Unknown Bus Route)";
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            return "Bus Refund";
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.MRT) {
            return "MRT";
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.TOP_UP) {
            return "Top-up";
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.CREATION) {
            return "First use";
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "Retail Purchase";
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.SERVICE) {
            return "Service Charge";
        }
        return "(Unknown Route)";
    }

    @Override
    public String getFareString(@NonNull Resources resources) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance("SGD"));

        int balance = -getTransaction().getAmount();
        if (balance < 0) {
            return "Credit " + numberFormat.format(-balance / 100.0);
        } else {
            return numberFormat.format(balance / 100.0);
        }
    }

    @Override
    public boolean hasFare() {
        return (getTransaction().getType() != CEPASTransaction.TransactionType.CREATION);
    }

    @Override
    public String getBalanceString() {
        return "(???)";
    }

    @Override
    public Station getStartStation() {
        if (getTransaction().getType() == CEPASTransaction.TransactionType.CREATION) {
            return null;
        }
        if (getTransaction().getUserData().charAt(3) == '-'
                || getTransaction().getUserData().charAt(3) == ' ') {
            String startStationAbbr = getTransaction().getUserData().substring(0, 3);
            return EZLinkData.getStation(startStationAbbr);
        }
        return null;
    }

    @Override
    public Station getEndStation() {
        if (getTransaction().getType() == CEPASTransaction.TransactionType.CREATION) {
            return null;
        }
        if (getTransaction().getUserData().charAt(3) == '-'
                || getTransaction().getUserData().charAt(3) == ' ') {
            String endStationAbbr = getTransaction().getUserData().substring(4, 7);
            return EZLinkData.getStation(endStationAbbr);
        }
        return null;
    }

    @Override
    public String getStartStationName(@NonNull Resources resources) {
        Station startStation = getStartStation();
        if (startStation != null) {
            return startStation.getStationName();
        } else if (getTransaction().getUserData().charAt(3) == '-'
                || getTransaction().getUserData().charAt(3) == ' ') {
            return getTransaction().getUserData().substring(0, 3); // extract startStationAbbr
        }
        return getTransaction().getUserData();
    }

    @Override
    public String getEndStationName(@NonNull Resources resources) {
        Station endStation = getEndStation();
        if (endStation != null) {
            return endStation.getStationName();
        } else if (getTransaction().getUserData().charAt(3) == '-'
                || getTransaction().getUserData().charAt(3) == ' ') {
            return getTransaction().getUserData().substring(4, 7); // extract endStationAbbr
        }
        return null;
    }

    @Override
    public Mode getMode() {
        if (getTransaction().getType() == CEPASTransaction.TransactionType.BUS
                || getTransaction().getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            return Mode.BUS;
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.MRT) {
            return Mode.METRO;
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.TOP_UP) {
            return Mode.TICKET_MACHINE;
        } else if (getTransaction().getType() == CEPASTransaction.TransactionType.RETAIL
                || getTransaction().getType() == CEPASTransaction.TransactionType.SERVICE) {
            return Mode.POS;
        }
        return Mode.OTHER;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    abstract CEPASTransaction getTransaction();

    abstract String getCardName();
}
