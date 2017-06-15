/*
 * DesfireManufacturingData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
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

import android.support.annotation.NonNull;

import com.codebutler.farebot.base.util.ByteUtils;
import com.google.auto.value.AutoValue;

import java.io.ByteArrayInputStream;

@AutoValue
public abstract class DesfireManufacturingData {

    @NonNull
    public static DesfireManufacturingData.Builder builder() {
        return new AutoValue_DesfireManufacturingData.Builder();
    }

    @NonNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static DesfireManufacturingData create(@NonNull byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        int hwVendorID = stream.read();
        int hwType = stream.read();
        int hwSubType = stream.read();
        int hwMajorVersion = stream.read();
        int hwMinorVersion = stream.read();
        int hwStorageSize = stream.read();
        int hwProtocol = stream.read();

        int swVendorID = stream.read();
        int swType = stream.read();
        int swSubType = stream.read();
        int swMajorVersion = stream.read();
        int swMinorVersion = stream.read();
        int swStorageSize = stream.read();
        int swProtocol = stream.read();

        // FIXME: This has fewer digits than what's contained in EXTRA_ID, why?
        byte[] buf = new byte[7];
        stream.read(buf, 0, buf.length);
        int uid = ByteUtils.byteArrayToInt(buf);

        // FIXME: This is returning a negative number. Probably is unsigned.
        buf = new byte[5];
        stream.read(buf, 0, buf.length);
        int batchNo = ByteUtils.byteArrayToInt(buf);

        // FIXME: These numbers aren't making sense.
        int weekProd = stream.read();
        int yearProd = stream.read();

        return new AutoValue_DesfireManufacturingData.Builder()
                .hwVendorID(hwVendorID)
                .hwType(hwType)
                .hwSubType(hwSubType)
                .hwMajorVersion(hwMajorVersion)
                .hwMinorVersion(hwMinorVersion)
                .hwStorageSize(hwStorageSize)
                .hwProtocol(hwProtocol)
                .swVendorID(swVendorID)
                .swType(swType)
                .swSubType(swSubType)
                .swMajorVersion(swMajorVersion)
                .swMinorVersion(swMinorVersion)
                .swStorageSize(swStorageSize)
                .swProtocol(swProtocol)
                .uid(uid)
                .batchNo(batchNo)
                .weekProd(weekProd)
                .yearProd(yearProd)
                .build();
    }

    public abstract int getHwVendorID();

    public abstract int getHwType();

    public abstract int getHwSubType();

    public abstract int getHwMajorVersion();

    public abstract int getHwMinorVersion();

    public abstract int getHwStorageSize();

    public abstract int getHwProtocol();

    public abstract int getSwVendorID();

    public abstract int getSwType();

    public abstract int getSwSubType();

    public abstract int getSwMajorVersion();

    public abstract int getSwMinorVersion();

    public abstract int getSwStorageSize();

    public abstract int getSwProtocol();

    public abstract int getUid();

    public abstract int getBatchNo();

    public abstract int getWeekProd();

    public abstract int getYearProd();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder hwVendorID(int hwVendorID);

        public abstract Builder hwType(int hwType);

        public abstract Builder hwSubType(int hwSubType);

        public abstract Builder hwMajorVersion(int hwMajorVersion);

        public abstract Builder hwMinorVersion(int hwMinorVersion);

        public abstract Builder hwStorageSize(int hwStorageSize);

        public abstract Builder hwProtocol(int hwProtocol);

        public abstract Builder swVendorID(int swVendorID);

        public abstract Builder swType(int swType);

        public abstract Builder swSubType(int swSubType);

        public abstract Builder swMajorVersion(int swMajorVersion);

        public abstract Builder swMinorVersion(int swMinorVersion);

        public abstract Builder swStorageSize(int swStorageSize);

        public abstract Builder swProtocol(int swProtocol);

        public abstract Builder uid(int uid);

        public abstract Builder batchNo(int batchNo);

        public abstract Builder weekProd(int weekProd);

        public abstract Builder yearProd(int yearProd);

        public abstract DesfireManufacturingData build();
    }
}
