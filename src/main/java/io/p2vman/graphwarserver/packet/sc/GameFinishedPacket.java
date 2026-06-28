package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;

public class GameFinishedPacket implements Packet {

    @Override
    public String serialize() {
        return "40&";
    }
}
