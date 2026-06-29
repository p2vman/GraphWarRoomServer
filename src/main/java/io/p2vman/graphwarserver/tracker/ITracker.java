package io.p2vman.graphwarserver.tracker;

import io.p2vman.graphwarserver.FuncType;

public interface ITracker {
    void hideRoom();
    void showRoom();
    void createRoom(String name);
    void renameRoom(String newName);
    void closeRoom();
    void sendRoomStatus(FuncType type, int online);

    void bind(int port);
}
