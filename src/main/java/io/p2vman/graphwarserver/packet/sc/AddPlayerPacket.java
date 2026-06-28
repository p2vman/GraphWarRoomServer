package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.Team;
import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Getter
public class AddPlayerPacket implements Packet {
    @With
    private final int id;
    @With
    private final @NonNull String name;
    @With
    private final @NonNull Team team;
    @With
    private final boolean local;
    private final int num_soldiers;
    private final boolean ready;

    @Override
    public String serialize() {
        return "16&"+ id + "&" + URLEncoder.encode(name, StandardCharsets.UTF_8)+"&"+team.getIndex()+"&"+(local ? 1 : 0)+"&"+num_soldiers+"&"+(ready ? 1 : 0);
    }
}
