package io.p2vman.graphwarserver.tracker;

import io.netty.channel.ChannelFuture;
import io.p2vman.graphwarserver.FuncType;

public interface ITracker {
    ChannelFuture hideRoom();
    ChannelFuture showRoom();
    ChannelFuture createRoom(String name);
    void renameRoom(String newName);
    ChannelFuture closeRoom();
    ChannelFuture sendRoomStatus(FuncType type, int online);
    String getName();

    void bind(String name);
    void bind(int port);
}
