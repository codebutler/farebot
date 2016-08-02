package com.codebutler.farebot.card.ultralight;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardHasManufacturingInfo;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.fragment.UltralightCardRawDataFragment;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

/**
 * Utility class for reading Mifare Ultralight / Ultralight C
 */
@CardRawDataFragmentClass(UltralightCardRawDataFragment.class)
@CardHasManufacturingInfo(false)
@AutoValue
public abstract class UltralightCard implements Card {

    static final int ULTRALIGHT_SIZE = 0x0F;
    static final int ULTRALIGHT_C_SIZE = 0x2B;

    @NonNull
    public static UltralightCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<UltralightPage> pages,
            int type) {
        return new AutoValue_UltralightCard(
                tagId,
                scannedAt,
                pages,
                type);
    }

    @NonNull
    @Override
    public CardType getCardType() {
        return CardType.MifareUltralight;
    }

    @Nullable
    @Override
    public TransitIdentity parseTransitIdentity() {
        // The card could not be identified.
        return null;
    }

    @Nullable
    @Override
    public TransitData parseTransitData() {
        // The card could not be identified.
        return null;
    }

    @NonNull
    public abstract List<UltralightPage> getPages();

    @NonNull
    public UltralightPage getPage(int index) {
        return getPages().get(index);
    }

    /**
     * Get the type of Ultralight card this is.  This is either MifareUltralight.TYPE_ULTRALIGHT,
     * or MifareUltralight.TYPE_ULTRALIGHT_C.
     *
     * @return Type of Ultralight card this is.
     */
    public abstract int getUltralightType();
}
