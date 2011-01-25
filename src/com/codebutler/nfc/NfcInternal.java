/*
 * NfcInternal.java
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

package com.codebutler.nfc;

import android.nfc.INfcTag;
import android.nfc.NfcAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class NfcInternal
{
    public static byte[] transceive (Object tag, byte[] data) throws Exception
    {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter();

        Class<?> tagClass = Class.forName("android.nfc.Tag");
        Method method = NfcAdapter.class.getMethod("createRawTagConnection", new Class[] { tagClass });

        Object conn = method.invoke(adapter, tag);
        Class<?> rawConnectionClass = Class.forName("android.nfc.RawTagConnection");

        Method transceiveMethod = rawConnectionClass.getMethod("transceive", new Class[] { byte[].class });
        return (byte[]) transceiveMethod.invoke(conn, data);
    }

    // r u serious?
    public static String getCardType (Object tagObject) throws Exception
    {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter();

        Class<?> tagClass = Class.forName("android.nfc.Tag");
        Method method = NfcAdapter.class.getMethod("createRawTagConnection", new Class[] { tagClass });

        Object conn = method.invoke(adapter, tagObject);
        Class<?> rawConnectionClass = Class.forName("android.nfc.RawTagConnection");

        Method getServiceHandleMethod = tagClass.getMethod("getServiceHandle");
        Integer serviceHandle = (Integer) getServiceHandleMethod.invoke(tagObject, null);

        Field tagServiceField = rawConnectionClass.getDeclaredField("mTagService");
        tagServiceField.setAccessible(true);

        INfcTag tagService = (INfcTag) tagServiceField.get(conn);

        return tagService.getType(serviceHandle);
    }
}
