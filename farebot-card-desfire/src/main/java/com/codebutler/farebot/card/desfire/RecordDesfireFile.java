/*
 * RecordDesfireFile.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.desfire;

import androidx.annotation.NonNull;

import com.codebutler.farebot.base.util.ArrayUtils;
import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class RecordDesfireFile implements DesfireFile {

    @NonNull
    public static RecordDesfireFile create(
            int fileId,
            @NonNull DesfireFileSettings fileSettings,
            @NonNull byte[] fileData) {
        RecordDesfireFileSettings settings = (RecordDesfireFileSettings) fileSettings;
        List<DesfireRecord> records = new ArrayList<>(settings.getCurRecords());
        for (int i = 0; i < settings.getCurRecords(); i++) {
            int start = settings.getRecordSize() * i;
            int end = start + settings.getRecordSize();
            records.add(DesfireRecord.create(ArrayUtils.subarray(fileData, start, end)));
        }
        return new AutoValue_RecordDesfireFile(
                fileId,
                fileSettings,
                Collections.unmodifiableList(records),
                ByteArray.create(fileData));
    }

    @NonNull
    public abstract List<DesfireRecord> getRecords();

    /**
     * @deprecated Use {@link #getRecords()} instead.
     */
    @NonNull
    @Deprecated
    public abstract ByteArray getData();
}
