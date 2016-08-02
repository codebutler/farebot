package com.codebutler.farebot.card.desfire.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawDesfireApplication {

    @NonNull
    public static RawDesfireApplication create(int appId, @NonNull List<RawDesfireFile> rawDesfireFiles) {
        return new AutoValue_RawDesfireApplication(appId, rawDesfireFiles);
    }

    @NonNull
    public static TypeAdapter<RawDesfireApplication> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireApplication.GsonTypeAdapter(gson);
    }

    @NonNull
    public DesfireApplication parse() {
        List<DesfireFile> files = newArrayList(transform(files(), new Function<RawDesfireFile, DesfireFile>() {
            @Override
            public DesfireFile apply(RawDesfireFile rawDesfireFile) {
                return rawDesfireFile.parse();
            }
        }));
        return DesfireApplication.create(appId(), files);
    }

    public abstract int appId();

    @NonNull
    public abstract List<RawDesfireFile> files();
}
