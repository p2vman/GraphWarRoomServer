package io.p2vman.graphwarserver.tracker;

import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.FuncType;
import io.p2vman.graphwarserver.Player;

public interface ServerStatusTracker {
    void onPlayerLogin(BasicServer server, Player player);
    void onPlayerLogOut(BasicServer server, Player player);
    void changeFuncType(BasicServer server, Player player, FuncType type);
    void sync(BasicServer server);
}
