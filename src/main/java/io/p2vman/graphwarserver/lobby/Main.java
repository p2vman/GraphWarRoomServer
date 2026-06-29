package io.p2vman.graphwarserver.lobby;

import io.p2vman.graphwarserver.FuncType;
import io.p2vman.graphwarserver.tracker.TrackerClient;
import io.p2vman.graphwarserver.tracker.TrackerWrapper;
import io.p2vman.graphwarserver.util.EventLoopGroupType;

public class Main {
    public static void main(String[] args) throws Exception {
        var ctx = EventLoopGroupType.getAvailable().asContext(4, 12);

        TrackerClient client = new TrackerClient("www.graphwar.com", 23761, ctx);
        client.login();
        TrackerWrapper wrapper = new TrackerWrapper(client);


        try (LobbyServer server = new LobbyServer(20002, ctx)) {
            wrapper.bind(20002);
            var future = server.run();
            wrapper.createRoom("");
            wrapper.sendRoomStatus(FuncType.NORMAL_FUNC, 0);
            future.sync();
        }
    }
}
