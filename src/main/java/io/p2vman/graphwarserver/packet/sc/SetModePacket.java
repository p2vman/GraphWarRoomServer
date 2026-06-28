package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.FuncType;
import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class SetModePacket implements Packet {
    private final @NonNull FuncType mode;

    @Override
    public String serialize() {
        return "33&"+mode.ordinal();
    }
}
