package io.p2vman.graphwarserver;

import io.p2vman.graphwarserver.packet.st.ChatMessagePacket;
import io.p2vman.graphwarserver.packet.st.CreateRoomPacket;
import io.p2vman.graphwarserver.packet.st.RoomStatusPacket;
import io.p2vman.graphwarserver.tracker.ServerStatusTracker;
import io.p2vman.graphwarserver.tracker.TrackerClient;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        var ctx = EventLoopGroupType.getAvailable().asContext(4, 12);

        TrackerClient client = new TrackerClient("www.graphwar.com", 23761, ctx);
        client.login();
        BasicServer server = new BasicServer(10000, new ServerStatusTracker() {
            @Override
            public void onPlayerLogin(BasicServer server, Player player) {

            }

            @Override
            public void onPlayerLogOut(BasicServer server, Player player) {

            }

            @Override
            public void changeFuncType(BasicServer server, Player player, FuncType type) {

            }

            @Override
            public void sync(BasicServer server) {
                client.sendPacket(new RoomStatusPacket(server.funcType, server.avatars.size()));
            }
        }, ctx.getBoosGroup(), ctx.getWorkerGroup(), ctx.getType(), client);
        server.run((channel) -> {
            try {
                client.sendAsyncPacket(new CreateRoomPacket("Custom room", 10000)).await();
                client.sendPacket(new RoomStatusPacket(server.funcType, server.avatars.size()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

    }
}
