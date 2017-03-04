package com.codebutler.farebot.persist;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.persist.db.model.SavedKey;

import java.util.List;

public interface CardKeysPersister {

    @NonNull
    List<SavedKey> getSavedKeys();

    @Nullable
    SavedKey getForTagId(@NonNull String tagId);

    long insert(@NonNull SavedKey savedKey);

    void delete(@NonNull SavedKey savedKey);
}
