package io.p2vman.graphwarserver.lobby.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.p2vman.graphwarserver.lobby.LobbyPlayer;
import io.p2vman.graphwarserver.lobby.LobbyServer;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

@AllArgsConstructor
public class LobbyHandshakeHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyHandshakeHandler.class);
    private final LobbyServer server;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String) msg;
        if (message == null || message.isEmpty()) {
            ctx.close();
            return;
        }
        var channel = ctx.channel();
        String[] i = message.split("&");
        if (i.length != 2) {
            ctx.close();
            return;
        }
        if (!Objects.equals(i[0], "16")) {
            ctx.close();
            return;
        }
        String n = i[1].trim();
        if (n.isEmpty()) {
            ctx.close();
            return;
        }

        var name = URLDecoder.decode(n, StandardCharsets.UTF_8).trim();

        if (name.length() < 3 || name.length() > 18) {
            LOGGER.warn("Handshake rejected: invalid length ({}) from {}", name.length(), channel.remoteAddress());
            ctx.close();
            return;
        }


        String lowerName = name.toLowerCase(Locale.ROOT);

        if (name.equals("Player") || lowerName.contains("bot") || name.equals("NotMe")) {
            LOGGER.warn("Handshake rejected: default/bot name '{}' from {}", name, channel.remoteAddress());
            ctx.close();
            return;
        }


        var player = new LobbyPlayer(channel, this.server, name);
        if (this.server.onPlayerLogin(player)) {
            ctx.close();
            return;
        }
        channel.attr(LobbyPlayer.LOBBY_PLAYER_ATTRIBUTE_KEY).set(player);
        var pipeline = ctx.pipeline();

        pipeline.remove("handler");
        pipeline.addLast("handler", new LobbyServerHandler(player, this.server));
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
