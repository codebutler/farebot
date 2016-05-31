/*
 * SkippableRegistryStrategy.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014-2015 Eric Butler <eric@codebutler.com>
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

import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

import java.util.Map;

public class SkippableRegistryStrategy extends RegistryStrategy {
    public SkippableRegistryStrategy(Registry registry) {
        super(registry);
    }

    public SkippableRegistryStrategy(Registry registry, Strategy strategy) {
        super(registry, strategy);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value read(Type type, NodeMap<InputNode> node, Map map) throws Exception {
        try {
            return super.read(type, node, map);
        } catch (SkipException ignored) {
            return null;
        }
    }

    @Override
    public boolean write(Type type, Object value, NodeMap<OutputNode> node, Map map) throws Exception {
        try {
            return super.write(type, value, node, map);
        } catch (SkipException ignored) {
            return false;
        }
    }

    public static class SkipException extends Exception {
    }
}
