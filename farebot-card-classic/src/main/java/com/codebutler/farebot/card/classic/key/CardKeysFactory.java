package com.codebutler.farebot.card.classic.key;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.provider.CardKeyProvider;
import com.codebutler.farebot.card.provider.KeysTableColumns;
import com.codebutler.farebot.core.ByteUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class CardKeysFactory {

    @NonNull private final Context mContext;

    public CardKeysFactory(@NonNull Context context) {
        mContext = context;
    }

    @Nullable
    public CardKeys forTagId(@NonNull byte[] tagId) throws Exception {
        String tagIdString = ByteUtils.getHexString(tagId);
        Cursor cursor = mContext.getContentResolver().query(
                Uri.withAppendedPath(CardKeyProvider.getContentUri(mContext), tagIdString),
                null,
                null,
                null,
                null);
        if (cursor.moveToFirst()) {
            return fromCursor(cursor);
        } else {
            return null;
        }
    }

    private static CardKeys fromCursor(Cursor cursor) throws JSONException {
        String cardType = cursor.getString(cursor.getColumnIndex(KeysTableColumns.CARD_TYPE));
        String keyData = cursor.getString(cursor.getColumnIndex(KeysTableColumns.KEY_DATA));

        JSONObject keyJSON = new JSONObject(keyData);

        if (cardType.equals("MifareClassic")) {
            return ClassicCardKeys.fromJSON(keyJSON);
        }

        throw new IllegalArgumentException("Unknown card type for key: " + cardType);
    }
}
