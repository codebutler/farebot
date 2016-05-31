/*
 * EZLinkTrip.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
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

import android.os.Parcel;

import com.codebutler.farebot.card.cepas.CEPASTransaction;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.text.NumberFormat;
import java.util.Currency;

public class EZLinkTrip extends Trip {
    private final CEPASTransaction mTransaction;
    private final String mCardName;

    public EZLinkTrip(CEPASTransaction transaction, String cardName) {
        mTransaction = transaction;
        mCardName = cardName;
    }

    private EZLinkTrip(Parcel parcel) {
        mTransaction = parcel.readParcelable(CEPASTransaction.class.getClassLoader());
        mCardName = parcel.readString();
    }

    public static final Creator<EZLinkTrip> CREATOR = new Creator<EZLinkTrip>() {
        @Override
        public EZLinkTrip createFromParcel(Parcel parcel) {
            return new EZLinkTrip(parcel);
        }

        @Override
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
    public String getAgencyName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
            if (EZLinkTransitData.SBS_BUSES.contains(routeString)) {
                return "SBS";
            }
            return "SMRT";
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION
                || mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP
                || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
            return mCardName;
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    @Override
    public String getShortAgencyName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            String routeString = mTransaction.getUserData().substring(3, 7).replace(" ", "");
            if (EZLinkTransitData.SBS_BUSES.contains(routeString)) {
                return "SBS";
            }
            return "SMRT";
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION
                || mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP
                || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
            if (mCardName.equals("EZ-Link")) {
                return "EZ";
            } else {
                return mCardName;
            }
        }
        if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "POS";
        }
        return "SMRT";
    }

    @Override
    public String getRouteName() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS) {
            if (mTransaction.getUserData().startsWith("SVC")) {
                return "Bus #" + mTransaction.getUserData().substring(3, 7).replace(" ", "");
            }
            return "(Unknown Bus Route)";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            return "Bus Refund";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT) {
            return "MRT";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP) {
            return "Top-up";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION) {
            return "First use";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL) {
            return "Retail Purchase";
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
            return "Service Charge";
        }
        return "(Unknown Route)";
    }

    @Override
    public String getFareString() {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setCurrency(Currency.getInstance("SGD"));

        int balance = -mTransaction.getAmount();
        if (balance < 0) {
            return "Credit " + numberFormat.format(-balance / 100.0);
        } else {
            return numberFormat.format(balance / 100.0);
        }
    }

    @Override
    public boolean hasFare() {
        return (mTransaction.getType() != CEPASTransaction.TransactionType.CREATION);
    }

    @Override
    public String getBalanceString() {
        return "(???)";
    }

    @Override
    public Station getStartStation() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION) {
            return null;
        }
        if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            String startStationAbbr = mTransaction.getUserData().substring(0, 3);
            return EZLinkTransitData.getStation(startStationAbbr);
        }
        return null;
    }

    @Override
    public Station getEndStation() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.CREATION) {
            return null;
        }
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
        if (startStation != null) {
            return startStation.getStationName();
        } else if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            return mTransaction.getUserData().substring(0, 3); // extract startStationAbbr
        }
        return mTransaction.getUserData();
    }

    @Override
    public String getEndStationName() {
        Station endStation = getEndStation();
        if (endStation != null) {
            return endStation.getStationName();
        } else if (mTransaction.getUserData().charAt(3) == '-'
                || mTransaction.getUserData().charAt(3) == ' ') {
            return mTransaction.getUserData().substring(4, 7); // extract endStationAbbr
        }
        return null;
    }

    @Override
    public Mode getMode() {
        if (mTransaction.getType() == CEPASTransaction.TransactionType.BUS
                || mTransaction.getType() == CEPASTransaction.TransactionType.BUS_REFUND) {
            return Mode.BUS;
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.MRT) {
            return Mode.METRO;
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.TOP_UP) {
            return Mode.TICKET_MACHINE;
        } else if (mTransaction.getType() == CEPASTransaction.TransactionType.RETAIL
                || mTransaction.getType() == CEPASTransaction.TransactionType.SERVICE) {
            return Mode.POS;
        }
        return Mode.OTHER;
    }

    @Override
    public boolean hasTime() {
        return true;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(mTransaction, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
