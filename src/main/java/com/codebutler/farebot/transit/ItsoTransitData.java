/**
 * 
 */
package com.codebutler.farebot.transit;

import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifare.Card;
import com.codebutler.farebot.mifare.DesfireCard;
import com.codebutler.farebot.transit.ClipperTransitData.ClipperRefill;
import com.codebutler.farebot.transit.ClipperTransitData.ClipperTrip;

import android.os.Parcel;

/**
 * @author rjmunro
 *
 */
public class ITSOTransitData extends TransitData {

	
	
    public static boolean check (Card card)
    {
    	// Mifare Desfire
        if ((card instanceof DesfireCard) && (((DesfireCard) card).getApplication(0x1602a0) != null))
        	return true;

        return false;
    }

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		// TODO Auto-generated method stub

	}
	
    public ITSOTransitData(Parcel parcel) {
        mSerialNumber = parcel.readLong();
        mBalance      = (short) parcel.readLong();
                
        mTrips = new ClipperTrip[parcel.readInt()];
        parcel.readTypedArray(mTrips, ClipperTrip.CREATOR);
        
        mRefills = new ClipperRefill[parcel.readInt()];
        parcel.readTypedArray(mRefills, ClipperRefill.CREATOR);
    }

	
    public ITSOTransitData(Card card) {
    	if (card instanceof DesfireCard) {
            DesfireCard desfireCard = (DesfireCard) card;

            byte[] data;

            try {
                data = desfireCard.getApplication(0x1602a0).getFile(0x08).getData();
                mSerialNumber = Utils.byteArrayToLong(data, 1, 4);
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing Clipper serial", ex);
            }

            try {
                data = desfireCard.getApplication(0x9011f2).getFile(0x02).getData();
                mBalance = (short) (((0xFF & data[18]) << 8) | (0xFF & data[19]));
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing Clipper balance", ex);
            }

            try {
                mTrips = parseTrips(desfireCard);
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing Clipper trips", ex);
            }

            try {
                mRefills = parseRefills(desfireCard);
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing Clipper refills", ex);
            }
    	}
    }

	/* (non-Javadoc)
	 * @see com.codebutler.farebot.transit.TransitData#getBalanceString()
	 */
	@Override
	public String getBalanceString() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codebutler.farebot.transit.TransitData#getSerialNumber()
	 */
	@Override
	public String getSerialNumber() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codebutler.farebot.transit.TransitData#getTrips()
	 */
	@Override
	public Trip[] getTrips() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codebutler.farebot.transit.TransitData#getRefills()
	 */
	@Override
	public Refill[] getRefills() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.codebutler.farebot.transit.TransitData#getCardName()
	 */
	@Override
	public String getCardName() {
		// TODO Auto-generated method stub
		return null;
	}

}
