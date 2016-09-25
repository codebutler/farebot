package com.codebutler.farebot.card.desfire.raw;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.InvalidDesfireFile;
import com.codebutler.farebot.card.desfire.RecordDesfireFile;
import com.codebutler.farebot.card.desfire.StandardDesfireFile;
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile;
import com.codebutler.farebot.card.desfire.ValueDesfireFile;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import static com.codebutler.farebot.card.desfire.DesfireFileSettings.BACKUP_DATA_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.CYCLIC_RECORD_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.LINEAR_RECORD_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.STANDARD_DATA_FILE;
import static com.codebutler.farebot.card.desfire.DesfireFileSettings.VALUE_FILE;

@AutoValue
public abstract class RawDesfireFile {

    @NonNull
    public static RawDesfireFile create(
            int fileId,
            @NonNull RawDesfireFileSettings fileSettings,
            @NonNull byte[] fileData) {
        return new AutoValue_RawDesfireFile(fileId, fileSettings, ByteArray.create(fileData), null);
    }

    @NonNull
    public static RawDesfireFile createUnauthorized(
            int fileId,
            @NonNull RawDesfireFileSettings fileSettings,
            @NonNull String errorMessage) {
        Error error = Error.create(Error.TYPE_UNAUTHORIZED, errorMessage);
        return new AutoValue_RawDesfireFile(fileId, fileSettings, null, error);
    }

    @NonNull
    public static RawDesfireFile createInvalid(
            int fileId,
            @NonNull RawDesfireFileSettings fileSettings,
            @NonNull String errorMessage) {
        Error error = Error.create(Error.TYPE_INVALID, errorMessage);
        return new AutoValue_RawDesfireFile(fileId, fileSettings, null, error);
    }

    @NonNull
    public static TypeAdapter<RawDesfireFile> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireFile.GsonTypeAdapter(gson);
    }

    @NonNull
    public DesfireFile parse() {
        Error error = error();
        if (error != null) {
            if (error.type() == Error.TYPE_UNAUTHORIZED) {
                return UnauthorizedDesfireFile.create(fileId(), fileSettings().parse(), error.message());
            } else {
                return InvalidDesfireFile.create(fileId(), fileSettings().parse(), error.message());
            }
        }
        ByteArray data = fileData();
        if (data == null) {
            throw new RuntimeException("fileData was null");
        }
        DesfireFileSettings fileSettings = fileSettings().parse();
        switch (fileSettings.getFileType()) {
            case STANDARD_DATA_FILE:
            case BACKUP_DATA_FILE:
                return StandardDesfireFile.create(fileId(), fileSettings, data.bytes());
            case LINEAR_RECORD_FILE:
            case CYCLIC_RECORD_FILE:
                return RecordDesfireFile.create(fileId(), fileSettings, data.bytes());
            case VALUE_FILE:
                return ValueDesfireFile.create(fileId(), fileSettings, data.bytes());
            default:
                throw new RuntimeException("Unknown file type: " + Integer.toHexString(fileSettings.getFileType()));
        }
    }

    public abstract int fileId();

    public abstract RawDesfireFileSettings fileSettings();

    @Nullable
    public abstract ByteArray fileData();

    @Nullable
    public abstract Error error();

    @AutoValue
    public abstract static class Error {

        public static final int TYPE_NONE = 0;
        public static final int TYPE_UNAUTHORIZED = 1;
        public static final int TYPE_INVALID = 2;

        @NonNull
        static Error create(int type, @NonNull String message) {
            return new AutoValue_RawDesfireFile_Error(type, message);
        }

        @NonNull
        public static TypeAdapter<Error> typeAdapter(@NonNull Gson gson) {
            return new AutoValue_RawDesfireFile_Error.GsonTypeAdapter(gson);
        }

        public abstract int type();

        @Nullable
        public abstract String message();
    }
}
