package io.p2vman.graphwarserver.tracker;

import io.p2vman.graphwarserver.FuncType;
import io.p2vman.graphwarserver.packet.st.CloseRoomPacket;
import io.p2vman.graphwarserver.packet.st.CreateRoomPacket;
import io.p2vman.graphwarserver.packet.st.RoomStatusPacket;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class TrackerWrapper implements ITracker {
    private final TrackerClient client;
    private String room_name;
    private Integer port = null;

    public TrackerWrapper(@NonNull TrackerClient client) {
        this.client = client;
    }

    @Override
    public void hideRoom() {
        this.closeRoom();
    }

    @Override
    public void showRoom() {
        this.createRoom(this.room_name);
    }

    @Override
    public void createRoom(String name) {
        this.room_name = name;
        if (port == null) throw new NullPointerException();
        this.client.sendAsyncPacket(new CreateRoomPacket(this.room_name, port)).awaitUninterruptibly();
    }

    @Override
    public void renameRoom(String newName) {
        closeRoom();
        createRoom(newName);
    }

    @Override
    public void closeRoom() {
        this.client.sendAsyncPacket(new CloseRoomPacket()).awaitUninterruptibly();
    }

    @Override
    public void sendRoomStatus(FuncType type, int online) {
        this.client.sendAsyncPacket(new RoomStatusPacket(type, online)).awaitUninterruptibly();
    }

    @Override
    public void bind(int port) {
        this.port = port;
    }
}
