package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.Team;
import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.With;

@AllArgsConstructor
@Getter
public class SetTeamPacket implements Packet {
    @With
    private final int id;
    @With
    private final @NonNull Team team;
    @Override
    public String serialize() {
        return "20&"+team.getIndex()+"&"+id;
    }
}
