package io.p2vman.graphwarserver.lobby;

import io.p2vman.graphwarserver.util.EventLoopGroupType;

public class Main {
    public static void main(String[] args) throws Exception {
        var ctx = EventLoopGroupType.getAvailable().asContext(4, 12);


        try (LobbyServer server = new LobbyServer(8080, ctx)) {
            server.run();
        }
    }
}
