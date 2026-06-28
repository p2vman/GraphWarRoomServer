package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Getter
public class FunctionPreviewPacket implements Packet {
    private final int id;
    private final @NonNull String function;
    @Override
    public String serialize() {
        return "44&"+id+"&"+URLEncoder.encode(function, StandardCharsets.UTF_8);
    }
}
