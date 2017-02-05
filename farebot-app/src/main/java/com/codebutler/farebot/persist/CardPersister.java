/*
 * CardPersister.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.persist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.provider.CardProvider;
import com.codebutler.farebot.card.provider.CardsTableColumns;
import com.codebutler.farebot.card.serialize.CardSerializer;

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
