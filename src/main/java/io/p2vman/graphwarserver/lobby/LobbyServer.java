package io.p2vman.graphwarserver.lobby;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.p2vman.graphwarserver.packet.PacketEncoder;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

@Getter
public class LobbyServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyServer.class);

    private final int port;
    private final EventLoopGroupType.EventLoopContext ctx;
    private Channel channel;

    public LobbyServer(final int port, final EventLoopGroupType.EventLoopContext ctx) {
        this.port = port;
        this.ctx = ctx;
    }

    public ChannelFuture run() {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(ctx.getBoosGroup(), ctx.getWorkerGroup())
                    .channel(ctx.getType().serverSocketCls)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        protected void initChannel(SocketChannel ch)
                        {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new LineBasedFrameDecoder(65536));
                            p.addLast(new StringDecoder(StandardCharsets.UTF_8));
                            p.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            p.addLast(new PacketEncoder());

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture f = b.bind(port).sync();
            channel = f.channel();
            return channel.closeFuture();
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
        throw new RuntimeException();
    }

    @Override
    public void close() throws Exception {

    }
}
