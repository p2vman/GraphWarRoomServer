package io.p2vman.graphwarserver.lobby.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.p2vman.graphwarserver.lobby.LobbyPlayer;
import io.p2vman.graphwarserver.lobby.LobbyServer;
import io.p2vman.graphwarserver.lobby.Room;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class RoomHandler extends ChannelInboundHandlerAdapter {
    private final @NonNull Room room;
    private final @NonNull LobbyPlayer player;
    private final @NonNull LobbyServer server;


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        var channel = ctx.channel();

        if (channel.hasAttr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY)) {
            var attr = channel.attr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY);
            this.server.onPlayerLogOut(attr.get());
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        var channel = ctx.channel();

        if (channel.hasAttr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY)) {
            var attr = channel.attr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY);
            this.server.onPlayerLogOut(attr.get());
            ctx.close();
            return;
        }
        ctx.close();
    }
}
