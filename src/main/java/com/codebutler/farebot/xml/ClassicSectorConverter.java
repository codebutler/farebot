/*
 * ClassicSectorConverter.java
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

import com.codebutler.farebot.card.classic.ClassicSector;
import com.codebutler.farebot.card.classic.InvalidClassicSector;
import com.codebutler.farebot.card.classic.UnauthorizedClassicSector;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class ClassicSectorConverter implements Converter<ClassicSector> {
    @Override public ClassicSector read(InputNode node) throws Exception {
        int sectorIndex = Integer.parseInt(node.getAttribute("index").getValue());

        if (node.getAttribute("unauthorized") != null && node.getAttribute("unauthorized").getValue().equals("true")) {
            return new UnauthorizedClassicSector(sectorIndex);
        }

        if (node.getAttribute("invalid") != null && node.getAttribute("invalid").getValue().equals("true")) {
            return new InvalidClassicSector(sectorIndex, node.getAttribute("error").getValue());
        }

        throw new SkippableRegistryStrategy.SkipException();
    }

    @Override public void write(OutputNode node, ClassicSector value) throws Exception {
        throw new SkippableRegistryStrategy.SkipException();
    }
}
