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
