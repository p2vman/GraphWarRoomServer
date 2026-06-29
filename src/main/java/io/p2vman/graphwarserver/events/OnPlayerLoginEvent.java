package io.p2vman.graphwarserver.events;

import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class OnPlayerLoginEvent extends CancelableEvent {
    private final @NonNull Player player;
    private final @NonNull BasicServer server;
}
