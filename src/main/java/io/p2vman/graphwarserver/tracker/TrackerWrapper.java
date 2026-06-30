package io.p2vman.graphwarserver.tracker;

import io.netty.channel.ChannelFuture;
import io.p2vman.graphwarserver.FuncType;
import io.p2vman.graphwarserver.packet.st.CloseRoomPacket;
import io.p2vman.graphwarserver.packet.st.CreateRoomPacket;
import io.p2vman.graphwarserver.packet.st.RoomStatusPacket;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

@Getter
public class TrackerWrapper implements ITracker {
    private final TrackerClient client;
    private String room_name = null;
    private Integer port = null;

    public TrackerWrapper(@NonNull TrackerClient client) {
        this.client = client;
    }

    @Override
    public ChannelFuture hideRoom() {
        return this.closeRoom();
    }

    @Override
    public ChannelFuture showRoom() {
        return this.createRoom(this.room_name);
    }

    @Override
    public ChannelFuture createRoom(String name) {
        if (name != null) {
            this.room_name = name;
        }
        Objects.requireNonNull(port);
        Objects.requireNonNull(room_name);
        return this.client.sendAsyncPacket(new CreateRoomPacket(this.room_name, port));
    }

    @Override
    public void renameRoom(String newName) {
        closeRoom();
        createRoom(newName);
    }

    @Override
    public ChannelFuture closeRoom() {
        return this.client.sendAsyncPacket(new CloseRoomPacket());
    }

    @Override
    public ChannelFuture sendRoomStatus(FuncType type, int online) {
        return this.client.sendAsyncPacket(new RoomStatusPacket(type, online));
    }

    @Override
    public String getName() {
        return room_name;
    }

    @Override
    public void bind(String name) {
        this.room_name = name;
    }

    @Override
    public void bind(int port) {
        this.port = port;
    }
}
