package com.codebutler.farebot.persist.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.persist.db.model.SavedCard;
import com.codebutler.farebot.persist.db.model.SavedCardModel;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.ArrayList;
import java.util.List;

public class DbCardPersister implements CardPersister {

    @NonNull private final FareBotOpenHelper mOpenHelper;

    public DbCardPersister(@NonNull FareBotOpenHelper openHelper) {
        mOpenHelper = openHelper;
    }

    @NonNull
    @Override
    public List<SavedCard> getCards() {
        List<SavedCard> result = new ArrayList<>();
        try (SQLiteDatabase db = mOpenHelper.getReadableDatabase()) {
            SqlDelightStatement query = SavedCard.SELECT_ALL;
            try (Cursor cursor = db.rawQuery(query.statement, query.args)) {
                while (cursor.moveToNext()) {
                    result.add(SavedCard.SELECT_ALL_MAPPER.map(cursor));
                }
            }
        }
        return result;
    }

    @Nullable
    @Override
    public SavedCard getCard(long id) {
        try (SQLiteDatabase db = mOpenHelper.getReadableDatabase()) {
            SqlDelightStatement query = SavedCard.FACTORY.select_by_id(id);
            try (Cursor cursor = db.rawQuery(query.statement, query.args)) {
                if (cursor.moveToFirst()) {
                    return SavedCard.SELECT_ALL_MAPPER.map(cursor);
                }
            }
        }
        return null;
    }

    @Override
    public long insertCard(@NonNull SavedCard savedCard) {
        try (SQLiteDatabase db = mOpenHelper.getWritableDatabase()) {
            SavedCardModel.Insert_row insertRow = new SavedCardModel.Insert_row(db, SavedCard.FACTORY);
            insertRow.bind(
                    savedCard.type(),
                    savedCard.serial(),
                    savedCard.data(),
                    savedCard.scanned_at());
            return insertRow.program.executeInsert();
        }
    }

    public void deleteCard(@NonNull SavedCard savedCard) {
        try (SQLiteDatabase db = mOpenHelper.getWritableDatabase()) {
            SavedCardModel.Delete_by_id deleteRow = new SavedCardModel.Delete_by_id(db);
            deleteRow.bind(savedCard._id());
            deleteRow.program.executeUpdateDelete();
        }
    }
}
