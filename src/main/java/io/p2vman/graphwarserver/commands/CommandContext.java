package io.p2vman.graphwarserver.commands;

import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.Player;
import io.p2vman.graphwarserver.packet.sc.ChatMessagePacket;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandContext {
    @Getter
    private final Player client;
    @Getter
    private final BasicServer server;

    public CommandContext(@NonNull Player client, @NonNull  BasicServer server) {
        this.client = client;
        this.server = server;
    }

    public void sendMessage(String s) {
        client.sendPacket(new ChatMessagePacket(-1, s));
    }
}
