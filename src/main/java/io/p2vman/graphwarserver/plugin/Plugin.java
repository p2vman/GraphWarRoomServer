package io.p2vman.graphwarserver.plugin;

import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.util.EventLoopGroupType;

public interface Plugin {
    void init(EventLoopGroupType.EventLoopContext ctx);
    void onEnable();
    void onServerStarting(BasicServer server);
}
