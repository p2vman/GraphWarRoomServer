package io.p2vman.graphwarserver;

import io.p2vman.graphwarserver.packet.st.RoomStatusPacket;
import io.p2vman.graphwarserver.tracker.ServerStatusTracker;
import io.p2vman.graphwarserver.tracker.TrackerClient;
import io.p2vman.graphwarserver.tracker.TrackerWrapper;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import io.p2vman.graphwarserver.util.TextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        var ctx = EventLoopGroupType.getAvailable().asContext(2, 12);
        var ntb = new TextBuilder()
                .setText("Custom room: ${index}");

        TrackerClient client = new TrackerClient("www.graphwar.com", 23761, ctx);
        client.login();
        var w = new TrackerWrapper(client);


        BasicServer server = new BasicServer(20001, new ServerStatusTracker() {
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
        }, ctx.getBoosGroup(), ctx.getWorkerGroup(), ctx.getType(), w);

        //ntb.setVariable("index", 0).build()

        server.run((channel) -> {
            w.createRoom(ntb
                    .setVariable("index", 0)
                    .build()
            );
            w.sendRoomStatus(server.funcType, server.avatars.size());
        });

    }
}
