/*
 * DesfireManufacturingData.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2015 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.ByteArrayInputStream;

@Root(name = "manufacturing-data")
public class DesfireManufacturingData {

    @Element(name = "hw-vendor-id") public int hwVendorID;
    @Element(name = "hw-type") public int hwType;
    @Element(name = "hw-sub-type") public int hwSubType;
    @Element(name = "hw-major-version") public int hwMajorVersion;
    @Element(name = "hw-minor-version") public int hwMinorVersion;
    @Element(name = "hw-storage-size") public int hwStorageSize;
    @Element(name = "hw-protocol") public int hwProtocol;

    @Element(name = "sw-vendor-id") public int swVendorID;
    @Element(name = "sw-type") public int swType;
    @Element(name = "sw-sub-type") public int swSubType;
    @Element(name = "sw-major-version") public int swMajorVersion;
    @Element(name = "sw-minor-version") public int swMinorVersion;
    @Element(name = "sw-storage-size") public int swStorageSize;
    @Element(name = "sw-protocol") public int swProtocol;

    @Element(name = "uid") public int uid;
    @Element(name = "batch-no") public int batchNo;
    @Element(name = "week-prod") public int weekProd;
    @Element(name = "year-prod") public int yearProd;

    DesfireManufacturingData(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        hwVendorID = stream.read();
        hwType = stream.read();
        hwSubType = stream.read();
        hwMajorVersion = stream.read();
        hwMinorVersion = stream.read();
        hwStorageSize = stream.read();
        hwProtocol = stream.read();

        swVendorID = stream.read();
        swType = stream.read();
        swSubType = stream.read();
        swMajorVersion = stream.read();
        swMinorVersion = stream.read();
        swStorageSize = stream.read();
        swProtocol = stream.read();

        // FIXME: This has fewer digits than what's contained in EXTRA_ID, why?
        byte[] buf = new byte[7];
        stream.read(buf, 0, buf.length);
        uid = Utils.byteArrayToInt(buf);

        // FIXME: This is returning a negative number. Probably is unsigned.
        buf = new byte[5];
        stream.read(buf, 0, buf.length);
        batchNo = Utils.byteArrayToInt(buf);

        // FIXME: These numbers aren't making sense.
        weekProd = stream.read();
        yearProd = stream.read();
    }

    private DesfireManufacturingData() { /* For XML Serializer */ }
}
