/*
 * UnsupportedDesfireFile.java
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

package com.codebutler.farebot.card.desfire;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * Represents a DESFire file which could not be read due to
 * access control limits.
 */
@Root(name = "file")
public class UnauthorizedDesfireFile extends InvalidDesfireFile {
    @Attribute(name = "unauthorized") public static final boolean UNAUTHORIZED = true;

    @SuppressWarnings("unused")
    private UnauthorizedDesfireFile() { /* For XML Serializer */ }

    public UnauthorizedDesfireFile(int fileId, String errorMessage, DesfireFileSettings settings) {
        super(fileId, errorMessage, settings);
    }

    @Override
    public byte[] getData() {
        throw new IllegalStateException(String.format("Unauthorized access to file: %s", getErrorMessage()));
    }
}
