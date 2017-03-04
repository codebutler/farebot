/*
 * BilheteUnicoSPTransitFactory.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2013-2015 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Marcelo Liberato <mliberato@gmail.com>
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

package com.codebutler.farebot.transit.bilhete_unico;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.DataClassicSector;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;

import java.util.Arrays;

public class BilheteUnicoSPTransitFactory implements TransitFactory<ClassicCard, BilheteUnicoSPTransitInfo> {

    private static final byte[] MANUFACTURER = {
            (byte) 0x62,
            (byte) 0x63,
            (byte) 0x64,
            (byte) 0x65,
            (byte) 0x66,
            (byte) 0x67,
            (byte) 0x68,
            (byte) 0x69
    };

    @Override
    public boolean check(@NonNull ClassicCard card) {
        if (card.getSector(0) instanceof DataClassicSector) {
            byte[] blockData = ((DataClassicSector) card.getSector(0)).getBlock(0).getData().bytes();
            return Arrays.equals(Arrays.copyOfRange(blockData, 8, 16), MANUFACTURER);
        }
        return false;
    }

    @NonNull
    @Override
    public TransitIdentity parseIdentity(@NonNull ClassicCard card) {
        return TransitIdentity.create(BilheteUnicoSPTransitInfo.NAME, null);
    }

    @NonNull
    @Override
    public BilheteUnicoSPTransitInfo parseInfo(@NonNull ClassicCard card) {
        byte[] data = ((DataClassicSector) card.getSector(8)).getBlock(1).getData().bytes();
        BilheteUnicoSPCredit credit = BilheteUnicoSPCredit.create(data);
        return BilheteUnicoSPTransitInfo.create(credit);
    }
}
