package io.p2vman.graphwarserver.packet.st;

import io.p2vman.graphwarserver.packet.Packet;

public class CloseRoomPacket implements Packet {
    @Override
    public String serialize() {
        return "107";
    }
}
