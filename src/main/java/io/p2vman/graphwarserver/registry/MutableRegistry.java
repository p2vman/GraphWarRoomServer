package io.p2vman.graphwarserver.registry;

public interface MutableRegistry<T> extends Registry<T> {
    boolean isEmpty();
    boolean add(Identifier identifier, T value);
}