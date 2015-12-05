package com.codebutler.farebot.transit.manly_fast_ferry;

import android.os.Parcel;

import com.codebutler.farebot.R;
import com.codebutler.farebot.card.UnauthorizedException;
import com.codebutler.farebot.card.classic.ClassicBlock;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryBalanceRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryMetadataRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryPurseRecord;
import com.codebutler.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRecord;
import com.codebutler.farebot.ui.HeaderListItem;
import com.codebutler.farebot.ui.ListItem;
import com.codebutler.farebot.util.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Transit data type for Manly Fast Ferry Smartcard (Sydney, AU).
 *
 * This transit card is a system made by ERG Group (now Videlli Limited / Vix Technology).
 *
 * Note: This is a distinct private company who run their own ferry service to Manly, separate to
 * Transport for NSW's Manly Ferry service.
 *
 * Documentation of format: https://github.com/micolous/farebot/wiki/Manly-Fast-Ferry
 */
public class ManlyFastFerryTransitData extends TransitData {
    private String            mSerialNumber;
    private GregorianCalendar mEpochDate;
    private int               mBalance;
    private Trip[]            mTrips;
    private Refill[]          mRefills;


    public static final String NAME = "Manly Fast Ferry";

    public static final byte[] SIGNATURE = {
            0x32, 0x32, 0x00, 0x00, 0x00, 0x01, 0x01
    };

    public static boolean check(ClassicCard card) {
        // TODO: Improve this check
        // The card contains two copies of the card's serial number on the card.
        // Lets use this for now to check that this is a Manly Fast Ferry card.
        byte[] file1; //, file2;

        try {
            file1 = card.getSector(0).getBlock(1).getData();
            //file2 = card.getSector(0).getBlock(2).getData();
        } catch (UnauthorizedException ex) {
            // These blocks of the card are not protected.
            // This must not be a Manly Fast Ferry smartcard.
            return false;
        }

        // Serial number is from byte 10 in file 1 and byte 7 of file 2, for 4 bytes.
        // DISABLED: This check fails on 2012-era cards.
        //if (!Arrays.equals(Arrays.copyOfRange(file1, 10, 14), Arrays.copyOfRange(file2, 7, 11))) {
        //    return false;
        //}

        // Check a signature
        return Arrays.equals(Arrays.copyOfRange(file1, 0, SIGNATURE.length), SIGNATURE);
    }

    public static TransitIdentity parseTransitIdentity(ClassicCard card) {
        byte[] file2 = card.getSector(0).getBlock(2).getData();
        ManlyFastFerryRecord metadata = ManlyFastFerryRecord.recordFromBytes(file2);
        if (!(metadata instanceof ManlyFastFerryMetadataRecord)) {
            throw new AssertionError("Unexpected Manly record type: " + metadata.getClass().toString());
        }
        return new TransitIdentity(NAME, ((ManlyFastFerryMetadataRecord)metadata).getCardSerial());
    }

    // Parcel
    @SuppressWarnings("UnusedDeclaration")
    public ManlyFastFerryTransitData (Parcel parcel) {
        mSerialNumber = parcel.readString();
        mEpochDate = new GregorianCalendar();
        mEpochDate.setTimeInMillis(parcel.readLong());
        mTrips = parcel.createTypedArray(ManlyFastFerryTrip.CREATOR);
        mRefills = parcel.createTypedArray(ManlyFastFerryRefill.CREATOR);
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeLong(mEpochDate.getTimeInMillis());
        parcel.writeTypedArray(mTrips, flags);
        parcel.writeTypedArray(mRefills, flags);
    }

    // Decoder
    public ManlyFastFerryTransitData(ClassicCard card) {
        ArrayList<ManlyFastFerryRecord> records = new ArrayList<>();

        // Iterate through blocks on the card and deserialize all the binary data.
        for (ClassicSector sector : card.getSectors()) {
            for (ClassicBlock block : sector.getBlocks()) {
                if (sector.getIndex() == 0 && block.getIndex() == 0) {
                    continue;
                }

                if (block.getIndex() == 3) {
                    continue;
                }

                ManlyFastFerryRecord record = ManlyFastFerryRecord.recordFromBytes(block.getData());

                if (record != null) {
                    records.add(record);
                }
            }

        }

        // Now do a first pass for metadata and balance information.
        ArrayList<ManlyFastFerryBalanceRecord> balances = new ArrayList<>();

        for (ManlyFastFerryRecord record : records) {
            if (record instanceof ManlyFastFerryMetadataRecord) {
                mSerialNumber = ((ManlyFastFerryMetadataRecord)record).getCardSerial();
                mEpochDate = ((ManlyFastFerryMetadataRecord)record).getEpochDate();
            } else if (record instanceof ManlyFastFerryBalanceRecord) {
                balances.add((ManlyFastFerryBalanceRecord)record);
            }
        }

        if (balances.size() >= 1) {
            Collections.sort(balances);
            mBalance = balances.get(0).getBalance();
        }

        // Now generate a transaction list.
        // These need the Epoch to be known first.
        ArrayList<Trip> trips = new ArrayList<>();
        ArrayList<Refill> refills = new ArrayList<>();

        for (ManlyFastFerryRecord record: records) {
            if (record instanceof ManlyFastFerryPurseRecord) {
                ManlyFastFerryPurseRecord purseRecord = (ManlyFastFerryPurseRecord)record;

                // Now convert this.
                if (purseRecord.getIsCredit()) {
                    // Credit
                    refills.add(new ManlyFastFerryRefill(purseRecord, mEpochDate));
                } else {
                    // Debit
                    trips.add(new ManlyFastFerryTrip(purseRecord, mEpochDate));
                }
            }
        }

        Collections.sort(trips, new Trip.Comparator());
        Collections.sort(refills, new Refill.Comparator());

        mTrips = trips.toArray(new Trip[trips.size()]);
        mRefills = refills.toArray(new Refill[refills.size()]);
    }

    @Override
    public String getBalanceString() {
        return NumberFormat.getCurrencyInstance(Locale.US).format((double)mBalance / 100.);
    }

    // Structures
    @Override public String getSerialNumber () {
        return mSerialNumber;
    }

    @Override
    public Trip[] getTrips() {
        return mTrips;
    }

    @Override
    public Refill[] getRefills() {
        return mRefills;
    }

    @Override
    public Subscription[] getSubscriptions() {
        // There is no concept of "subscriptions".
        return null;
    }

    @Override
    public List<ListItem> getInfo() {
        ArrayList<ListItem> items = new ArrayList<>();
        items.add(new HeaderListItem(R.string.general));
        Date cLastTransactionTime = mEpochDate.getTime();
        items.add(new ListItem(R.string.card_epoch, Utils.longDateFormat(cLastTransactionTime)));

        return items;
    }

    @Override
    public String getCardName() {
        return NAME;
    }


}
