package io.p2vman.graphwarserver.packet;

import io.p2vman.graphwarserver.util.Result;

@FunctionalInterface
public interface Encoder<T> {
    Result<String> encode(T input) throws Exception;
}
