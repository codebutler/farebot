/*
 * FelicaSystem.java
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

public class FelicaSystem implements Parcelable {
    private int mCode;
    private FelicaService[] mServices;

    public static Creator<FelicaSystem> CREATOR = new Creator<FelicaSystem>() {
        public FelicaSystem createFromParcel(Parcel parcel) {
            int systemCode = parcel.readInt();

            FelicaService[] services = new FelicaService[parcel.readInt()];
            parcel.readTypedArray(services, FelicaService.CREATOR);

            return new FelicaSystem(systemCode, services);
        }

        public FelicaSystem[] newArray(int size) {
            return new FelicaSystem[size];
        }
    };

    public FelicaSystem(int code, FelicaService[] services) {
        mCode = code;
        mServices = services;
    }

    public int getCode() {
        return mCode;
    }

    public FelicaService[] getServices() {
        return mServices;
    }

    public FelicaService getService(int serviceCode) {
        for (FelicaService service : mServices) {
            if (service.getServiceCode() == serviceCode) {
                return service;
            }
        }
        return null;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mCode);
        parcel.writeInt(mServices.length);
        parcel.writeTypedArray(mServices, flags);
    }

    public int describeContents() {
        return 0;
    }
}
