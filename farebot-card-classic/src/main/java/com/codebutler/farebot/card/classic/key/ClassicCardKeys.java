/*
 * ClassicCardKeys.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.card.classic.key;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassicCardKeys implements CardKeys {

    private static final String KEYS = "keys";
    private static final int KEY_LEN = 6;

    private ClassicSectorKey[] mSectorKeys;

    private ClassicCardKeys(ClassicSectorKey[] sectorKeys) {
        mSectorKeys = sectorKeys;
    }

    public static ClassicCardKeys fromDump(String keyType, byte[] keyData) {
        List<ClassicSectorKey> keys = new ArrayList<>();

        int numSectors = keyData.length / KEY_LEN;
        for (int i = 0; i < numSectors; i++) {
            int start = i * KEY_LEN;
            keys.add(new ClassicSectorKey(keyType, Arrays.copyOfRange(keyData, start, start + KEY_LEN)));
        }

        return new ClassicCardKeys(keys.toArray(new ClassicSectorKey[keys.size()]));
    }

    public static ClassicCardKeys fromJSON(JSONObject json) throws JSONException {
        JSONArray keysJson = json.getJSONArray(KEYS);
        ClassicSectorKey[] sectorKeys = new ClassicSectorKey[keysJson.length()];
        for (int i = 0; i < keysJson.length(); i++) {
            sectorKeys[i] = ClassicSectorKey.fromJSON(keysJson.getJSONObject(i));
        }
        return new ClassicCardKeys(sectorKeys);
    }

    /**
     * Gets the key for a particular sector on the card.
     *
     * @param sectorNumber The sector number to retrieve the key for
     * @return A ClassicSectorKey for that sector, or null if there is no known key or the value is
     * out of range.
     */
    public ClassicSectorKey keyForSector(int sectorNumber) {
        if (sectorNumber >= mSectorKeys.length) {
            return null;
        }
        return mSectorKeys[sectorNumber];
    }

    public ClassicSectorKey[] keys() {
        return mSectorKeys.clone();
    }

    @Override
    public JSONObject toJSON() {
        try {
            JSONArray keysJson = new JSONArray();
            for (ClassicSectorKey key : mSectorKeys) {
                keysJson.put(key.toJSON());
            }

            JSONObject json = new JSONObject();
            json.put(KEYS, keysJson);
            return json;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
