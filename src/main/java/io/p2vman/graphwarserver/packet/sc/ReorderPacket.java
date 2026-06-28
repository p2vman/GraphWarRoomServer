package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.Avatar;
import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Setter
public class ReorderPacket implements Packet {
    private final List<Avatar> players;

    @Override
    public String serialize() {
        var avatar = "43";
        for (Avatar player : players) {
            avatar += "&" + player.getId();
        }
        return avatar;
    }
}
