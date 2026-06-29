package io.p2vman.graphwarserver.packet.st;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Getter
public class ChatMessagePacket implements Packet {
    private final @NonNull String message;

    @Override
    public String serialize() {
        return "102&"+ URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}
