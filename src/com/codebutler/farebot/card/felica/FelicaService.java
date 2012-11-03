/*
 * FelicaService.java
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

public class FelicaService implements Parcelable {
    private int           mServiceCode;
    private FelicaBlock[] mBlocks;

    public static Creator<FelicaService> CREATOR = new Creator<FelicaService>() {
        public FelicaService createFromParcel(Parcel parcel) {
            int serviceCode = parcel.readInt();

            FelicaBlock[] blocks = new FelicaBlock[parcel.readInt()];
            parcel.readTypedArray(blocks, FelicaBlock.CREATOR);

            return new FelicaService(serviceCode, blocks);
        }

        public FelicaService[] newArray(int size) {
            return new FelicaService[size];
        }
    };

    public FelicaService(int serviceCode, FelicaBlock[] blocks) {
        mServiceCode = serviceCode;
        mBlocks      = blocks;
    }

    public int getServiceCode() {
        return mServiceCode;
    }

    public FelicaBlock[] getBlocks() {
        return mBlocks;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mServiceCode);
        parcel.writeInt(mBlocks.length);
        parcel.writeTypedArray(mBlocks, flags);
    }

    public int describeContents() {
        return 0;
    }
}
