/**
 *
 */
package com.codebutler.farebot.transit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kazzz.felica.lib.Util;

import android.os.Parcel;

import com.codebutler.farebot.ListItem;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;

public class ItsoTransitData extends TransitData {
	private long mSerialNumber;
	private long issn;
	private byte[] directoryBytes;
	private byte[][] logs;
	private byte[] shellBytes;

	public static boolean check(Card card) {
		// Mifare Desfire
		if ((card instanceof DesfireCard)
				&& (((DesfireCard) card).getApplication(0x1602a0) != null)) {

			/* Need to check IIN is 633597
			if (Utils.getHexString(data, 2, 3) != "633597") {
			 */

			return true;
		}

		// TODO: Support Mifare classic etc. here

		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public ItsoTransitData(Parcel parcel) {
		/*
		 * mSerialNumber = parcel.readLong(); mBalance = (short)
		 * parcel.readLong();
		 *
		 * mTrips = new ClipperTrip[parcel.readInt()];
		 * parcel.readTypedArray(mTrips, ClipperTrip.CREATOR);
		 *
		 * mRefills = new ClipperRefill[parcel.readInt()];
		 * parcel.readTypedArray(mRefills, ClipperRefill.CREATOR);
		 */
	}

	public ItsoTransitData(Card card) {
		if (card instanceof DesfireCard) {
			DesfireCard desfireCard = (DesfireCard) card;

			DesfireApplication application = desfireCard
					.getApplication(0x1602a0);

			// See page 100 of
			// http://www.itso.org.uk/content/Specification/Spec_v2.1.4/ITSO_TS_1000-10_V2_1_4_2010-02.pdf
			directoryBytes = application.getFile(0x00).getData();
			logs = divideArray(application.getFile(0x01).getData(), 48);
			shellBytes = application.getFile(0xf).getData();

			// We go via hex strings because these are binary coded decimal.
			issn = Long.parseLong(Utils.getHexString(shellBytes, 5, 2));
			mSerialNumber = Long.parseLong(Utils.getHexString(shellBytes, 7, 4));

			byte[] data;
			/*
			 * try { data =
			 * desfireCard.getApplication(0x1602a0).getFile(0x08).getData();
			 * mSerialNumber = Utils.byteArrayToLong(data, 1, 4); } catch
			 * (Exception ex) { throw new
			 * RuntimeException("Error parsing Clipper serial", ex); }
			 *
			 * try { data =
			 * desfireCard.getApplication(0x9011f2).getFile(0x02).getData();
			 * mBalance = (short) (((0xFF & data[18]) << 8) | (0xFF &
			 * data[19])); } catch (Exception ex) { throw new
			 * RuntimeException("Error parsing Clipper balance", ex); }
			 *
			 * try { mTrips = parseTrips(desfireCard); } catch (Exception ex) {
			 * throw new RuntimeException("Error parsing Clipper trips", ex); }
			 *
			 * try { mRefills = parseRefills(desfireCard); } catch (Exception
			 * ex) { throw new RuntimeException("Error parsing Clipper refills",
			 * ex); }
			 */
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.codebutler.farebot.transit.TransitData#getBalanceString()
	 */
	@Override
	public String getBalanceString() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.codebutler.farebot.transit.TransitData#getSerialNumber()
	 */
	@Override
	public String getSerialNumber() {
		String stringSerialNo = String.format("%08d", mSerialNumber);
		return String.format("%04d", issn) + " " + stringSerialNo.substring(0, 4) + " " + stringSerialNo.substring(4);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.codebutler.farebot.transit.TransitData#getTrips()
	 */
	@Override
	public Trip[] getTrips() {
		// TODO Auto-generated method stub
		ItsoTrip[] trips = new ItsoTrip[logs.length];

		int tripCount = 0;

		for(byte[] logEntry : logs) {
			//Util.logEntry;
			long minutes = (0xFF & logEntry[4]) * 65536 + (0xFF & logEntry[5]) * 256 + (0xFF & logEntry[6]);
			if (minutes > 0) {
				trips[tripCount] = new ItsoTrip(minutes * 60 + 852076800L); // 852076800L is the timestamp of 1st Jan 1997
				trips[tripCount].setAgency((0xFF & logEntry[21]) * 256 + (0xFF & logEntry[22]));
				trips[tripCount].setRoute((0xFF & logEntry[23]) * 256 + (0xFF & logEntry[24]));
				tripCount++;
			}
		}
		return Arrays.copyOfRange(trips, 0, tripCount);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.codebutler.farebot.transit.TransitData#getRefills()
	 */
	@Override
	public Refill[] getRefills() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.codebutler.farebot.transit.TransitData#getCardName()
	 */
	@Override
	public String getCardName() {
		// TODO Auto-generated method stub
		return "ITSO";
	}

	public static TransitIdentity parseTransitIdentity(Card card) {
		try {

			byte[] data = ((DesfireCard) card).getApplication(0x1602a0)
					.getFile(0x0f).getData();

			return new TransitIdentity("ITSO",
					Utils.getHexString(data, 5, 2)
							+ " "
							+ Utils.getHexString(data, 7, 2)
							+ " "
							+ Utils.getHexString(data, 9, 2));
		} catch (Exception ex) {
			throw new RuntimeException("Error parsing ITSO serial", ex);
		}
	}

	@Override
	public Subscription[] getSubscriptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ListItem> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Divide an array of bytes into equal sized chunks
	 *
	 * Taken from: http://stackoverflow.com/a/3405233/3408
	 *
	 * @param source The array to divide
	 * @param chunksize Number of bytes to put in each chunk
	 * @return
	 */
	public static byte[][] divideArray(byte[] source, int chunksize) {

		byte[][] ret = new byte[(int) Math.ceil(source.length
				/ (double) chunksize)][chunksize];

		int start = 0;

		for (int i = 0; i < ret.length; i++) {
			ret[i] = Arrays.copyOfRange(source, start, start + chunksize);
			start += chunksize;
		}

		return ret;
	}

	public static class ItsoTrip extends Trip {

		private final long mTimestamp;
		private int route;
		private int agency;

		public ItsoTrip (Long startTime) {
			mTimestamp = startTime;
		}

		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			// TODO Auto-generated method stub

		}

		@Override
		public long getTimestamp() {
			return mTimestamp;
		}

		@Override
		public long getExitTimestamp() {
			// TODO Auto-generated method stub
			return 0;
		}

		public ItsoTrip setRoute(int routeID) {
			this.route = routeID;
			return this;
		}

		private static Map<Integer, String> routes = new HashMap<Integer, String>() {{
			put(0x1fff,   "3");
			put(0x27ff,   "4");
			put(0x22bf,   "4A");
			put(0x233f,   "4C");
			put(0x2fff,   "5");
			put(0x087f,  "11");
			put(0x08ff,  "13");
			put(0x28ff,  "U1");
			put(0xC07f,  "S1");
			put(0xe047, "X13");
		}};

		@Override
		public String getRouteName() {
			if (routes.containsKey(route)) {
				return routes.get(route);
			}
			return "Unknown Route: " + Util.getHexString(Util.toBytes(route));
		}

		private static Map<Integer, String> agencys = new HashMap<Integer, String>() {{
			put(0x0000, "Stagecoach");
			put(0x206c, "Oxford Bus company");
		}};

		public ItsoTrip setAgency(int agencyID) {
			this.agency = agencyID;
			return this;
		}

		@Override
		public String getAgencyName() {
			if (agencys.containsKey(agency)) {
				return agencys.get(agency);
			}
			return "Unknown operator: " + Util.getHexString(Util.toBytes(agency));
		}

		@Override
		public String getShortAgencyName() {
			return getAgencyName();
		}

		@Override
		public String getFareString() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getBalanceString() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getStartStationName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Station getStartStation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getEndStationName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Station getEndStation() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double getFare() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Mode getMode() {
			return Mode.BUS;
		}

		@Override
		public boolean hasTime() {
			// TODO Auto-generated method stub
			return false;
		}
    }

}
