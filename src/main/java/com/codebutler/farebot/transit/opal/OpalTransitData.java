package com.codebutler.farebot.transit.opal;

import android.os.Parcel;
import android.text.format.DateFormat;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Opal (Sydney, AU).
 *
 * Documentation of format: https://github.com/codebutler/farebot/wiki/Opal
 */
public class OpalTransitData extends TransitData {
    private int    mSerialNumber;
    private int    mBalance; // cents
    private int    mChecksum;
    private int    mWeeklyTrips;
    private boolean mAutoTopup;
    private int    mActionType;
    private int    mVehicleType;
    private int    mMinute;
    private int    mDay;
    private int    mTransactionNumber;
    private int    mLastDigit;

    private static GregorianCalendar OPAL_EPOCH = new GregorianCalendar(1980, Calendar.JANUARY, 1);
    private static OpalSubscription OPAL_AUTOMATIC_TOP_UP = new OpalSubscription();

    public static boolean check (Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x314553) != null);
    }

    public OpalTransitData (Parcel parcel) {
        mSerialNumber = parcel.readInt();
        mBalance      = parcel.readInt();
        mChecksum     = parcel.readInt();
        mWeeklyTrips  = parcel.readInt();
        mAutoTopup = parcel.readByte() == 0x01;
        mActionType   = parcel.readInt();
        mVehicleType = parcel.readInt();
        mMinute       = parcel.readInt();
        mDay          = parcel.readInt();
        mTransactionNumber = parcel.readInt();
        mLastDigit    = parcel.readInt();
    }

    public OpalTransitData (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(0x314553).getFile(0x07).getData();
        int iRawBalance;

        data = Utils.reverseBuffer(data, 0, 16);

        try {
            mChecksum = Utils.getBitsFromBuffer(data, 0, 16);
            mWeeklyTrips = Utils.getBitsFromBuffer(data, 16, 4);
            mAutoTopup = Utils.getBitsFromBuffer(data, 20, 1) == 0x01;
            mActionType = Utils.getBitsFromBuffer(data, 21, 4);
            mVehicleType = Utils.getBitsFromBuffer(data, 25, 3);
            mMinute = Utils.getBitsFromBuffer(data, 28, 11);
            mDay = Utils.getBitsFromBuffer(data, 39, 15);
            iRawBalance = Utils.getBitsFromBuffer(data, 54, 21);
            mTransactionNumber = Utils.getBitsFromBuffer(data, 75, 16);
            // Skip bit here
            mLastDigit = Utils.getBitsFromBuffer(data, 92, 4);
            mSerialNumber = Utils.getBitsFromBuffer(data, 96, 32);
        } catch (Exception ex){
            throw new RuntimeException("Error parsing Opal data", ex);
        }

        mBalance = Utils.unsignedToTwoComplement(iRawBalance, 21);
    }

    @Override public String getCardName () {
        return "Opal";
    }


    @Override public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.);
    }

    @Override public String getSerialNumber () {
        return formatSerialNumber(mSerialNumber, mLastDigit);
    }

    private static String formatSerialNumber(int serialNumber, int lastDigit) {
        return String.format("308522%09d%01d", new Object[] { serialNumber, lastDigit });

    }
    public Calendar getLastTransactionTime() {
        Calendar cLastTransaction = GregorianCalendar.getInstance();
        cLastTransaction.setTimeInMillis(OPAL_EPOCH.getTimeInMillis());
        cLastTransaction.add(Calendar.DATE, mDay);
        cLastTransaction.add(Calendar.MINUTE, mMinute);
        return cLastTransaction;
    }

    @Override public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();

        items.add(new HeaderListItem(R.string.general));
        items.add(new ListItem(R.string.opal_weekly_trips, Integer.toString(mWeeklyTrips)));
        items.add(new ListItem(R.string.checksum, Integer.toString(mChecksum)));

        items.add(new HeaderListItem(R.string.last_transaction));
        items.add(new ListItem(R.string.transaction_sequence, Integer.toString(mTransactionNumber)));
        Date cLastTransactionTime = getLastTransactionTime().getTime();
        items.add(new ListItem(R.string.date, DateFormat.getLongDateFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));
        items.add(new ListItem(R.string.time, DateFormat.getTimeFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));
        items.add(new ListItem(R.string.vehicle_type, getVehicleType(mVehicleType)));
        items.add(new ListItem(R.string.transaction_type, getActionType(mActionType)));

        return items;
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(0x314553).getFile(0x07).getData();
        data = Utils.reverseBuffer(data, 0, 5);

        int lastDigit = Utils.getBitsFromBuffer(data, 4, 4);
        int serialNumber = Utils.getBitsFromBuffer(data, 8, 32);
        return new TransitIdentity("Opal", formatSerialNumber(serialNumber, lastDigit));
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerialNumber);
        parcel.writeInt(mBalance);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mWeeklyTrips);
        parcel.writeByte((byte) (mAutoTopup ? 0x01 : 0x00));
        parcel.writeInt(mActionType);
        parcel.writeInt(mVehicleType);
        parcel.writeInt(mMinute);
        parcel.writeInt(mDay);
        parcel.writeInt(mTransactionNumber);
        parcel.writeInt(mLastDigit);
    }

    @Override public Subscription[] getSubscriptions() {
        // Opal has no concept of "subscriptions" (travel pass), only automatic top up.
        if (mAutoTopup) {
            return new Subscription[] {OPAL_AUTOMATIC_TOP_UP};
        }
        return new Subscription[] {};
    }

    public static String getVehicleType(int vehicleType) {
        if (OpalData.VEHICLES.containsKey(vehicleType)) {
            return Utils.localizeString(OpalData.VEHICLES.get(vehicleType));
        }
        return Utils.localizeString(R.string.unknown_format, "0x" + Long.toString(vehicleType, 16));
    }

    public static String getActionType(int actionType) {
        if (OpalData.ACTIONS.containsKey(actionType)) {
            return Utils.localizeString(OpalData.ACTIONS.get(actionType));
        }

        return Utils.localizeString(R.string.unknown_format, "0x" + Long.toString(actionType, 16));
    }

    // Unsupported elements
    @Override public Refill[] getRefills () { return null; }
    @Override public Trip[] getTrips () {
        return null;
    }


}
