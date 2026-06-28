package io.p2vman.graphwarserver;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Avatar {
    private final String name;
    private final int id;
    @Setter
    private int num_soldiers;
    @Setter
    private boolean ready;
    @Setter
    private Team team;

    public Avatar(String name, int id) {
        this.name = name;
        this.id = id;
        this.num_soldiers = 1;
        this.ready = false;
    }
}
