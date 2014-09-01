package com.codebutler.farebot.xml;

import android.util.Base64;

import net.kazzz.felica.lib.FeliCaLib;

import org.simpleframework.xml.transform.Transform;

public class FelicaIDmTransform implements Transform<FeliCaLib.IDm> {
    @Override public FeliCaLib.IDm read(String value) throws Exception {
        return new FeliCaLib.IDm(Base64.decode(value, Base64.DEFAULT));
    }

    @Override public String write(FeliCaLib.IDm value) throws Exception {
        return Base64.encodeToString(value.getBytes(), Base64.NO_WRAP);
    }
}
