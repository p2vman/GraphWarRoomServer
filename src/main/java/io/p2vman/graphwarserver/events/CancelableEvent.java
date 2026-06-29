package io.p2vman.graphwarserver.events;

import lombok.Getter;
import lombok.Setter;

public class CancelableEvent extends Event {
    @Getter
    @Setter
    private boolean cancel;

    public void cancel() {
        this.cancel = true;
    }
}
