package io.p2vman.graphwarserver.packet.st;

import io.p2vman.graphwarserver.packet.Packet;

public class QuitPacket implements Packet {
    @Override
    public String serialize() {
        return "106";
    }
}
