/*
 * Keys.java
 *
 * Copyright (C) 2012 Eric Butler
 *
 * Authors:
 * Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.keys;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.FareBotApplication;
import com.codebutler.farebot.Utils;
import com.codebutler.farebot.mifareclassic.ClassicProtocol;

public class Keys implements Parcelable {
    private String   mTagId;
    private String[] mKeys;

    protected Keys (String id, String[] keys) {
        mTagId = id;
        mKeys  = keys;
    }

    public String getId () {
        return mTagId;
    }

    public String[] getKeys () {
        return mKeys;
    }

    public String getKeyString (int sector) {
    	return mKeys[sector];
    }

    public byte[] getKeyByteArray (int sector) {
    	return Utils.hexStringToByteArray(mKeys[sector]);
    }

    public static final Parcelable.Creator<Keys> CREATOR = new Parcelable.Creator<Keys>() {
        public Keys createFromParcel(Parcel source) {
        	String id = source.readString();

            int keysLength = source.readInt();
            String[] keys = new String[keysLength];
            source.readStringArray(keys); 

            return new Keys(id, keys);
        }

        public Keys[] newArray (int size) {
            return new Keys[size];
        }
    };

    public static Keys getAllKeys(ClassicProtocol protocol, String id) {
    	String[] strKeys = null;

    	try {
    		strKeys = FareBotApplication.getInstance().getKeysUtil().getKeysForCard(protocol, id, "a");
		} catch (Exception e) {
			e.printStackTrace();
		}

    	if (strKeys == null)
    		return null;
    	else
    		return new Keys(id, strKeys);
    }

    public void writeToParcel (Parcel parcel, int flags) {
        parcel.writeString(mTagId);
        parcel.writeInt(mKeys.length);
        parcel.writeStringArray(mKeys);
    }

    public int describeContents () {
        return 0;
    } 
}