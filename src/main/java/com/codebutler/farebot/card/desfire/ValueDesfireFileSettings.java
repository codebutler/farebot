package com.codebutler.farebot.card.desfire;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.google.auto.value.AutoValue;

/**
 * Contains FileSettings for Value file types.
 * See GetFileSettings for schemadata.
 */
@AutoValue
public abstract class ValueDesfireFileSettings extends DesfireFileSettings {

    @NonNull
    public static ValueDesfireFileSettings create(
            byte fileType,
            byte commSetting,
            @NonNull byte[] accessRights,
            int lowerLimit,
            int upperLimit,
            int limitedCreditValue,
            boolean limitedCreditEnabled) {
        return new AutoValue_ValueDesfireFileSettings(
                fileType,
                commSetting,
                ByteArray.create(accessRights),
                lowerLimit,
                upperLimit,
                limitedCreditValue,
                limitedCreditEnabled);
    }

    public abstract int getLowerLimit();

    public abstract int getUpperLimit();

    public abstract int getLimitedCreditValue();

    public abstract boolean getLimitedCreditEnabled();
}
