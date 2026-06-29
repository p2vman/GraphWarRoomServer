package io.p2vman.graphwarserver.packet;

import io.p2vman.graphwarserver.util.Result;

@FunctionalInterface
public interface Decoder<T> {
    Result<T> decode(StringInput input) throws Exception;
}
