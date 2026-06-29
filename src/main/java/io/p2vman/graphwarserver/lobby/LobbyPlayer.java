package io.p2vman.graphwarserver.lobby;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.p2vman.graphwarserver.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;


@Getter
public class LobbyPlayer {
    public static final AttributeKey<LobbyPlayer> LOBBY_PLAYER_ATTRIBUTE_KEY = AttributeKey.newInstance("lobby_player");
    private final @NonNull Channel channel;
    private final @NonNull LobbyServer server;
    @Setter
    private Room room;
    private final @NonNull String name;
    @Setter
    private long lastMessage;

    public LobbyPlayer(@NonNull Channel channel, @NonNull LobbyServer server, final @NonNull String name) {
        this.channel = channel;
        this.server = server;
        this.name = name;
        this.lastMessage = System.currentTimeMillis();
        this.room = null;
    }

    public void sendMessage(String message)
    {
        this.lastMessage = System.currentTimeMillis();
        channel.writeAndFlush(message + "\n");
    }

    public ChannelFuture sendFutureMessage(String message)
    {
        this.lastMessage = System.currentTimeMillis();
        return channel.writeAndFlush(message + "\n");
    }


    public void sendPacket(Packet packet) {
        this.lastMessage = System.currentTimeMillis();
        channel.writeAndFlush(packet);
    }

    public ChannelFuture sendFuturePacket(Packet packet) {
        this.lastMessage = System.currentTimeMillis();
        return channel.writeAndFlush(packet);
    }
}
