package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NextTurnPacket implements Packet {
    @Override
    public String serialize() {
        return "25&";
    }
}
