/*
 * HSLTransitData.java
 *
 * Copyright (C) 2013 Eric Butler
 *
 * Authors:
 * Lauri Andler <lauri.andler@gmail.com>
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

package com.codebutler.farebot.transit.hsl;

import android.os.Parcel;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.RecordDesfireFile;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HSLTransitData extends TransitData {
    private String        mSerialNumber;
    private double        mBalance;
    private List<HSLTrip> mTrips;
    private boolean       mHasKausi;
    private long          mKausiStart;
    private long          mKausiEnd;
    private long          mKausiPrevStart;
    private long          mKausiPrevEnd;
    private long          mKausiPurchasePrice;
    private long          mKausiLastUse;
    private long          mKausiPurchase;
    private HSLRefill     mLastRefill;
    private boolean       mKausiNoData;
    private long          mArvoExit;
    private long          mArvoPurchase;
    private long          mArvoExpire;
    private long          mArvoPax;
    private long          mArvoPurchasePrice;
    private long          mArvoXfer;
    private long          mArvoDiscoGroup;
    private long          mArvoMystery1;
    private long          mArvoDuration;
    private long          mArvoRegional;
    private long          mArvoJOREExt;
    private long          mArvoVehicleNumber;
    private long          mArvoUnknown;
    private long          mArvoLineJORE;
    private long          mKausiVehicleNumber;
    private long          mKausiUnknown;
    private long          mKausiLineJORE;
    private long          mKausiJOREExt;
    private long          mArvoDirection;
    private long          mKausiDirection;

    private static final long EPOCH = 0x32C97ED0;
    private static final String[] regionNames = {"N/A", "Helsinki", "Espoo", "Vantaa", "Koko alue", "Seutu", "", "", "", "",  // 0-9
        "", "", "", "", "", "", "", "", "", "", // 10-19
        "", "", "", "", "", "", "", "", "", "", // 20-29
        "", "", "", "", "", "", "", "", "", ""}; // 30-39
/*    private static final Map<Long,String> vehicleNames =  Collections.unmodifiableMap(new HashMap<Long, String>() {{ 
        put(1L, "Metro");
        put(18L, "Bus");
        put(16L, "Tram");
    }});*/

    public static boolean check(Card card) {
        return (card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x1120ef) != null);
    }

    public static TransitIdentity parseTransitIdentity(Card card) {
        try {
            byte[] data = ((DesfireCard) card).getApplication(0x1120ef).getFile(0x08).getData();
            return new TransitIdentity("HSL", Utils.getHexString(data).substring(2, 20));
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL serial", ex);
        }
    }

    public HSLTransitData(Parcel parcel) {
        mSerialNumber       = parcel.readString();
        mBalance            = parcel.readDouble();
        mArvoMystery1       = parcel.readLong();
        mArvoDuration       = parcel.readLong();
        mArvoRegional       = parcel.readLong();
        mArvoExit           = parcel.readLong();
        mArvoPurchasePrice  = parcel.readLong();
        mArvoDiscoGroup     = parcel.readLong();
        mArvoPurchase       = parcel.readLong();
        mArvoExpire         = parcel.readLong();
        mArvoPax            = parcel.readLong();
        mArvoXfer           = parcel.readLong();
        mArvoVehicleNumber  = parcel.readLong();
        mArvoUnknown        = parcel.readLong();
        mArvoLineJORE       = parcel.readLong();
        mArvoJOREExt        = parcel.readLong();
        mArvoDirection      = parcel.readLong();
        mKausiVehicleNumber = parcel.readLong();
        mKausiUnknown       = parcel.readLong();
        mKausiLineJORE      = parcel.readLong();
        mKausiJOREExt       = parcel.readLong();
        mKausiDirection     = parcel.readLong();

        mTrips = new ArrayList<>();
        parcel.readTypedList(mTrips, HSLTrip.CREATOR);
    }

    public static long bitsToLong(int start, int len, byte[] data) {
        long ret = 0;
        for (int i = start; i < start + len; ++i) {
            long bit = ((data[i / 8] >> (7 - i % 8)) & 1);
            ret = ret | (bit << ((start + len - 1) - i));
        }
        return ret;
    }

    public static long bitsToLong(int start, int len, long[] data) {
        long ret = 0;
        for (int i = start; i < start + len; ++i) {
            long bit = ((data[i / 8] >> (7 - i % 8)) & 1);
            ret = ret | (bit << ((start + len - 1) - i));
        }
        return ret;
    }

    public HSLTransitData(Card card) {
        DesfireCard desfireCard = (DesfireCard) card;

        byte[] data;

        try {
            data = desfireCard.getApplication(0x1120ef).getFile(0x08).getData();
            mSerialNumber = Utils.getHexString(data).substring(2, 20);  //Utils.byteArrayToInt(data, 1, 9);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL serial", ex);
        }

        try {
            data = desfireCard.getApplication(0x1120ef).getFile(0x02).getData();
            mBalance = bitsToLong(0, 20, data);
            mLastRefill = new HSLRefill(data);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL refills", ex);
        }

        try {
            mTrips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL trips", ex);
        }

        int balanceIndex = -1;

        for (int i = 0; i < mTrips.size(); ++i) {
            if (mTrips.get(i).mArvo == 1) {
                balanceIndex = i;
                break;
            }
        }

        try {
            data = desfireCard.getApplication(0x1120ef).getFile(0x03).getData();
            mArvoMystery1 = bitsToLong(0, 9, data);
            mArvoDiscoGroup = bitsToLong(9, 5, data);
            mArvoDuration = bitsToLong(14, 13, data);
            mArvoRegional = bitsToLong(27, 5, data);

            mArvoExit = CardDateToTimestamp(bitsToLong(32, 14, data), bitsToLong(46, 11, data));
            mArvoPurchasePrice = bitsToLong(68, 14, data);
            //mArvoDiscoGroup = bitsToLong(82, 6,data);
            mArvoPurchase = CardDateToTimestamp(bitsToLong(88, 14, data), bitsToLong(102, 11, data)); //68 price, 82 zone?
            mArvoExpire = CardDateToTimestamp(bitsToLong(113, 14, data), bitsToLong(127, 11, data)); //68 price, 82 zone?
            mArvoPax = bitsToLong(138, 6, data);

            mArvoXfer = CardDateToTimestamp(bitsToLong(144, 14, data), bitsToLong(158, 11, data)); //68 price, 82 zone?

            mArvoVehicleNumber = bitsToLong(169, 14, data);

            mArvoUnknown = bitsToLong(183, 2, data);

            mArvoLineJORE = bitsToLong(185, 14, data);
            mArvoJOREExt = bitsToLong(199, 4, data);
            mArvoDirection = bitsToLong(203, 1, data);

            if (balanceIndex > -1) {
                mTrips.get(balanceIndex).mLine = Long.toString(mArvoLineJORE);
                mTrips.get(balanceIndex).mVehicleNumber = mArvoVehicleNumber;
            } else if (mArvoPurchase > 2) {
                HSLTrip t = new HSLTrip();
                t.mArvo = 1;
                t.mExpireTimestamp = mArvoExpire;
                t.mFare = mArvoPurchasePrice;
                t.mPax = mArvoPax;
                t.mTimestamp = mArvoPurchase;
                t.mVehicleNumber = mArvoVehicleNumber;
                t.mLine = Long.toString(mArvoLineJORE);
                mTrips.add(t);
                Collections.sort(mTrips, new Trip.Comparator());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL value data", ex);
        }

        int seasonIndex = -1;
        for (int i = 0; i < mTrips.size(); ++i) {
            if (mTrips.get(i).mArvo == 0) {
                seasonIndex = i;
                break;
            }
        }

        try {
            data = desfireCard.getApplication(0x1120ef).getFile(0x01).getData();

            if (bitsToLong(19, 14, data) == 0 && bitsToLong(67, 14, data) == 0) {
                mKausiNoData = true;
            }

            mKausiStart = CardDateToTimestamp(bitsToLong(19, 14, data), 0);
            mKausiEnd = CardDateToTimestamp(bitsToLong(33, 14, data), 0);
            mKausiPrevStart = CardDateToTimestamp(bitsToLong(67, 14, data), 0);
            mKausiPrevEnd = CardDateToTimestamp(bitsToLong(81, 14, data), 0);
            if (mKausiPrevStart > mKausiStart) {
                long temp = mKausiStart;
                long temp2 = mKausiEnd;
                mKausiStart = mKausiPrevStart;
                mKausiEnd = mKausiPrevEnd;
                mKausiPrevStart = temp;
                mKausiPrevEnd = temp2;
            }
            mHasKausi = mKausiEnd > (System.currentTimeMillis() / 1000.0);
            mKausiPurchase = CardDateToTimestamp(bitsToLong(110, 14, data), bitsToLong(124, 11, data));
            mKausiPurchasePrice = bitsToLong(149, 15, data);
            mKausiLastUse = CardDateToTimestamp(bitsToLong(192, 14, data), bitsToLong(206, 11, data));
            mKausiVehicleNumber = bitsToLong(217, 14, data);
            //mTrips[0].mVehicleNumber = mArvoVehicleNumber;

            mKausiUnknown = bitsToLong(231, 2, data);

            mKausiLineJORE = bitsToLong(233, 14, data);
            //mTrips[0].mLine = Long.toString(mArvoLineJORE).substring(1);

            mKausiJOREExt = bitsToLong(247, 4, data);
            mKausiDirection = bitsToLong(241, 1, data);
            if (seasonIndex > -1) {
                mTrips.get(seasonIndex).mVehicleNumber = mKausiVehicleNumber;
                mTrips.get(seasonIndex).mLine = Long.toString(mKausiLineJORE);
            } else if (mKausiVehicleNumber > 0) {
                HSLTrip t = new HSLTrip();
                t.mArvo = 0;
                t.mExpireTimestamp = mKausiPurchase;
                t.mFare = mKausiPurchasePrice;
                t.mPax = 1;
                t.mTimestamp = mKausiPurchase;
                t.mVehicleNumber = mKausiVehicleNumber;
                t.mLine = Long.toString(mKausiLineJORE);
                mTrips.add(t);
                Collections.sort(mTrips, new Trip.Comparator());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL kausi data", ex);
        }
    }

    public static long CardDateToTimestamp(long day, long minute) {
        return (EPOCH) + day * (60 * 60 * 24) + minute * 60;
    }

    @Override public String getCardName() {
        return "HSL";
    }

    @Override public String getBalanceString() {
        FareBotApplication app = FareBotApplication.getInstance();
        String ret = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mBalance / 100);
        if (mHasKausi)
            ret += "\n" + app.getString(R.string.hsl_pass_is_valid);
        if (mArvoExpire * 1000.0 > System.currentTimeMillis())
            ret += "\n" + app.getString(R.string.hsl_value_ticket_is_valid) + "!";
        return ret;
    }

    /*
    public String getCustomString() {
        DateFormat shortDateTimeFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        DateFormat shortDateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder ret = new StringBuilder();
        if (!mKausiNoData) {
            ret.append(GR(R.string.hsl_season_ticket)).append(":\n");
            ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ").append(mKausiVehicleNumber).append("\n");
            ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ").append(Long.toString(mKausiLineJORE).substring(1)).append("\n");
            ret.append("JORE extension").append(": ").append(mKausiJOREExt).append("\n");
            ret.append("Direction").append(": ").append(mKausiDirection).append("\n");

            ret.append(GR(R.string.hsl_season_ticket_starts)).append(": ").append(shortDateFormat.format(mKausiStart * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_ends)).append(": ").append(shortDateFormat.format(mKausiEnd * 1000.0));
            ret.append("\n\n");
            ret.append(GR(R.string.hsl_season_ticket_bought_on)).append(": ").append(shortDateTimeFormat.format(mKausiPurchase * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_price_was)).append(": ").append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mKausiPurchasePrice / 100.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_you_last_used_this_ticket)).append(": ").append(shortDateTimeFormat.format(mKausiLastUse * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_previous_season_ticket)).append(": ").append(shortDateFormat.format(mKausiPrevStart * 1000.0));
            ret.append(" - ").append(shortDateFormat.format(mKausiPrevEnd * 1000.0));
            ret.append("\n\n");
        }

        ret.append(GR(R.string.hsl_value_ticket)).append(":\n");
        ret.append(GR(R.string.hsl_value_ticket_bought_on)).append(": ").append(shortDateTimeFormat.format(mArvoPurchase * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_expires_on)).append(": ").append(shortDateTimeFormat.format(mArvoExpire * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_transfer)).append(": ").append(shortDateTimeFormat.format(mArvoXfer * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_sign)).append(": ").append(shortDateTimeFormat.format(mArvoExit * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_price)).append(": ").append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mArvoPurchasePrice / 100.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_disco_group)).append(": ").append(mArvoDiscoGroup).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_pax)).append(": ").append(mArvoPax).append("\n");
        ret.append("Mystery1").append(": ").append(mArvoMystery1).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_duration)).append(": ").append(mArvoDuration).append(" min\n");
        ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ").append(mArvoVehicleNumber).append("\n");
        ret.append("Region").append(": ").append(regionNames[(int) mArvoRegional]).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ").append(Long.toString(mArvoLineJORE).substring(1)).append("\n");
        ret.append("JORE extension").append(": ").append(mArvoJOREExt).append("\n");
        ret.append("Direction").append(": ").append(mArvoDirection).append("\n");

        return ret.toString();
    }
    */

    @Override public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override public Trip[] getTrips() {
        return mTrips.toArray(new HSLTrip[mTrips.size()]);
    }

    @Override public Refill[] getRefills() {
        Refill[] ret = {mLastRefill};
        return ret;
    }

    @Override public Subscription[] getSubscriptions() {
        return null;
    }

    @Override public List<ListItem> getInfo() {
        return null;
    }

    private List<HSLTrip> parseTrips(DesfireCard card) {
        DesfireFile file = card.getApplication(0x1120ef).getFile(0x04);

        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) card.getApplication(0x1120ef).getFile(0x04);


            List<HSLTrip> useLog = new ArrayList<>();
            for (int i = 0; i < recordFile.getRecords().size(); i++) {
                useLog.add(new HSLTrip(recordFile.getRecords().get(i)));
            }
            Collections.sort(useLog, new Trip.Comparator());
            return useLog;
        }
        return new ArrayList<>();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeDouble(mBalance);

        parcel.writeLong(mArvoMystery1);
        parcel.writeLong(mArvoDuration);
        parcel.writeLong(mArvoRegional);

        parcel.writeLong(mArvoExit);
        parcel.writeLong(mArvoPurchasePrice);
        parcel.writeLong(mArvoDiscoGroup);
        parcel.writeLong(mArvoPurchase);
        parcel.writeLong(mArvoExpire);
        parcel.writeLong(mArvoPax);
        parcel.writeLong(mArvoXfer);
        parcel.writeLong(mArvoVehicleNumber);
        parcel.writeLong(mArvoUnknown);
        parcel.writeLong(mArvoLineJORE);
        parcel.writeLong(mArvoJOREExt);
        parcel.writeLong(mArvoDirection);
        parcel.writeLong(mKausiVehicleNumber);
        parcel.writeLong(mKausiUnknown);
        parcel.writeLong(mKausiLineJORE);
        parcel.writeLong(mKausiJOREExt);
        parcel.writeLong(mKausiDirection);
        if (mTrips != null) {
            parcel.writeTypedList(mTrips);
        } else {
            parcel.writeInt(0);
        }
    }

}
