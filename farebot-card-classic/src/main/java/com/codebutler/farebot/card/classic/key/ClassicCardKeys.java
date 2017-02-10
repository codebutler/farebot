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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.key.CardKeys;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AutoValue
public abstract class ClassicCardKeys implements CardKeys {

    private static final int KEY_LEN = 6;

    @NonNull
    public static ClassicCardKeys fromDump(String keyType, byte[] keyData) {
        List<ClassicSectorKey> keys = new ArrayList<>();
        int numSectors = keyData.length / KEY_LEN;
        for (int i = 0; i < numSectors; i++) {
            int start = i * KEY_LEN;
            keys.add(ClassicSectorKey.create(keyType, Arrays.copyOfRange(keyData, start, start + KEY_LEN)));
        }
        return ClassicCardKeys.create(keys);
    }

    @NonNull
    public static ClassicCardKeys create(@NonNull List<ClassicSectorKey> keys) {
        return new AutoValue_ClassicCardKeys(CardType.MifareClassic, keys);
    }

    /**
     * Gets the key for a particular sector on the card.
     *
     * @param sectorNumber The sector number to retrieve the key for
     * @return A ClassicSectorKey for that sector, or null if there is no known key or the value is
     * out of range.
     */
    @Nullable
    public ClassicSectorKey keyForSector(int sectorNumber) {
        List<ClassicSectorKey> keys = getKeys();
        if (sectorNumber >= keys.size()) {
            return null;
        }
        return keys.get(sectorNumber);
    }

    public abstract List<ClassicSectorKey> getKeys();
}
