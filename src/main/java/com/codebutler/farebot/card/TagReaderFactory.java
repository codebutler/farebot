/*
 * TagReaderFactory.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

import android.nfc.Tag;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.cepas.CEPASTagReader;
import com.codebutler.farebot.card.classic.ClassicTagReader;
import com.codebutler.farebot.card.desfire.DesfireTagReader;
import com.codebutler.farebot.card.felica.FelicaTagReader;
import com.codebutler.farebot.card.ultralight.UltralightTagReader;
import com.codebutler.farebot.util.Utils;

import org.apache.commons.lang3.ArrayUtils;

public class TagReaderFactory {

    @NonNull
    public TagReader getTagReader(@NonNull byte[] tagId, @NonNull Tag tag) throws UnsupportedTagException {
        String[] techs = tag.getTechList();
        if (ArrayUtils.contains(techs, "android.nfc.tech.IsoDep")) {
            return new DesfireTagReader(tagId, tag);
         } else if (ArrayUtils.contains(techs, "android.nfc.tech.NfcB")) {
            return new CEPASTagReader(tagId, tag);
        } else if (ArrayUtils.contains(techs, "android.nfc.tech.NfcF")) {
            return new FelicaTagReader(tagId, tag);
        } else if (ArrayUtils.contains(techs, "android.nfc.tech.MifareClassic")) {
            return new ClassicTagReader(tagId, tag);
        } else if (ArrayUtils.contains(techs, "android.nfc.tech.MifareUltralight")) {
            return new UltralightTagReader(tagId, tag);
        } else {
            throw new UnsupportedTagException(techs, Utils.getHexString(tag.getId()));
        }
    }
}
