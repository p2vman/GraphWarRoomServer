package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AddSoldierPacket implements Packet {
    private final int id;

    @Override
    public String serialize() {
        return "17&"+id;
    }
}
