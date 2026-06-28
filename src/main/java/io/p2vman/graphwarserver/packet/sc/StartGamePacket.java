package io.p2vman.graphwarserver.packet.sc;

import io.p2vman.graphwarserver.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.List;

@AllArgsConstructor
@Getter
public class StartGamePacket implements Packet {
    private final int num_circles;
    private final @NonNull List<Vector3i> circles;
    private final @NonNull List<Vector2i> soldiers;
    private final int start_player_index;

    @Override
    public String serialize() {
        var m = "22&"+num_circles;
        for (Vector3i circle : circles) {
            m += "&"+circle.x+"&"+circle.y+"&"+circle.z;
        }
        for (Vector2i soldier : soldiers) {
            m += "&"+soldier.x+"&"+soldier.y;
        }
        m+="&"+start_player_index;
        return m;
    }
}
