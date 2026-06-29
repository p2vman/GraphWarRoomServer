package io.p2vman.graphwarserver.events;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class OnHandshakeEvent extends CancelableEvent {
    private final @NonNull Channel channel;
    private final @NonNull String name;
}
