/**
 *
 */
package com.codebutler.farebot.transit;

import android.os.Parcel;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.mifare.DesfireApplication;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.mifare.DesfireFile;

public class ItsoTransitData extends TransitData {
	private long mSerialNumber;

	public static boolean check(Card card) {
		// Mifare Desfire
		if ((card instanceof DesfireCard)
				&& (((DesfireCard) card).getApplication(0x1602a0) != null)) {

			/* Need to check IIN is 633597
			if (Utils.getHexString(data, 2, 3) != "633597") {
			 */



			return true;
		}

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
			byte[] directoryBytes = application.getFile(0x00).getData();
			byte[] logBytes = application.getFile(0x01).getData();
			byte[] shellBytes = application.getFile(0xf).getData();

			// ISSN = Utils.byteArrayToLong(shellBytes,6,7);

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
		return Long.toString(mSerialNumber);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.codebutler.farebot.transit.TransitData#getTrips()
	 */
	@Override
	public Trip[] getTrips() {
		// TODO Auto-generated method stub
		return null;
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

}
