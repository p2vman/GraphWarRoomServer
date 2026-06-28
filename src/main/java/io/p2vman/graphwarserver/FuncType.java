package io.p2vman.graphwarserver;

public enum FuncType {
    NORMAL_FUNC,
    FST_ODE,
    SND_ODE;

    private static final FuncType[] VALUES = values();

    public FuncType next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public FuncType previous() {
        return VALUES[(ordinal() - 1 + VALUES.length) % VALUES.length];
    }
}
