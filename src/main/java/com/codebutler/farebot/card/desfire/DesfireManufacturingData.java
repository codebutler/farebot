/*
 * DesfireManufacturingData.java
 *
 * Copyright (C) 2011 Eric Butler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
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

import android.os.Parcel;
import android.os.Parcelable;
import com.codebutler.farebot.Utils;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;

public class DesfireManufacturingData implements Parcelable {
    public final int hwVendorID;
    public final int hwType;
    public final int hwSubType;
    public final int hwMajorVersion;
    public final int hwMinorVersion;
    public final int hwStorageSize;
    public final int hwProtocol;

    public final int swVendorID;
    public final int swType;
    public final int swSubType;
    public final int swMajorVersion;
    public final int swMinorVersion;
    public final int swStorageSize;
    public final int swProtocol;

    public final int uid;
    public final int batchNo;
    public final int weekProd;
    public final int yearProd;

    public DesfireManufacturingData (byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        hwVendorID     = stream.read();
        hwType         = stream.read();
        hwSubType      = stream.read();
        hwMajorVersion = stream.read();
        hwMinorVersion = stream.read();
        hwStorageSize  = stream.read();
        hwProtocol     = stream.read();

        swVendorID     = stream.read();
        swType         = stream.read();
        swSubType      = stream.read();
        swMajorVersion = stream.read();
        swMinorVersion = stream.read();
        swStorageSize  = stream.read();
        swProtocol     = stream.read();

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

    public static DesfireManufacturingData fromXml (Element element) {
        return new DesfireManufacturingData(element);
    }

    private DesfireManufacturingData (Element element) {
        hwVendorID     = Integer.parseInt(element.getElementsByTagName("hw-vendor-id").item(0).getTextContent());
        hwType         = Integer.parseInt(element.getElementsByTagName("hw-type").item(0).getTextContent());
        hwSubType      = Integer.parseInt(element.getElementsByTagName("hw-sub-type").item(0).getTextContent());
        hwMajorVersion = Integer.parseInt(element.getElementsByTagName("hw-major-version").item(0).getTextContent());
        hwMinorVersion = Integer.parseInt(element.getElementsByTagName("hw-minor-version").item(0).getTextContent());
        hwStorageSize  = Integer.parseInt(element.getElementsByTagName("hw-storage-size").item(0).getTextContent());
        hwProtocol     = Integer.parseInt(element.getElementsByTagName("hw-protocol").item(0).getTextContent());

        swVendorID     = Integer.parseInt(element.getElementsByTagName("sw-vendor-id").item(0).getTextContent());
        swType         = Integer.parseInt(element.getElementsByTagName("sw-type").item(0).getTextContent());
        swSubType      = Integer.parseInt(element.getElementsByTagName("sw-sub-type").item(0).getTextContent());
        swMajorVersion = Integer.parseInt(element.getElementsByTagName("sw-major-version").item(0).getTextContent());
        swMinorVersion = Integer.parseInt(element.getElementsByTagName("sw-minor-version").item(0).getTextContent());
        swStorageSize  = Integer.parseInt(element.getElementsByTagName("sw-storage-size").item(0).getTextContent());
        swProtocol     = Integer.parseInt(element.getElementsByTagName("sw-protocol").item(0).getTextContent());

        uid      = Integer.parseInt(element.getElementsByTagName("uid").item(0).getTextContent());
        batchNo  = Integer.parseInt(element.getElementsByTagName("batch-no").item(0).getTextContent());
        weekProd = Integer.parseInt(element.getElementsByTagName("week-prod").item(0).getTextContent());
        yearProd = Integer.parseInt(element.getElementsByTagName("year-prod").item(0).getTextContent());
    }

    private DesfireManufacturingData (Parcel parcel) {
        hwVendorID     = parcel.readInt();
        hwType         = parcel.readInt();
        hwSubType      = parcel.readInt();
        hwMajorVersion = parcel.readInt();
        hwMinorVersion = parcel.readInt();
        hwStorageSize  = parcel.readInt();
        hwProtocol     = parcel.readInt();

        swVendorID     = parcel.readInt();
        swType         = parcel.readInt();
        swSubType      = parcel.readInt();
        swMajorVersion = parcel.readInt();
        swMinorVersion = parcel.readInt();
        swStorageSize  = parcel.readInt();
        swProtocol     = parcel.readInt();

        uid      = parcel.readInt();
        batchNo  = parcel.readInt();
        weekProd = parcel.readInt();
        yearProd = parcel.readInt();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(hwVendorID);
        parcel.writeInt(hwType);
        parcel.writeInt(hwSubType);
        parcel.writeInt(hwMajorVersion);
        parcel.writeInt(hwMinorVersion);
        parcel.writeInt(hwStorageSize);
        parcel.writeInt(hwProtocol);

        parcel.writeInt(swVendorID);
        parcel.writeInt(swType);
        parcel.writeInt(swSubType);
        parcel.writeInt(swMajorVersion);
        parcel.writeInt(swMinorVersion);
        parcel.writeInt(swStorageSize);
        parcel.writeInt(swProtocol);

        parcel.writeInt(uid);
        parcel.writeInt(batchNo);
        parcel.writeInt(weekProd);
        parcel.writeInt(yearProd);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<DesfireManufacturingData> CREATOR = new Parcelable.Creator<DesfireManufacturingData>() {
        public DesfireManufacturingData createFromParcel(Parcel source) {
            return new DesfireManufacturingData(source);
        }

        public DesfireManufacturingData[] newArray(int size) {
            return new DesfireManufacturingData[size];
        }
    };
}
