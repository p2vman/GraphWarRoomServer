package io.p2vman.graphwarserver.lobby.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.p2vman.graphwarserver.lobby.LobbyPlayer;
import io.p2vman.graphwarserver.lobby.LobbyServer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class LobbyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyServerHandler.class);
    private final @NonNull LobbyPlayer player;
    private final @NonNull LobbyServer server;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String) msg;
        if (message == null || message.isEmpty()) {
            ctx.close();
            return;
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        var channel = ctx.channel();

        if (channel.hasAttr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY)) {
            var attr = channel.attr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY);
            this.server.onPlayerLogOut(attr.get());
            ctx.close();
            return;
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
