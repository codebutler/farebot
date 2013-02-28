/*
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
package com.codebutler.farebot.transit;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.Parcel;
import android.os.Parcelable;

public class BilheteUnicoSPCredit implements Parcelable {
    private final int mCredit;

    public BilheteUnicoSPCredit (int credit) {
        mCredit   = credit;
    }

    public BilheteUnicoSPCredit (byte[] data) {
        if (data == null) {
            data = new byte[16];
        }

        mCredit = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
    }

    public int getCredit() {
        return mCredit;
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<BilheteUnicoSPCredit> CREATOR = new Parcelable.Creator<BilheteUnicoSPCredit>() {
        public BilheteUnicoSPCredit createFromParcel(Parcel source) {
            int credit   = source.readInt();
            return new BilheteUnicoSPCredit(credit);
        }

        public BilheteUnicoSPCredit[] newArray (int size) {
            return new BilheteUnicoSPCredit[size];
        }
    };

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mCredit);
    }
}