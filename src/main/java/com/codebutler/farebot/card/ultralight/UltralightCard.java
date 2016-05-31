package com.codebutler.farebot.card.ultralight;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardHasManufacturingInfo;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.UnsupportedTagException;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.fragment.UltralightCardRawDataFragment;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Utility class for reading Mifare Ultralight / Ultralight C
 */
@Root(name = "card")
@CardRawDataFragmentClass(UltralightCardRawDataFragment.class)
@CardHasManufacturingInfo(false)
public class UltralightCard extends Card {
    @ElementList(name = "pages") private List<UltralightPage> mPages;
    @Attribute(name = "ultralightType") private int mUltralightType;

    private static final int ULTRALIGHT_SIZE = 0x0F;
    private static final int ULTRALIGHT_C_SIZE = 0x2B;

    private UltralightCard() { /* For XML Serializer */ }

    private UltralightCard(byte[] tagId, Date scannedAt, int ultralightType, UltralightPage[] pages) {
        super(CardType.MifareUltralight, tagId, scannedAt);
        mUltralightType = ultralightType;
        mPages = Utils.arrayAsList(pages);
    }

    public static UltralightCard dumpTag(byte[] tagId, Tag tag) throws Exception {
        List<DesfireApplication> apps = new ArrayList<>();

        MifareUltralight tech = null;

        try {
            tech = MifareUltralight.get(tag);
            tech.connect();

            int size;
            switch (tech.getType()) {
                case MifareUltralight.TYPE_ULTRALIGHT:
                    size = ULTRALIGHT_SIZE;
                    break;
                case MifareUltralight.TYPE_ULTRALIGHT_C:
                    size = ULTRALIGHT_C_SIZE;
                    break;

                // unknown
                default:
                    throw new UnsupportedTagException(new String[]{"Ultralight"},
                            "Unknown Ultralight type " + tech.getType());
            }

            // Now iterate through the pages and grab all the datas
            int pageNumber = 0;
            byte[] pageBuffer = new byte[0];
            List<UltralightPage> pages = new ArrayList<>();
            while (pageNumber <= size) {
                if (pageNumber % 4 == 0) {
                    // Lets make a new buffer of data. (16 bytes = 4 pages * 4 bytes)
                    pageBuffer = tech.readPages(pageNumber);
                }

                // Now lets stuff this into some pages.
                pages.add(new UltralightPage(pageNumber, Arrays.copyOfRange(
                        pageBuffer,
                        (pageNumber % 4) * MifareUltralight.PAGE_SIZE,
                        ((pageNumber % 4) + 1) * MifareUltralight.PAGE_SIZE)));
                pageNumber++;
            }

            // Now we have pages to stuff in the card.
            return new UltralightCard(tagId, new Date(), tech.getType(),
                    pages.toArray(new UltralightPage[pages.size()]));

        } finally {
            if (tech != null && tech.isConnected()) {
                tech.close();
            }
        }
    }


    @Override
    public TransitIdentity parseTransitIdentity() {

        // The card could not be identified.
        return null;
    }

    @Override
    public TransitData parseTransitData() {

        // The card could not be identified.
        return null;
    }

    public UltralightPage[] getPages() {
        return mPages.toArray(new UltralightPage[mPages.size()]);
    }

    public UltralightPage getPage(int index) {
        return mPages.get(index);
    }

    /**
     * Get the type of Ultralight card this is.  This is either MifareUltralight.TYPE_ULTRALIGHT,
     * or MifareUltralight.TYPE_ULTRALIGHT_C.
     *
     * @return Type of Ultralight card this is.
     */
    public int getUltralightType() {
        return mUltralightType;
    }
}
