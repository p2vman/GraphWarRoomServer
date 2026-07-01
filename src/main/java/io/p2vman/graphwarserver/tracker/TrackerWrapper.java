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
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class TrackerWrapper implements ITracker {
    private final TrackerClient client;
    private String room_name = null;
    private Integer port = null;
    private final AtomicBoolean listed = new AtomicBoolean(false);
    private FuncType last_type = FuncType.NORMAL_FUNC;
    private int last_online = 0;

    public TrackerWrapper(@NonNull TrackerClient client) {
        this.client = client;
        this.client.setReconnectTrigger((cl) -> {
            try {
                cl.login();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            createRoom(null).addListener(future -> {
                sendRoomStatus(last_type, last_online);
            });
        });
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
        return this.client.sendAsyncPacket(new CreateRoomPacket(this.room_name, port)).addListener(future -> {
            this.listed.set(true);
        });
    }

    @Override
    public void renameRoom(String newName) {
        closeRoom();
        createRoom(newName);
    }

    @Override
    public ChannelFuture closeRoom() {
        return this.client.sendAsyncPacket(new CloseRoomPacket()).addListener(future -> {
            this.listed.set(false);
        });
    }

    @Override
    public ChannelFuture sendRoomStatus(FuncType type, int online) {
        this.last_online = online;
        this.last_type = type;
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
