/*
 * FelicaBlock.java
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

package com.codebutler.farebot.card.felica;

import android.os.Parcel;
import android.os.Parcelable;

public class FelicaBlock implements Parcelable {
    private byte   mAddr;
    private byte[] mData;

    public static Creator<FelicaBlock> CREATOR = new Creator<FelicaBlock>() {
        public FelicaBlock createFromParcel(Parcel parcel) {
            byte addr       = parcel.readByte();
            int  dataLenght = parcel.readInt();

            byte[] data = new byte[dataLenght];
            parcel.readByteArray(data);

            return new FelicaBlock(addr, data);
        }

        public FelicaBlock[] newArray(int size) {
            return new FelicaBlock[size];
        }
    };

    public FelicaBlock(byte addr, byte[] data) {
        mAddr = addr;
        mData = data;
    }

    public byte getAddress() {
        return mAddr;
    }

    public byte[] getData() {
        return mData;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeByte(mAddr);
        parcel.writeInt(mData.length);
        parcel.writeByteArray(mData);
    }

    public int describeContents() {
        return 0;
    }
}
