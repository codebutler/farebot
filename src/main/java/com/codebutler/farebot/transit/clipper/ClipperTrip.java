package com.codebutler.farebot.transit.clipper;

import android.os.Parcel;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.text.NumberFormat;
import java.util.Locale;

public class ClipperTrip extends Trip {
    final long mTimestamp;
    final long mExitTimestamp;
    final long mFare;
    final long mAgency;
    final long mFrom;
    final long mTo;
    final long mRoute;
    long mBalance;

    public ClipperTrip(long timestamp, long exitTimestamp, long fare, long agency, long from, long to, long route) {
        mTimestamp      = timestamp;
        mExitTimestamp  = exitTimestamp;
        mFare           = fare;
        mAgency         = agency;
        mFrom           = from;
        mTo             = to;
        mRoute          = route;
        mBalance        = 0;
    }

    public static Creator<ClipperTrip> CREATOR = new Creator<ClipperTrip>() {
        public ClipperTrip createFromParcel(Parcel parcel) {
            return new ClipperTrip(parcel);
        }

        public ClipperTrip[] newArray(int size) {
            return new ClipperTrip[size];
        }
    };

    ClipperTrip(Parcel parcel) {
        mTimestamp     = parcel.readLong();
        mExitTimestamp = parcel.readLong();
        mFare          = parcel.readLong();
        mAgency        = parcel.readLong();
        mFrom          = parcel.readLong();
        mTo            = parcel.readLong();
        mRoute         = parcel.readLong();
        mBalance       = parcel.readLong();
    }

    @Override public long getTimestamp () {
        return mTimestamp;
    }

    @Override public long getExitTimestamp () {
        return mExitTimestamp;
    }

    @Override public String getAgencyName () {
        return ClipperTransitData.getAgencyName((int)mAgency);
    }

    @Override public String getShortAgencyName () {
        return ClipperTransitData.getShortAgencyName((int)mAgency);
    }

    @Override public String getRouteName () {
        if (mAgency == ClipperData.AGENCY_FERRY &&
            ClipperData.FERRY_ROUTES.containsKey(mRoute)) {
            return ClipperData.FERRY_ROUTES.get(mRoute);
        } else {
            // FIXME: Need to find bus route #s
            // return "(Route 0x" + Long.toString(mRoute, 16) + ")";
            return null;
        }
    }

    @Override public String getFareString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mFare / 100.0);
    }

    @Override public double getFare () {
        return mFare;
    }

    @Override public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.0);
    }

    @Override public Station getStartStation() {
        if (mAgency == ClipperData.AGENCY_BART) {
            if (ClipperData.BART_STATIONS.containsKey(mFrom)) {
                return ClipperData.BART_STATIONS.get(mFrom);
            }
        } else if (mAgency == ClipperData.AGENCY_FERRY) {
            if (ClipperData.FERRY_TERMINALS.containsKey(mFrom)) {
                return ClipperData.FERRY_TERMINALS.get(mFrom);
            }
        }
        return null;
    }

    @Override public Station getEndStation() {
        if (mAgency == ClipperData.AGENCY_BART) {
            if (ClipperData.BART_STATIONS.containsKey(mTo)) {
                return ClipperData.BART_STATIONS.get(mTo);
            }
        } else if (mAgency == ClipperData.AGENCY_FERRY) {
            if (ClipperData.FERRY_TERMINALS.containsKey(mTo)) {
                return ClipperData.FERRY_TERMINALS.get(mTo);
            }
        }
        return null;
    }

    @Override public String getStartStationName () {
        if (mAgency == ClipperData.AGENCY_BART || mAgency == ClipperData.AGENCY_FERRY) {
            Station station = getStartStation();
            if (station != null)
                return station.getShortStationName();
            else
                return "Station #0x" + Long.toString(mFrom, 16);
        } else if (mAgency == ClipperData.AGENCY_MUNI) {
            return null; // Coach number is not collected
        } else if (mAgency == ClipperData.AGENCY_GGT || mAgency == ClipperData.AGENCY_CALTRAIN) {
            return "Zone #" + mFrom;
        } else {
            return "(Unknown Station)";
        }
    }

    @Override public String getEndStationName () {
        if (mAgency == ClipperData.AGENCY_BART) {
            Station station = getEndStation();
            if (station != null)
                return ClipperData.BART_STATIONS.get(mTo).getShortStationName();
            else
                return "Station #0x" + Long.toString(mTo, 16);
        } else if (mAgency == ClipperData.AGENCY_MUNI) {
            return null; // Coach number is not collected
        } else if (mAgency == ClipperData.AGENCY_GGT || mAgency == ClipperData.AGENCY_CALTRAIN ||
                   mAgency == ClipperData.AGENCY_FERRY) {
            if (mTo == 0xffff)
                return "(End of line)";
            return "Zone #" + mTo;
        } else {
            return "(Unknown Station)";
        }
    }

    @Override public Mode getMode() {
        if (mAgency == ClipperData.AGENCY_ACTRAN)
            return Mode.BUS;
        if (mAgency == ClipperData.AGENCY_BART)
            return Mode.METRO;
        if (mAgency == ClipperData.AGENCY_CALTRAIN)
            return Mode.TRAIN;
        if (mAgency == ClipperData.AGENCY_GGT)
            return Mode.BUS;
        if (mAgency == ClipperData.AGENCY_SAMTRANS)
            return Mode.BUS;
        if (mAgency == ClipperData.AGENCY_VTA)
            return Mode.BUS; // FIXME: or Mode.TRAM for light rail
        if (mAgency == ClipperData.AGENCY_MUNI)
            return Mode.BUS; // FIXME: or Mode.TRAM for "Muni Metro"
        if (mAgency == ClipperData.AGENCY_FERRY)
            return Mode.FERRY;
        return Mode.OTHER;
    }

    @Override public boolean hasTime() {
        return true;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mTimestamp);
        parcel.writeLong(mExitTimestamp);
        parcel.writeLong(mFare);
        parcel.writeLong(mAgency);
        parcel.writeLong(mFrom);
        parcel.writeLong(mTo);
        parcel.writeLong(mRoute);
        parcel.writeLong(mBalance);
    }

    public int describeContents() {
        return 0;
    }
}
