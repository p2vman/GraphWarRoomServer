package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@AllArgsConstructor
public class ChatMessagePacket implements Packet {
    @With
    private final int player_id;
    @With
    private final @NonNull String message;

    @Override
    public String serialize() {
        return "14&"+player_id+"&"+ URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}
