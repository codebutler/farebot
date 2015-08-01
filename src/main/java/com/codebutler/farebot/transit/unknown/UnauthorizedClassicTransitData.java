package com.codebutler.farebot.transit.unknown;

import android.os.Parcel;

import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.util.List;

/**
 * Handle MiFare Classic with no open sectors
 */
public class UnauthorizedClassicTransitData  extends TransitData {
    public static boolean check (Card card) {
        if (!(card instanceof ClassicCard)) {
            return false;
        }

        ClassicCard classic = (ClassicCard)card;
        // check to see if all sectors are blocked
        for (ClassicSector s : classic.getSectors()) {
            if (!(s instanceof UnauthorizedClassicSector)) {
                // At least one sector is "open", this is not for us
                return false;
            }
        }
        return true;
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        return new TransitIdentity(Utils.localizeString(R.string.locked_card), null);
    }


    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getSerialNumber() {
        return null;
    }

    @Override
    public Trip[] getTrips() {
        return null;
    }

    @Override
    public Refill[] getRefills() {
        return null;
    }

    @Override
    public Subscription[] getSubscriptions() {
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        return null;
    }

    @Override
    public String getCardName() {
        return Utils.localizeString(R.string.locked_card);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
