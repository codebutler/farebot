package com.codebutler.farebot.persist.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.db.SavedKeyModel;
import com.codebutler.farebot.persist.CardKeysPersister;
import com.codebutler.farebot.persist.model.SavedKey;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.ArrayList;
import java.util.List;

public class DbCardKeysPersister implements CardKeysPersister {

    @NonNull private final FareBotOpenHelper mOpenHelper;

    public DbCardKeysPersister(@NonNull FareBotOpenHelper openHelper) {
        mOpenHelper = openHelper;
    }

    @NonNull
    @Override
    public List<SavedKey> getSavedKeys() {
        List<SavedKey> result = new ArrayList<>();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(SavedKey.SELECT_ALL, null)) {
            while (cursor.moveToNext()) {
                result.add(SavedKey.SELECT_ALL_MAPPER.map(cursor));
            }
        }
        return result;
    }

    @Nullable
    @Override
    public SavedKey getForTagId(@NonNull String tagId) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SqlDelightStatement query = SavedKey.FACTORY.select_by_card_id(tagId);
        try (Cursor cursor = db.rawQuery(query.statement, query.args)) {
            if (cursor.moveToFirst()) {
                return SavedKey.SELECT_ALL_MAPPER.map(cursor);
            }
        }
        return null;
    }

    @Override
    public long insert(@NonNull SavedKey savedKey) {
        try (SQLiteDatabase db = mOpenHelper.getWritableDatabase()) {
            SavedKey.Insert_row insertRow = new SavedKeyModel.Insert_row(db, SavedKey.FACTORY);
            insertRow.bind(
                    savedKey.card_id(),
                    savedKey.card_type(),
                    savedKey.key_data(),
                    savedKey.created_at());
            return insertRow.program.executeInsert();
        }
    }

    @Override
    public void delete(@NonNull SavedKey savedKey) {
        try (SQLiteDatabase db = mOpenHelper.getWritableDatabase()) {
            SavedKey.Delete_by_id deleteRow = new SavedKeyModel.Delete_by_id(db);
            deleteRow.bind(savedKey._id());
            deleteRow.program.executeUpdateDelete();
        }
    }
}
