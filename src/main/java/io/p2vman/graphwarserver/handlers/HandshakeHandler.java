package io.p2vman.graphwarserver.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.Player;
import io.p2vman.graphwarserver.events.OnHandshakeEvent;
import io.p2vman.graphwarserver.packet.sc.ChatMessagePacket;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@AllArgsConstructor
public class HandshakeHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandshakeHandler.class);

    private static final Pattern VALID_NAME_PATTERN =
            Pattern.compile("^[\\p{IsLatin}\\p{IsCyrillic}\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}0-9 _-]+$");


    private static final String[] BANNED_WORDS = {
            "admin", "moder", "server", "support", "system", "root", "dev_", "_dev", "owner",
            "hitler", "gitler", "stalin", "lenin", "mussolini", "himmler", "goebbels",
            "nazi", "ss_", "_ss", "gestapo", "swastika", "fascist", "фашист", "гитлер", "сталин",
            "nigger", "nigga", "niga", "niger", "negr", "blackie", "pidor", "пидор", "негр", "хач",
            "хуй", "хуе", "хуи", "пизд", "ебал", "ебат", "сука", "бля", "гандон", "презерватив",
            "fuck", "bitch", "asshole", "dick", "cunt", "whore", "slut", "bastard"
    };

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

        var name = URLDecoder.decode(n, StandardCharsets.UTF_8).trim();

        LOGGER.info("Handshake: {}", name);
        if (name.length() < 3 || name.length() > 20) {
            LOGGER.warn("Handshake rejected: invalid length ({}) from {}", name.length(), channel.remoteAddress());
            ctx.close();
            return;
        }

        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            LOGGER.warn("Handshake rejected: invalid characters in name '{}' from {}", name, channel.remoteAddress());
            ctx.close();
            return;
        }

        String lowerName = name.toLowerCase(Locale.ROOT);

        if (name.equals("Player") || lowerName.contains("bot") || name.equals("NotMe")) {
            LOGGER.warn("Handshake rejected: default/bot name '{}' from {}", name, channel.remoteAddress());
            ctx.close();
            return;
        }

        for (String banned : BANNED_WORDS) {
            if (lowerName.contains(banned)) {
                LOGGER.warn("Handshake rejected: banned word match '{}' in name '{}' from {}", banned, name, channel.remoteAddress());
                ctx.close();
                return;
            }
        }

        if (!this.server.accept_clients.get()) {
            ctx.close();
            return;
        }

        var event = new OnHandshakeEvent(channel, name);
        this.server.getEventbus().publish(event);
        if (event.isCancel()) {
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
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        var channel = ctx.channel();

        if (channel.hasAttr(Player.PLAYER_ATTRIBUTE_KEY)) {
            var attr = channel.attr(Player.PLAYER_ATTRIBUTE_KEY);
            this.server.onPlayerLogOut(attr.get());
        }
        ctx.close();
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