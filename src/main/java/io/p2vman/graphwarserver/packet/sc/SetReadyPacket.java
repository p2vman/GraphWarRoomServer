package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@Getter
@AllArgsConstructor
public class SetReadyPacket implements Packet {
    @With
    private final int player_id;
    @With
    private final boolean ready;

    @Override
    public String serialize() {
        return "21&"+player_id+"&"+(ready ? 1 : 0);
    }
}
