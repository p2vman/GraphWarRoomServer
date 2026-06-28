package io.p2vman.graphwarserver;

import lombok.Getter;

public enum Team {
    LEFT(1),
    RIGHT(2);
    @Getter
    private final int index;
    private Team(int index) {
        this.index = index;
    }

    public static Team ofIndex(int index) {
        return switch (index) {
            case 1 -> LEFT;
            case 2 -> RIGHT;
            default -> null;
        };
    }
}
