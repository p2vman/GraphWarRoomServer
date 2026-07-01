package io.p2vman.graphwarserver.events;

import io.p2vman.graphwarserver.Avatar;
import io.p2vman.graphwarserver.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class OnAsyncChatMessageEvent extends CancelableEvent {
    private final @NonNull Player player;
    @Setter
    private @NonNull String message;
    private final @NonNull Avatar avatar;
}
