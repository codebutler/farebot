package com.codebutler.farebot.persist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.provider.CardProvider;
import com.codebutler.farebot.provider.CardsTableColumns;
import com.codebutler.farebot.serialize.CardSerializer;

public class CardPersister {

    @NonNull private final Context mContext;
    @NonNull private final CardSerializer mCardSerializer;

    public CardPersister(@NonNull Context context, @NonNull CardSerializer cardSerializer) {
        mContext = context;
        mCardSerializer = cardSerializer;
    }

    @NonNull
    public RawCard readCard(@NonNull Cursor cursor) {
        String data = cursor.getString(cursor.getColumnIndex(CardsTableColumns.DATA));
        return mCardSerializer.deserialize(data);
    }

    @NonNull
    public Uri saveCard(@NonNull RawCard card) {
        ContentValues values = new ContentValues();
        values.put(CardsTableColumns.TYPE, card.cardType().toInteger());
        values.put(CardsTableColumns.TAG_SERIAL, card.tagId().hex());
        values.put(CardsTableColumns.DATA, mCardSerializer.serialize(card));
        values.put(CardsTableColumns.SCANNED_AT, card.scannedAt().getTime());
        return mContext.getContentResolver().insert(CardProvider.CONTENT_URI_CARD, values);
    }
}
