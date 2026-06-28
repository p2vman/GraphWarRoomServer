package io.p2vman.graphwarserver.packet.st;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
public class CreateRoomPacket implements Packet {
    @Getter
    @With
    private final @NonNull String roomName;
    @Getter
    @With
    private final int port;
    @Override
    public String serialize() {
        return "108&"+ URLEncoder.encode(roomName, StandardCharsets.UTF_8)+"&"+port;
    }
}
