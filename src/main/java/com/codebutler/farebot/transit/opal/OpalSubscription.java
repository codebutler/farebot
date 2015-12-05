package com.codebutler.farebot.transit.opal;


import android.os.Parcel;

import com.codebutler.farebot.R;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.util.Utils;

import java.util.Date;

/**
 * Class describing auto-topup on Opal.
 *
 * Opal has no concept of subscriptions, but when auto-topup is enabled, you no longer need to
 * manually refill the card with credit.
 *
 * Dates given are not valid.
 */
public class OpalSubscription extends Subscription {

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Date getValidFrom() {
        // Start of Opal trial
        return new Date(2012 - 1900, 12 - 1, 7);
    }

    @Override
    public Date getValidTo() {
        // Maximum possible date representable on the card
        return new Date(2159 - 1900, 6 - 1, 6);
    }

    @Override
    public String getAgencyName() {
        return getShortAgencyName();
    }

    @Override
    public String getShortAgencyName() {
        return "Opal";
    }

    @Override
    public int getMachineId() {
        return 0;
    }

    @Override
    public String getSubscriptionName() {
        return Utils.localizeString(R.string.opal_automatic_top_up);
    }

    @Override
    public String getActivation() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
