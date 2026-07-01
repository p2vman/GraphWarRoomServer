package io.p2vman.graphwarserver.misc;

public interface IAttributeMap {
    <T> T get(AttributeKey<T> key);
    <T> T put(AttributeKey<T> key, T value);
    <T> T remove(AttributeKey<T> key);
    boolean contains(AttributeKey<?> key);
}
