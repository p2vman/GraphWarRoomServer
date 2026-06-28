package io.p2vman.graphwarserver.packet.st;

import io.p2vman.graphwarserver.FuncType;
import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class RoomStatusPacket implements Packet {
    private final @NonNull FuncType type;
    private final int players;


    @Override
    public String serialize() {
        return "105&"+type.ordinal()+"&"+players;
    }
}
