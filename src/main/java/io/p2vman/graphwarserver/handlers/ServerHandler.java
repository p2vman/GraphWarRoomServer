package io.p2vman.graphwarserver.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.p2vman.graphwarserver.BasicServer;
import io.p2vman.graphwarserver.Player;
import io.p2vman.graphwarserver.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);
    private final BasicServer server;
    private final Player player;

    public ServerHandler(BasicServer server, Player player) {
        this.server = server;
        this.player = player;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String input_message = (String) msg;
        if (input_message == null) {
            LOGGER.warn("dis re {}" , player.getName());
            this.server.onPlayerLogOut(player);
            ctx.close();
            return;
        }
        Player player = ctx.channel().attr(Player.PLAYER_ATTRIBUTE_KEY).get();
        var channel = ctx.channel();
        String[] i = input_message.split("&");

        LOGGER.info("{}: {} < {}", player.getName(), input_message, i);

        switch (Integer.parseInt(i[0])) {
            case 39:
            {
                LOGGER.warn("dis pe {}" , player.getName());
                this.server.onPlayerLogOut(player);
                ctx.close();
                break;
            }
            case 10:
                break;
            case 31: // chage function mode
            {
                this.server.changeFuncType(player);
                break;
            }
            case 21: //set ready
            {
                int playerID = Integer.parseInt(i[1]);
                boolean ready = Integer.parseInt(i[2]) != 0;
                this.server.setReady(player, playerID, ready);
                break;
            }
            case 29:
            {
                int pid = Integer.parseInt(i[1]);
                this.server.removeAvatar(player, pid);
                break;
            }
            case 16:
            {
                var name = URLDecoder.decode(i[1].trim(), StandardCharsets.UTF_8);
                if (name.isEmpty()) {
                    this.server.sendError(player, "nae");
                    break;
                }
                this.server.addAvatar(player, name);
                break;
            }
            case 14:
            {
                var message = URLDecoder.decode(i[2].trim(), StandardCharsets.UTF_8);
                int pid = Integer.parseInt(i[1]);
                if (message.isEmpty()) {
                    this.server.sendError(player, "mae");
                    break;
                }
                this.server.handleMessageAsync(player, pid, message);
                break;
            }
            case 20:
            {
                int team = Integer.parseInt(i[1]);
                int pid = Integer.parseInt(i[2]);

                this.server.setTeam(player, Team.ofIndex(team), pid);
                break;
            }
            case 44:
            {
                if (i.length > 2) {
                    int pid = Integer.parseInt(i[1]);
                    var function = URLDecoder.decode(i[2].trim(), StandardCharsets.UTF_8);
                    if (function.isEmpty()) {
                        this.server.sendError(player, "fae");
                        break;
                    }
                    this.server.handleFunctionPreview(player, pid, function);
                }
                break;
            }
            case 24:
            {
                if (i.length > 2) {
                    int pid = Integer.parseInt(i[1]);
                    var function = URLDecoder.decode(i[2].trim(), StandardCharsets.UTF_8);
                    if (function.isEmpty()) {
                        this.server.sendError(player, "fae");
                        break;
                    }
                    this.server.fireFunction(player, pid, function);
                }
                break;
            }
            case 27:
            {
                this.server.readyNextTurn(player);
                break;
            }
            case 37: {
                this.server.checkTimeUp(player);
                break;
            }
            case 40: {
                this.server.gameFinished(player);
                break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.server.onPlayerLogOut(player);
        LOGGER.error("Error in handler: ", cause);
        this.server.sendError(player, "eih");
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.ALL_IDLE) {
                LOGGER.warn("dis t {}", player.getName());
                this.server.onPlayerLogOut(player);
                ctx.close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        LOGGER.warn("dis i {}" , player.getName());
        this.server.onPlayerLogOut(player);
        ctx.close();
    }
}
