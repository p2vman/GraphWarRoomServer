package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RemoveAvatarPacket implements Packet {
    private final int player_id;

    @Override
    public String serialize() {
        return "29&"+player_id;
    }
}
