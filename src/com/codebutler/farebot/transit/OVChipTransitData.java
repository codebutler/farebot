/*
 * OVChipTransitData.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.transit;

import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMNS_STATIONDATA;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMN_ROW_CITY;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMN_ROW_COMPANY;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMN_ROW_LAT;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMN_ROW_LON;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMN_ROW_NAME;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.COLUMN_ROW_OVCID;
import static com.codebutler.farebot.ovchip.OVChipDBUtil.TABLE_NAME;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.util.Log;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.ovchip.OVChipCard;
import com.codebutler.farebot.ovchip.OVChipSubscription;
import com.codebutler.farebot.ovchip.OVChipTransaction;

public class OVChipTransitData extends TransitData {
	private OVChipTrip[] mTrips;
	private OVChipSubscriptions[] mSubscriptions;
	private int mBalance;

    private static final int  PROCESS_PURCHASE  =  0x00;
    private static final int  PROCESS_CHECKIN   =  0x01;
    private static final int  PROCESS_CHECKOUT  =  0x02;
    private static final int  PROCESS_TRANSFER  =  0x06;
    private static final int  PROCESS_BANNED    =  0x07;
    private static final int  PROCESS_CREDIT    = -0x02;
    private static final int  PROCESS_NODATA    = -0x03;

    private static final int  AGENCY_TLS        = 0x00;
    private static final int  AGENCY_CONNEXXION = 0x01;
    private static final int  AGENCY_GVB        = 0x02;
    private static final int  AGENCY_HTM        = 0x03;
    private static final int  AGENCY_NS         = 0x04;
    private static final int  AGENCY_RET        = 0x05;
    private static final int  AGENCY_VEOLIA     = 0x07;
    private static final int  AGENCY_ARRIVA     = 0x08;
    private static final int  AGENCY_SYNTUS     = 0x09;
    private static final int  AGENCY_QBUZZ      = 0x0A;
    private static final int  AGENCY_DUO        = 0x0C;	// Could also be 2C though... ( http://www.ov-chipkaart.me/forum/viewtopic.php?f=10&t=299 )
    private static final int  AGENCY_STORE      = 0x19;

    private static Map<Integer, String> sAgencies = new HashMap<Integer, String>() {
        {
            put(AGENCY_TLS, "Trans Link Systems");
            put(AGENCY_CONNEXXION, "Connexxion");
            put(AGENCY_GVB, "Gemeentelijk Vervoersbedrijf");
            put(AGENCY_HTM, "Haagsche Tramweg-Maatschappij");
            put(AGENCY_NS, "Nederlandse Spoorwegen");
            put(AGENCY_RET, "Rotterdamse Elektrische Tram");
            put(AGENCY_VEOLIA, "Veolia");
            put(AGENCY_ARRIVA, "Arriva");
            put(AGENCY_SYNTUS, "Syntus");
            put(AGENCY_QBUZZ, "Qbuzz");
            put(AGENCY_DUO, "Dienst Uitvoering Onderwijs");
            put(AGENCY_STORE, "Reseller");
        }
    };

    private static Map<Integer, String> sShortAgencies = new HashMap<Integer, String>() {
        {
        	put(AGENCY_TLS, "TLS");
            put(AGENCY_CONNEXXION, "Connexxion");	/* or Breng, Hermes, GVU */
            put(AGENCY_GVB, "GVB");
            put(AGENCY_HTM, "HTM");
            put(AGENCY_NS, "NS");
            put(AGENCY_RET, "RET");
            put(AGENCY_VEOLIA, "Veolia");
            put(AGENCY_ARRIVA, "Arriva");			/* or Aquabus */
            put(AGENCY_SYNTUS, "Syntus");
            put(AGENCY_QBUZZ, "Qbuzz");
            put(AGENCY_DUO, "DUO");
            put(AGENCY_STORE, "Reseller");			/* used by Albert Heijn, Primera and Hermes busses and maybe even more */
        }
    };

	public Creator<OVChipTransitData> CREATOR = new Creator<OVChipTransitData>() {
        public OVChipTransitData createFromParcel(Parcel parcel) {
            return new OVChipTransitData(parcel);
        }

        public OVChipTransitData[] newArray(int size) {
            return new OVChipTransitData[size];
        }
    };

    public static boolean check(Card card) {
    	return (card instanceof OVChipCard);
    }

    public static TransitIdentity parseTransitIdentity (OVChipCard card) {
    	return new TransitIdentity("OV-chipkaart", null);
    }

    public OVChipTransitData(Parcel parcel) {
        mTrips = new OVChipTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, OVChipTrip.CREATOR);
        mSubscriptions = new OVChipSubscriptions[parcel.readInt()];
        parcel.readTypedArray(mSubscriptions, OVChipSubscriptions.CREATOR);
        mBalance = parcel.readInt();
    }
	
    public OVChipTransitData(OVChipCard card) {
    	if (card.getComplete())
    	{
	    	mBalance = card.getOVChipCredit().getCredit();
	
	    	List<OVChipTransaction> alltransactions = new ArrayList<OVChipTransaction>(Arrays.asList(card.getOVChipTransactions()));
	    	Collections.sort(alltransactions, ID_ORDER);
	
	    	List<OVChipTransaction> transactions = new ArrayList<OVChipTransaction>();
	    	List<OVChipTrip> trips = new ArrayList<OVChipTrip>();
	    	List<OVChipSubscriptions> subs = new ArrayList<OVChipSubscriptions>();
	
	    	// Sort the transactions and discard the duplicates (could use a much needed rewrite...)
	    	for (int i = 0; i < alltransactions.size(); i++)
	    	{
	    		OVChipTransaction transaction = alltransactions.get(i);
	    		OVChipTransaction prevTransaction = null;
	    		
	    		if (transaction.getValid() != 1 || transaction.getTransfer() == PROCESS_NODATA)
	    			continue;
	    		
	    		if (i != 0)
	    		{
	    			prevTransaction = alltransactions.get(i - 1);
	    			
	    			if (transaction.getId() == prevTransaction.getId())
	    				continue;
	    		}
	    		
	    		transactions.add(transaction);
	    	}

	    	for (int i = 0; i < transactions.size(); i++)
	    	{
	    		OVChipTransaction transaction = transactions.get(i);
	    		OVChipTransaction prevTransaction = null;
	    		OVChipTransaction nextTransaction = null;
	
	    		if (i != 0)
	    			prevTransaction = transactions.get(i - 1);
	    		
	    		if (i < transactions.size() - 1)
					nextTransaction = transactions.get(i + 1);
	
	    		OVChipTrip trip = new OVChipTrip(transaction, prevTransaction, nextTransaction);
	    		
	    		if (trip != null && trip.mSame == false)
	    			trips.add(trip);
	    	}

	    	OVChipSubscription[] subscriptions = card.getOVChipSubscriptions();
	    	for (int i = 0; i < subscriptions.length; i++) {
	    		OVChipSubscriptions sub = new OVChipSubscriptions(subscriptions[i]);
	    		
	    		if (sub != null)
	    			subs.add(sub);
	    	}

	    	mTrips = trips.toArray(new OVChipTrip[trips.size()]);
	    	mSubscriptions = subs.toArray(new OVChipSubscriptions[subs.size()]);
    	} else {
    		mTrips = null;
	    	mSubscriptions = null;
    	}
    }

    private static final Comparator<OVChipTransaction> ID_ORDER = new Comparator<OVChipTransaction>() {
		public int compare(OVChipTransaction t1, OVChipTransaction t2) {
			return (t1.getId() < t2.getId() ? -1 : (t1.getId() == t2.getId() ? 0 : 1));
		}
	};

	public static Date convertDate(int date) {
    	return convertDate(date, 0);
    }

    public static Date convertDate(int date, int time) {
        if (date == 0)
            return null;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1997);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, time / 60);
        calendar.set(Calendar.MINUTE, time % 60);
        
        calendar.add(Calendar.DATE, date);

        return calendar.getTime();
    }

    public static String convertAmount(int amount) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		formatter.setCurrency(Currency.getInstance("EUR"));

		return formatter.format((double)amount / 100.0);
	}

	@Override
    public String getCardName () {
        return "OV-Chipkaart";
    }

	public static String getAgencyName(int agency)
    {
        if (sAgencies.containsKey(agency)) {
            return sAgencies.get(agency);
        }
        return "Unknown Agency (0x" + Long.toString(agency, 16) + ")";
    }

    public static String getShortAgencyName (int agency) {
        if (sShortAgencies.containsKey(agency)) {
            return sShortAgencies.get(agency);
        }
        return "UNK(0x" + Long.toString(agency, 16) + ")";
    }

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(mTrips.length);
        parcel.writeTypedArray(mTrips, flags);
        parcel.writeInt(mSubscriptions.length);
        parcel.writeTypedArray(mSubscriptions, flags);
        parcel.writeInt(mBalance);
	}

	@Override
	public String getBalanceString() {
		return OVChipTransitData.convertAmount((int)mBalance);
	}

	@Override
	public String getSerialNumber() {
		return null;
	}

	@Override
	public Trip[] getTrips() {
		return mTrips;
	}

	@Override
	public Refill[] getRefills() {
		return null;
	}

	public Subscription[] getSubscriptions() {
		return mSubscriptions;
	}

	public static class OVChipTrip extends Trip {
		private boolean mSame;
        private final int mProcessType;
        private final int mAgency;
        private final boolean mIsBus;
        private final boolean mIsTrain;
        private final boolean mIsMetro;
        private final boolean mIsFerry;
        private final boolean mIsOther;
        private final boolean mIsCharge;
        private final boolean mIsPurchase;
        private final boolean mIsBanned;
        private final Date mTimestamp;
        private long mFare;
        private Date mExitTimestamp;
        private Station mStartStation;
        private Station mEndStation;
        private String mStartStationName;
        private String mEndStationName;

        public OVChipTrip(OVChipTransaction ovchipTransaction, OVChipTransaction prevTransaction, OVChipTransaction nextTransaction) {
        	boolean hasStation = false;
        	mSame = false;
        	mExitTimestamp = null;
        	mProcessType = ovchipTransaction.getTransfer();
        	mAgency = ovchipTransaction.getCompany();

        	int station = ovchipTransaction.getStation();

        	mIsTrain = mAgency == AGENCY_NS || (mAgency == AGENCY_ARRIVA && station < 800);
        	mIsMetro = (mAgency == AGENCY_GVB && station < 3000) || (mAgency == AGENCY_RET && station < 3000);	// TODO: Needs verification!
        	mIsOther = mAgency == AGENCY_TLS || mAgency == AGENCY_DUO || mAgency == AGENCY_STORE;
        	mIsFerry = mAgency == AGENCY_ARRIVA && (station > 4600 && station < 4700);	// TODO: Needs verification!

        	//mIsBusOrTram = (mAgency == AGENCY_GVB || mAgency == AGENCY_HTM || mAgency == AGENCY_RET && (!mIsMetro));
            //mIsBusOrTrain = mAgency == AGENCY_VEOLIA || mAgency == AGENCY_SYNTUS;

        	/* 
        	 * Everything else will be a bus, although this is not correct.
        	 * The only way to determine them would be to collect every single 'ovcid' out there :(
        	 */
        	mIsBus = (!mIsTrain && !mIsMetro && !mIsOther && !mIsFerry);

        	mIsCharge = mProcessType == PROCESS_CREDIT || mProcessType == PROCESS_TRANSFER;
        	mIsPurchase = mProcessType == PROCESS_PURCHASE;

        	mIsBanned = mProcessType == PROCESS_BANNED; // TODO: Needs icon, could use: http://thenounproject.com/noun/no-entry/#icon-No42

        	mTimestamp = convertDate(ovchipTransaction.getDate(), ovchipTransaction.getTime());
        	mFare = ovchipTransaction.getAmount();

        	if (nextTransaction != null) {
	        	if (mAgency == nextTransaction.getCompany() && mProcessType == PROCESS_CHECKIN && nextTransaction.getTransfer() == PROCESS_CHECKOUT ) {
	        		if (isSameTrip(ovchipTransaction.getDate(), nextTransaction.getDate(), ovchipTransaction.getTime(), nextTransaction.getTime(), mAgency)) {
		        		mStartStation = getStation(mAgency, station);
		        		mEndStation = getStation(mAgency, nextTransaction.getStation());
		        		mExitTimestamp = convertDate(nextTransaction.getDate(), nextTransaction.getTime());

		        		mFare = nextTransaction.getAmount();

		        		hasStation = true;
	        		}
	        	}
        	}

        	if (prevTransaction != null && hasStation != true) {
        		if (mAgency == prevTransaction.getCompany() && mProcessType == PROCESS_CHECKOUT && prevTransaction.getTransfer() == PROCESS_CHECKIN ) {
	        		if (isSameTrip(prevTransaction.getDate(), ovchipTransaction.getDate(), prevTransaction.getTime(), ovchipTransaction.getTime(), mAgency)) {
	        			mSame = true;

		        		return;
	        		}
        		}
        	} 

        	if (hasStation != true)
        		mStartStation = getStation(mAgency, station);

        	if (mStartStation != null)
        		mStartStationName = mStartStation.getStationName();

        	if (mEndStation != null)
        		mEndStationName = mEndStation.getStationName();
        }

        private boolean isSameTrip(int date, int nextDate, int time, int nextTime, int company) {
        	/* 
        	 * Information about checking in and out: 
        	 * http://www.chipinfo.nl/inchecken/
        	 */

        	if (date == nextDate)
    			return true;

        	if (date == nextDate + 1)
        	{
        		// All NS trips get reset at 4 AM (except if it's a night train, but that's out of our scope).
        		if (company == AGENCY_NS && nextTime < 240)
        			return true;

        		/*
        		 * Some companies expect a checkout at the maximum of 15 minutes after the estimated arrival at the endstation of the line.
        		 * But it's hard to determine the length of every single trip there is, so for now let's just assume a checkout at the next
        		 * day is still from the same trip. Better solutions are always welcome ;) 
        		 */
        		if (company != AGENCY_NS)
        			return true;
        	}

        	return false;
        }

        public static Creator<OVChipTrip> CREATOR = new Creator<OVChipTrip>() {
            public OVChipTrip createFromParcel(Parcel parcel) {
                return new OVChipTrip(parcel);
            }

            public OVChipTrip[] newArray(int size) {
                return new OVChipTrip[size];
            }
        };

        public OVChipTrip (Parcel parcel) {
        	mSame = (parcel.readInt() == 1);

            mProcessType = parcel.readInt();
            mAgency = parcel.readInt();

            mIsBus = (parcel.readInt() == 1);
            mIsTrain = (parcel.readInt() == 1);
            mIsMetro = (parcel.readInt() == 1);
            mIsFerry = (parcel.readInt() == 1);
            mIsOther = (parcel.readInt() == 1);
            mIsCharge = (parcel.readInt() == 1);
            mIsPurchase = (parcel.readInt() == 1);
            mIsBanned = (parcel.readInt() == 1);

            mFare = parcel.readLong();
            mTimestamp = new Date(parcel.readLong());

            if (parcel.readInt() == 1)
            	mExitTimestamp = new Date(parcel.readLong());

            if (parcel.readInt() == 1)
                mStartStation = parcel.readParcelable(Station.class.getClassLoader());

            if (parcel.readInt() == 1)
            	mEndStation = parcel.readParcelable(Station.class.getClassLoader());

            mStartStationName = parcel.readString(); 
            mEndStationName = parcel.readString(); 
        }

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			parcel.writeInt(mSame ? 1 : 0);

			parcel.writeInt(mProcessType);
			parcel.writeInt(mAgency);

			parcel.writeInt(mIsBus ? 1 : 0);
			parcel.writeInt(mIsTrain ? 1 : 0);
			parcel.writeInt(mIsMetro ? 1 : 0);
			parcel.writeInt(mIsFerry ? 1 : 0);
			parcel.writeInt(mIsOther ? 1 : 0);
			parcel.writeInt(mIsCharge ? 1 : 0);
			parcel.writeInt(mIsPurchase ? 1 : 0);
			parcel.writeInt(mIsBanned ? 1 : 0);

			parcel.writeLong(mFare);
            parcel.writeLong(mTimestamp.getTime());

            if (mExitTimestamp != null) {
                parcel.writeInt(1);
                parcel.writeLong(mExitTimestamp.getTime());
            } else {
                parcel.writeInt(0);
            }

            if (mStartStation != null) {
                parcel.writeInt(1);
                parcel.writeParcelable(mStartStation, flags);
            } else {
                parcel.writeInt(0);
            }

            if (mEndStation != null) {
                parcel.writeInt(1);
                parcel.writeParcelable(mEndStation, flags);
            } else {
                parcel.writeInt(0);
            }

            parcel.writeString(mStartStationName);
            parcel.writeString(mEndStationName);
		}

		@Override
		public String getRouteName() {
			return null;
		}

		@Override
		public String getAgencyName() {
			return OVChipTransitData.getShortAgencyName((int)mAgency);	// Nobody uses most of the long names
		}

		@Override
		public String getShortAgencyName() {
			return OVChipTransitData.getShortAgencyName((int)mAgency);
		}

		@Override
		public String getBalanceString() {
			return null;
		}

		@Override
		public String getStartStationName() {
			if (mStartStationName != null)
				return mStartStationName;
			else
				return "Unknown";
		}

		@Override
        public Station getStartStation() {
            return mStartStation;
        }

		@Override
		public String getEndStationName() {
			if (mEndStationName != null)
				return mEndStationName;
			else if (mEndStation != null)
				return "Unknown";
			else 
				return null;
		}

		@Override
		public Station getEndStation() {
			return mEndStation;
		}

		@Override
		public Mode getMode() {
			if (mIsTrain) {
                return Mode.TRAIN;
            } else if (mIsBus) {
                return Mode.BUS;
            } else if (mIsMetro) {
                return Mode.METRO;
            } else if (mIsFerry) {
                return Mode.FERRY;
            } else if (mIsPurchase) {
                return Mode.VENDING_MACHINE;
            } else if (mIsOther) {
            	return Mode.OTHER;
            } else {
            	return Mode.OTHER;
            }
		}

		@Override
		public long getTimestamp() {
			if (mTimestamp != null)
                return mTimestamp.getTime() / 1000;
            else
                return 0;
		}

		public long getExitTimestamp() {
			if (mExitTimestamp != null)
                return mExitTimestamp.getTime() / 1000;
            else
                return 0;
		}

		public boolean hasTime() {
            return (mTimestamp != null);
        }

		@Override
		public double getFare() {
			return mFare;
		}

		@Override
		public String getFareString() {
			return OVChipTransitData.convertAmount((int)mFare);
		}

		private static Station getStation(int companyCode, int stationCode) {
	        try {
	            SQLiteDatabase db = FareBotApplication.getInstance().getOVChipDBUtil().openDatabase();
	            Cursor cursor = db.query(
	            		TABLE_NAME,
	            		COLUMNS_STATIONDATA,
	                 String.format("%s = ? AND %s = ?", COLUMN_ROW_COMPANY, COLUMN_ROW_OVCID),
	                 new String[] {
	                     String.valueOf(companyCode),
	                     String.valueOf(stationCode)
	                 },
	                 null,
	                 null,
	                 COLUMN_ROW_OVCID);

	            if (!cursor.moveToFirst()) {
	                Log.w("OVChipTransitData", String.format("FAILED get rail company: c: 0x%s s: 0x%s",
	                    Integer.toHexString(companyCode),
	                    Integer.toHexString(stationCode)));

	                return null;
	            }

	            String cityName    = cursor.getString(cursor.getColumnIndex(COLUMN_ROW_CITY));
	            String stationName = cursor.getString(cursor.getColumnIndex(COLUMN_ROW_NAME));
	            String latitude    = cursor.getString(cursor.getColumnIndex(COLUMN_ROW_LAT));
	            String longitude   = cursor.getString(cursor.getColumnIndex(COLUMN_ROW_LON));

	            if (cityName != null)
	            	stationName = cityName.concat(", " + stationName);

	            return new Station(stationName, latitude, longitude);
	        } catch (Exception e) {
	            Log.e("OVChipStationProvider", "Error in getStation", e);
	            return null;
	        }
	    }
	}

	public static class OVChipSubscriptions extends Subscription {
        private final int mId;
        private final long mValidFrom;
        private final long mValidTo;
        private final int mAgency;
        private final int mMachineId;
        private final int mSubscription;

		private static Map<Integer, String> sSubscriptions = new HashMap<Integer, String>() {
			/* It seems that all the IDs are unique, so why bother with the companies? */
	        {
	        	/* NS */
	        	put(0x0005, "OV-jaarkaart");
	        	put(0x0007, "OV-Bijkaart 1e klas");
	        	put(0x0011, "NS Businesscard");
	        	put(0x0019, "Voordeelurenabonnement (twee jaar)");
	        	put(0x00AF, "Studenten OV-chipkaart week (2009)");
	        	put(0x00B0, "Studenten OV-chipkaart weekend (2009)");
	        	put(0x00B1, "Studentenkaart korting week (2009)");
	        	put(0x00B2, "Studentenkaart korting weekend (2009)");
	        	put(0x00C9, "Reizen op saldo bij NS, 1e klasse");
	        	put(0x00CA, "Reizen op saldo bij NS, 2de klasse");
	        	put(0x00CE, "Voordeelurenabonnement reizen op saldo");
	        	put(0x00E5, "Reizen op saldo (tijdelijk eerste klas)");
	        	put(0x00E6, "Reizen op saldo (tijdelijk tweede klas)");
	        	put(0x00E7, "Reizen op saldo (tijdelijk eerste klas korting)");
	        	/* Arriva */
	        	put(0x059A, "Dalkorting");
	        	/* Veolia */
	        	put(0x0626, "DALU Dalkorting");
	        	/* Connexxion */
	        	put(0x0692, "Daluren Oost-Nederland");
	        	put(0x069C, "Daluren Oost-Nederland");
	        	/* DUO */
	        	put(0x09C6, "Student weekend-vrij");
	        	put(0x09C7, "Student week-korting");
	        	put(0x09C9, "Student week-vrij");
	        	put(0x09CA, "Student weekend-korting");
	        	/* GVB */
	        	put(0x0BBD, "Fietssupplement");
	        }
	    };

        public static Creator<OVChipSubscriptions> CREATOR = new Creator<OVChipSubscriptions>() {
            public OVChipSubscriptions createFromParcel(Parcel parcel) {
                return new OVChipSubscriptions(parcel);
            }

            public OVChipSubscriptions[] newArray(int size) {
                return new OVChipSubscriptions[size];
            }
        };

        public OVChipSubscriptions (OVChipSubscription subscription)
        {
        	Date validFrom = OVChipTransitData.convertDate(subscription.getValidFrom());
        	Date validTo = OVChipTransitData.convertDate(subscription.getValidTo());

            mId = subscription.getId();
            mValidFrom = validFrom != null ? validFrom.getTime() : 0;
            mValidTo = validTo != null ? validTo.getTime() : 0;
            mAgency = subscription.getCompany();
            mMachineId = subscription.getMachineId();
            mSubscription = subscription.getSubscription();
        }

        public OVChipSubscriptions(Parcel parcel) {
        	mId = parcel.readInt();
        	mValidFrom = parcel.readLong();
        	mValidTo = parcel.readLong();
        	mAgency = parcel.readInt();
        	mMachineId = parcel.readInt();
        	mSubscription = parcel.readInt();
        }

        public static String getSubscriptionName(int subscription)
        {
            if (sSubscriptions.containsKey(subscription)) {
                return sSubscriptions.get(subscription);
            }
            return "Unknown Subscription (0x" + Long.toString(subscription, 16) + ")";
        }

		@Override
		public int getId() {
			return mId;
		}

		@Override
		public long getValidFrom() {
			return mValidFrom;
		}

		@Override
		public long getValidTo() {
			return mValidTo;
		}

		@Override
		public String getSubscriptionName() {
			return getSubscriptionName(mSubscription);
		}

		@Override
        public int getMachineId () {
            return mMachineId;
        }

		@Override
        public String getAgencyName () {
            return OVChipTransitData.getShortAgencyName((int)mAgency);	// Nobody uses most of the long names
        }

		@Override
        public String getShortAgencyName () {
            return OVChipTransitData.getShortAgencyName((int)mAgency);
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(mId);
        	parcel.writeLong(mValidFrom);
        	parcel.writeLong(mValidTo);
        	parcel.writeInt(mAgency);
        	parcel.writeInt(mMachineId);
        	parcel.writeInt(mSubscription);
        }
    }
}