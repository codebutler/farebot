/*
 * EdyTransitData.java
 *
 * Authors:
 * Chris Norden
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codebutler.farebot.transit;

import android.app.Application;
import android.os.Parcel;
import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.ListItem;
import com.codebutler.farebot.R;
import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaService;
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.Util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EdyTransitData extends TransitData {
    private EdyTrip[] mTrips;

    // defines
    public static final int FELICA_SERVICE_EDY_ID       = 0x110B;
    public static final int FELICA_SERVICE_EDY_BALANCE  = 0x1317;
    public static final int FELICA_SERVICE_EDY_HISTORY  = 0x170F;

    public static final int FELICA_MODE_EDY_DEBIT       = 0x20;
    public static final int FELICA_MODE_EDY_CHARGE      = 0x02;
    public static final int FELICA_MODE_EDY_GIFT        = 0x04;

    // private data
    private byte[]          mSerialNumber = new byte[8];
    private int             mCurrentBalance;


    public Creator<EdyTransitData> CREATOR = new Creator<EdyTransitData>() {
        public EdyTransitData createFromParcel(Parcel parcel) {
            return new EdyTransitData(parcel);
        }

        public EdyTransitData[] newArray(int size) {
            return new EdyTransitData[size];
        }
    };

    public static boolean check(FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_EDY) != null);
    }

    public static TransitIdentity parseTransitIdentity (FelicaCard card) {
        return new TransitIdentity("Edy", null);
    }

    public EdyTransitData(Parcel parcel) {
        mTrips = new EdyTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, EdyTrip.CREATOR);
    }

    public EdyTransitData(FelicaCard card) {
        // card ID is in block 0, bytes 2-9, big-endian ordering
        FelicaService serviceID = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_ID);
        FelicaBlock[] blocksID = serviceID.getBlocks();
        FelicaBlock blockID = blocksID[0];
        byte[] dataID = blockID.getData();
        for (int i=2; i<10; i++)
        {
            mSerialNumber[i-2] = dataID[i];
        }

        // current balance info in block 0, bytes 0-3, little-endian ordering
        FelicaService serviceBalance = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_BALANCE);
        FelicaBlock[] blocksBalance = serviceBalance.getBlocks();
        FelicaBlock blockBalance = blocksBalance[0];
        byte[] dataBalance = blockBalance.getData();
        mCurrentBalance = Util.toInt(dataBalance[3], dataBalance[2], dataBalance[1], dataBalance[0]);

        // now read the transaction history
        FelicaService serviceHistory = card.getSystem(FeliCaLib.SYSTEMCODE_EDY).getService(FELICA_SERVICE_EDY_HISTORY);
        List<EdyTrip> trips = new ArrayList<EdyTrip>();

        // Read blocks in order
        FelicaBlock[] blocks = serviceHistory.getBlocks();
        for (int i = 0; i < blocks.length; i++) {
            FelicaBlock block = blocks[i];
            EdyTrip trip = new EdyTrip(block);
            trips.add(trip);
        }

        mTrips = trips.toArray(new EdyTrip[trips.size()]);
    }

    @Override
    public String getBalanceString() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        format.setMaximumFractionDigits(0);
        return format.format(mCurrentBalance);
    }

    @Override
    public String getSerialNumber() {
        StringBuffer str = new StringBuffer(20);
        for (int i=0; i<8; i+=2) {
            str.append(String.format("%02X", mSerialNumber[i]));
            str.append(String.format("%02X", mSerialNumber[i+1]));
            if (i < 6)
                str.append(" ");
        }
        return str.toString();
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
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
        return "Edy";
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
    }

    public static class EdyTrip extends Trip {
        private final int mProcessType;
        private final int mSequenceNumber;
        private final Date mTimestamp;
        private final int mTransactionAmount;
        private final int mBalance;

        public EdyTrip(FelicaBlock block) {
            byte[] data = block.getData();

//          Data Offsets with values
//          ------------------------
//          0x00    type (0x20 = payment, 0x02 = charge, 0x04 = gift)
//          0x01    sequence number (3 bytes, big-endian)
//          0x04    date/time (upper 15 bits - added as day offset, lower 17 bits - added as second offset to Jan 1, 2000 00:00:00)
//          0x08    transaction amount (big-endian)
//          0x0c    balance (big-endian)


            mProcessType = data[0];
            mSequenceNumber = Util.toInt(data[1], data[2], data[3]);
            mTimestamp = extractDate(data);
            mTransactionAmount = Util.toInt(data[8], data[9], data[10], data[11]);
            mBalance = Util.toInt(data[12], data[13], data[14], data[15]);
        }
        
        public static Creator<EdyTrip> CREATOR = new Creator<EdyTrip>() {
            public EdyTrip createFromParcel(Parcel parcel) {
                return new EdyTrip(parcel);
            }

            public EdyTrip[] newArray(int size) {
                return new EdyTrip[size];
            }
        };

        public EdyTrip (Parcel parcel) {
            mProcessType = parcel.readInt();
            mSequenceNumber = parcel.readInt();
            mTimestamp  = new Date(parcel.readLong());
            mTransactionAmount = parcel.readInt();
            mBalance = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(mProcessType);
            parcel.writeInt(mSequenceNumber);
            parcel.writeLong(mTimestamp.getTime());
            parcel.writeInt(mTransactionAmount);
            parcel.writeInt(mBalance);
        }

        public Mode getMode() {
            if (mProcessType == FELICA_MODE_EDY_DEBIT) {
                return Mode.POS;
            } else if (mProcessType == FELICA_MODE_EDY_CHARGE) {
                return Mode.TICKET_MACHINE;
            } else if (mProcessType == FELICA_MODE_EDY_GIFT) {
                return Mode.VENDING_MACHINE;
            } else {
                return Mode.OTHER;
            }
        }

        public long getTimestamp() {
            if (mTimestamp != null)
                return mTimestamp.getTime() / 1000;
            else
                return 0;
        }

        public double getFare () {
            return mTransactionAmount;
        }

        public String getFareString () {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
            format.setMaximumFractionDigits(0);
            if (mProcessType != FELICA_MODE_EDY_DEBIT)
                return "+" + format.format(mTransactionAmount);
            return format.format(mTransactionAmount);
        }

        public String getBalanceString () {
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
            format.setMaximumFractionDigits(0);
            return format.format(mBalance);
        }

        // use agency name for the tranaction number
        public String getShortAgencyName () {
            return getAgencyName();
        }
        public String getAgencyName () {
            NumberFormat format = NumberFormat.getIntegerInstance();
            format.setMinimumIntegerDigits(8);
            format.setGroupingUsed(false);
            Application app = FareBotApplication.getInstance();
            String str;
            if (mProcessType != FELICA_MODE_EDY_DEBIT)
                str = app.getString(R.string.felica_process_charge);
            else
                str = app.getString(R.string.felica_process_merchandise_purchase);
            str += " " + app.getString(R.string.transaction_sequence) + format.format(mSequenceNumber);
            return str;
        }

        public boolean hasTime() {
            if (mTimestamp != null)
                return true;
            else
                return false;
        }

        // unused
        public String getRouteName () {
            return null;
        }
        public String getStartStationName () {
            return null;
        }
        public Station getStartStation () {
            return null;
        }
        public String getEndStationName () {
            return null;
        }
        public Station getEndStation () {
            return null;
        }
        public int describeContents() {
            return 0;
        }
        public long getExitTimestamp() {
            return 0;
        }
    }

    private static Date extractDate(byte[] data) {
        int fulloffset = Util.toInt(data[4], data[5], data[6], data[7]);
        if (fulloffset == 0)
            return null;

        int dateoffset = fulloffset >>> 17;
        int timeoffset = fulloffset & 0x1ffff;

        Calendar c = Calendar.getInstance();
        c.set(2000, 0, 1, 0, 0, 0);
        c.add(Calendar.DATE, dateoffset);
        c.add(Calendar.SECOND, timeoffset);

        return c.getTime();
    }
}

