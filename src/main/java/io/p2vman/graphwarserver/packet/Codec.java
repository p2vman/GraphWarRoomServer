package io.p2vman.graphwarserver.packet;

import io.p2vman.graphwarserver.util.Result;
import lombok.AllArgsConstructor;
import lombok.NonNull;


public interface Codec<T> extends Encoder<T>, Decoder<T> {
    static <T> Codec<T> create(@NonNull Encoder<T> encoder, @NonNull Decoder<T> decoder) {
        return new CodecContainer<>(encoder, decoder);
    }

    @AllArgsConstructor
    class CodecContainer<T> implements Codec<T> {
        private final Encoder<T> encoder;
        private final Decoder<T> decoder;

        @Override
        public Result<T> decode(StringInput input) throws Exception {
            return decoder.decode(input);
        }

        @Override
        public Result<String> encode(T input) throws Exception {
            return this.encoder.encode(input);
        }
    }
 }
