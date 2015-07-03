package com.codebutler.farebot.transit.opal;

import android.os.Parcel;
import android.text.format.DateFormat;
import android.util.Log;

import com.codebutler.farebot.FareBotApplication;
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
    private double mBalance; // cents
    private int    mChecksum;
    private int    mWeeklyTrips;
    private boolean mAutoRecharge;
    private int    mActionType;
    private int    mTransitMode;
    private int    mMinute;
    private int    mDay;
    private int    mTransactionNumber;
    private int    mLastDigit;

    private static GregorianCalendar OPAL_EPOCH = new GregorianCalendar(1980, Calendar.JANUARY, 1);

    public static boolean check (Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x314553) != null);
    }

    public OpalTransitData (Parcel parcel) {
        mSerialNumber = parcel.readInt();
        mBalance      = parcel.readDouble();
        mChecksum     = parcel.readInt();
        mWeeklyTrips  = parcel.readInt();
        mAutoRecharge = parcel.readByte() == 0x01;
        mActionType   = parcel.readInt();
        mTransitMode  = parcel.readInt();
        mMinute       = parcel.readInt();
        mDay          = parcel.readInt();
        mTransactionNumber = parcel.readInt();
        mLastDigit    = parcel.readInt();
    }

    public OpalTransitData (Card card) {
        DesfireCard desfireCard = (DesfireCard) card;
        byte[] data = desfireCard.getApplication(0x314553).getFile(0x07).getData();
        int iRawBalance;
        //Log.d("OpalRaw", Utils.getHexString(data));

        // All data on the card is actually reversed
        data = Utils.reverseBuffer(data, 0, 16);
        //Log.d("OpalRev", Utils.getHexString(data));

        try {
            mChecksum = Utils.getBitsFromBuffer(data, 0, 16);
            mWeeklyTrips = Utils.getBitsFromBuffer(data, 16, 4);
            mAutoRecharge = Utils.getBitsFromBuffer(data, 20, 1) == 0x01;
            mActionType = Utils.getBitsFromBuffer(data, 21, 4);
            mTransitMode = Utils.getBitsFromBuffer(data, 25, 3);
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

        // TODO: handle negative
        mBalance = iRawBalance;
    }

    @Override public String getCardName () {
        return "Opal";
    }


    @Override public String getBalanceString () {
        return NumberFormat.getCurrencyInstance(Locale.US).format(mBalance / 100);
    }

    @Override public String getSerialNumber () {
        return String.format("308522%09d%01d", new Object[] { mSerialNumber, mLastDigit });
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

        items.add(new HeaderListItem("Card Information"));
        items.add(new ListItem("Card Number", getSerialNumber()));
        items.add(new ListItem("Balance", getBalanceString()));
        items.add(new ListItem("Weekly Trips", Integer.toString(mWeeklyTrips)));
        items.add(new ListItem("Automatic Recharge", mAutoRecharge ? "Enabled" : "Disabled"));
        items.add(new ListItem("Data Checksum", Integer.toString(mChecksum)));

        items.add(new HeaderListItem("Last Transaction"));
        items.add(new ListItem("Number", Integer.toString(mTransactionNumber)));
        Date cLastTransactionTime = getLastTransactionTime().getTime();
        items.add(new ListItem("Date", DateFormat.getLongDateFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));
        items.add(new ListItem("Time", DateFormat.getTimeFormat(FareBotApplication.getInstance()).format(cLastTransactionTime)));
        items.add(new ListItem("Mode", Integer.toString(mTransitMode)));
        items.add(new ListItem("Action Type", Integer.toString(mActionType)));

        return items;
    }

    public static TransitIdentity parseTransitIdentity (Card card) {
        // TODO: Make this not parse the whole card first.
        OpalTransitData data = new OpalTransitData(card);
        return new TransitIdentity("Opal", data.getSerialNumber());
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mSerialNumber);
        parcel.writeDouble(mBalance);
        parcel.writeInt(mChecksum);
        parcel.writeInt(mWeeklyTrips);
        parcel.writeByte((byte) (mAutoRecharge ? 0x01 : 0x00));
        parcel.writeInt(mActionType);
        parcel.writeInt(mTransitMode);
        parcel.writeInt(mMinute);
        parcel.writeInt(mDay);
        parcel.writeInt(mTransactionNumber);
        parcel.writeInt(mLastDigit);
    }

    // Unsupported elements
    @Override public Refill[] getRefills () { return null; }
    @Override public Trip[] getTrips () {
        return null;
    }
    @Override public Subscription[] getSubscriptions() {
        return null;
    }


}
