package com.codebutler.farebot.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImmutableMapBuilder<K,V> {

    private HashMap<K,V> mMap = new HashMap<>();

    public ImmutableMapBuilder<K,V> put(K key, V value) {
        mMap.put(key, value);
        return this;
    }

    public Map<K,V> build() {
        return Collections.unmodifiableMap(new HashMap<>(mMap));
    }
}
