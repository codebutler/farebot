package com.codebutler.farebot.transit.orca;

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

public class MergedOrcaTrip extends Trip {
    private final OrcaTrip mStartTrip;
    private final OrcaTrip mEndTrip;

    public static Creator<MergedOrcaTrip> CREATOR = new Creator<MergedOrcaTrip>() {
        public MergedOrcaTrip createFromParcel(Parcel parcel) {
            return new MergedOrcaTrip(
                (OrcaTrip) parcel.readParcelable(OrcaTrip.class.getClassLoader()),
                (OrcaTrip) parcel.readParcelable(OrcaTrip.class.getClassLoader())
            );
        }

        public MergedOrcaTrip[] newArray(int size) {
            return new MergedOrcaTrip[size];
        }
    };

    public MergedOrcaTrip(OrcaTrip startTrip, OrcaTrip endTrip) {
        mStartTrip = startTrip;
        mEndTrip = endTrip;
    }

    @Override public long getTimestamp() {
        return mStartTrip.getTimestamp();
    }

    @Override public long getExitTimestamp() {
        return mEndTrip.getTimestamp();
    }

    @Override public String getRouteName() {
        return mStartTrip.getRouteName();
    }

    @Override public String getAgencyName() {
        return mStartTrip.getAgencyName();
    }

    @Override public String getShortAgencyName() {
        return mStartTrip.getShortAgencyName();
    }

    @Override public String getFareString() {
        if (mEndTrip.mTransType == OrcaTransitData.TRANS_TYPE_CANCEL_TRIP) {
            return FareBotApplication.getInstance().getString(R.string.fare_cancelled_format, mStartTrip.getFareString());
        }
        return mStartTrip.getFareString();
    }

    @Override public String getBalanceString() {
        return mEndTrip.getBalanceString();
    }

    @Override public String getStartStationName() {
        return mStartTrip.getStartStationName();
    }

    @Override public Station getStartStation() {
        return mStartTrip.getStartStation();
    }

    @Override public String getEndStationName() {
        return mEndTrip.getStartStationName();
    }

    @Override public Station getEndStation() {
        return mEndTrip.getStartStation();
    }

    @Override public double getFare() {
        return mStartTrip.getFare();
    }

    @Override public Mode getMode() {
        return mStartTrip.getMode();
    }

    @Override public boolean hasTime() {
        return mStartTrip.hasTime();
    }

    @Override public void writeToParcel(Parcel parcel, int flags) {
        mStartTrip.writeToParcel(parcel, flags);
        mEndTrip.writeToParcel(parcel, flags);
    }

    @Override public int describeContents() {
        return 0;
    }
}
