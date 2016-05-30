/*
 * DesfireFileConverter.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.xml;

import android.util.Base64;

import com.codebutler.farebot.card.desfire.DesfireFile;
import com.codebutler.farebot.card.desfire.DesfireFileSettings;
import com.codebutler.farebot.card.desfire.InvalidDesfireFile;
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class DesfireFileConverter implements Converter<DesfireFile> {
    private final Serializer mSerializer;

    public DesfireFileConverter(Serializer serializer) {
        mSerializer = serializer;
    }

    @Override public DesfireFile read(InputNode source) throws Exception {
        int id = Integer.parseInt(source.getAttribute("id").getValue());
        InputNode nUnauthorized = source.getAttribute("unauthorized");
        boolean unauthorized = false;
        if (nUnauthorized != null) {
            unauthorized = Boolean.parseBoolean(nUnauthorized.getValue());
        }
        DesfireFileSettings settings = null;
        byte[] data = null;
        String error = null;
        while (true) {
            InputNode node = source.getNext();
            if (node == null) {
                break;
            }
            switch (node.getName()) {
                case "settings":
                    settings = mSerializer.read(DesfireFileSettings.class, node);
                    break;
                case "data":
                    String value = node.getValue();
                    if (value != null) {
                        data = Base64.decode(value, Base64.DEFAULT);
                    }
                    break;
                case "error":
                    error = node.getValue();
                    break;
            }
        }
        if (unauthorized) {
            return new UnauthorizedDesfireFile(id, error, settings);
        }

        if (error != null) {
            return new InvalidDesfireFile(id, error, settings);
        }
        return DesfireFile.create(id, settings, data);
    }

    @Override public void write(OutputNode node, DesfireFile value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
