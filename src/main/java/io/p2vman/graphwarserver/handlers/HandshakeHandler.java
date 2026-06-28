package io.p2vman.graphwarserver.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.Player;
import io.p2vman.graphwarserver.packet.sc.ChatMessagePacket;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

@AllArgsConstructor
public class HandshakeHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeHandler.class);
    private final BasicServer server;

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

        var name = URLEncoder.encode(n, StandardCharsets.UTF_8);
        if (name.equals("Player") || name.toLowerCase(Locale.ROOT).contains("bot") || name.equals("NotMe")) {
            ctx.close();
            return;
        }
        LOGGER.info("Handshake: {}", name);
        if (!this.server.accept_clients.get()) {
            ctx.close();
            return;
        }
        var player = new Player(name, channel);
        var pipeline = ctx.pipeline();
        if (!this.server.onPlayerLogin(player)) {
            ctx.close().await();
            return;
        }
        channel.attr(Player.PLAYER_ATTRIBUTE_KEY).set(player);
        pipeline.remove("handler");
        pipeline.addLast("handler", new ServerHandler(server, player));
        for (Player player1 : this.server.players) {
            player1.sendPacketAndFlush(new ChatMessagePacket(-1, "Handshake: " + name));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        var channel = ctx.channel();

        if (channel.hasAttr(Player.PLAYER_ATTRIBUTE_KEY)) {
            var attr = channel.attr(Player.PLAYER_ATTRIBUTE_KEY);
            this.server.onPlayerLogOut(attr.get());
            ctx.close();
            return;
        }
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof IdleStateEvent)
        {
            IdleStateEvent e =
                    (IdleStateEvent) evt;

            var channel = ctx.channel();

            if (channel.hasAttr(Player.PLAYER_ATTRIBUTE_KEY)) {
                var attr = channel.attr(Player.PLAYER_ATTRIBUTE_KEY);
                this.server.onPlayerLogOut(attr.get());
                ctx.close();
                return;
            }
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        var channel = ctx.channel();

        if (channel.hasAttr(Player.PLAYER_ATTRIBUTE_KEY)) {
            var attr = channel.attr(Player.PLAYER_ATTRIBUTE_KEY);
            this.server.onPlayerLogOut(attr.get());
            ctx.close();
            return;
        }
        ctx.close();
    }
}
