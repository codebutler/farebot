/*
 * CardType.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public enum CardType {
    MifareClassic(0),
    MifareUltralight(1),
    MifareDesfire(2),
    CEPAS(3),
    FeliCa(4);

    private int mValue;

    CardType(int value) {
        mValue = value;
    }

    public static CardType parseValue(String value) {
        return CardType.class.getEnumConstants()[Integer.parseInt(value)];
    }

    public int toInteger() {
        return mValue;
    }

    public String toString() {
        switch (mValue) {
            case 0:
                return "MIFARE Classic";
            case 1:
                return "MIFARE Ultralight";
            case 2:
                return "MIFARE DESFire";
            case 3:
                return "CEPAS";
            case 4:
                return "FeliCa";
            default:
                return "Unknown";
        }
    }

    public static class GsonTypeAdapter extends TypeAdapter<CardType> {

        @Override
        public void write(JsonWriter out, CardType value) throws IOException {
            out.value(value.name());
        }

        @Override
        public CardType read(JsonReader in) throws IOException {
            return CardType.parseValue(in.nextString());
        }
    }
}
