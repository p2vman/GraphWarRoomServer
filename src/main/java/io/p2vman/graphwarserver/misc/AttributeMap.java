package io.p2vman.graphwarserver.misc;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class AttributeMap implements IAttributeMap {
    private final Object2ObjectMap<AttributeKey<?>, Object> attributes = new Object2ObjectOpenHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(AttributeKey<T> key) {
        return (T) attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T put(AttributeKey<T> key, T value) {
        return (T) attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(AttributeKey<T> key) {
        return (T) attributes.remove(key);
    }

    public boolean contains(AttributeKey<?> key) {
        return attributes.containsKey(key);
    }
}