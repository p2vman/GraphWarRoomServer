package io.p2vman.graphwarserver.events;

import io.p2vman.graphwarserver.commands.Dispatcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class OnCommandsRegisterEvent extends Event {
    private final @NonNull Dispatcher dispatcher;
}
