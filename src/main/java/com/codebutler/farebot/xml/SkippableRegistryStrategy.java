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
    @Override public Value read(Type type, NodeMap<InputNode> node, Map map) throws Exception {
        try {
            return super.read(type, node, map);
        } catch (SkipException ignored) {
            return null;
        }
    }

    @Override public boolean write(Type type, Object value, NodeMap<OutputNode> node, Map map) throws Exception {
        try {
            return super.write(type, value, node, map);
        } catch (SkipException ignored) {
            return false;
        }
    }

    public static class SkipException extends Exception { }
}