package io.p2vman.graphwarserver.packet;

import lombok.Getter;
import lombok.NonNull;

public class StringInput {
    private final String[] values;
    @Getter
    private int index = 0;

    public StringInput(@NonNull String[] input) {
        this.values = input;
    }

    public String read() {
        return values[index++];
    }

    public String peek() {
        return values[index];
    }

    public boolean hasRemaining() {
        return index < values.length;
    }
}
