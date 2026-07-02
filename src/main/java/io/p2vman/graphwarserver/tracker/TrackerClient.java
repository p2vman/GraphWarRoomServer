package io.p2vman.graphwarserver.tracker;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import io.p2vman.graphwarserver.packet.sc.NoInfoPacket;
import io.p2vman.graphwarserver.packet.Packet;
import io.p2vman.graphwarserver.packet.PacketEncoder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class TrackerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerClient.class);
    private volatile Channel channel = null;
    @Getter
    private volatile long lastReceivedTime;
    @Getter
    private volatile long lastSentTime;
    private final EventLoopGroupType.EventLoopContext ctx;
    private final String ip;
    private final int port;

    @Setter
    @Getter
    private Consumer<TrackerClient> reconnectTrigger = null;

    private ChannelFuture reconnect_furure = null;

    public TrackerClient(String ip, int port, EventLoopGroupType.EventLoopContext ctx) throws IOException {
        this.lastReceivedTime = System.currentTimeMillis();
        this.lastSentTime = System.currentTimeMillis();
        this.ctx = ctx;
        this.ip = ip;
        this.port = port;
        reconnect().awaitUninterruptibly();
        ctx.getWorkerGroup().scheduleAtFixedRate(this::keep, 0, 2, TimeUnit.SECONDS);
    }

    public ChannelFuture login() throws InterruptedException {
        return sendAsyncMessage("23E(S_%24%40)!Xc");
    }

    private void handleMessage(String msg) {
        String[] i = msg.split("&");
        if (Integer.parseInt(i[0])!=10) {
            //LOGGER.debug(msg);
        }
    }

    public void sendMessage(String message)
    {
        String out = message + "\n";

        if (channel != null && channel.isActive())
        {
            lastSentTime = System.currentTimeMillis();
            channel.writeAndFlush(out);
        }
    }

    public ChannelFuture sendAsyncMessage(String message)
    {
        String out = message + "\n";

        if (channel != null && channel.isActive())
        {
            lastSentTime = System.currentTimeMillis();
            return channel.writeAndFlush(out);
        }
        return null;
    }

    public void sendPacket(Packet packet) {
        if (channel != null && channel.isActive()) {
            lastSentTime = System.currentTimeMillis();
            channel.writeAndFlush(packet);
        }
    }

    public ChannelFuture sendAsyncPacket(Packet packet) {
        if (channel != null && channel.isActive()) {
            lastSentTime = System.currentTimeMillis();
            return channel.writeAndFlush(packet);
        }
        return null;
    }

    public synchronized void keep() {
        var time = System.currentTimeMillis();
        if (time - this.lastSentTime > TimeUnit.SECONDS.toMillis(4)) {
            sendPacket(new NoInfoPacket());
        }
        if (!channel.isActive()) {
            LOGGER.warn("Reconnect");
            if (reconnect_furure != null && !reconnect_furure.isSuccess()) return;
            this.reconnect_furure = reconnect().addListener(future -> {
                if (future.isSuccess()) {
                    login().addListener(future1 -> {
                        if (future1.isSuccess()) {
                            if (reconnectTrigger != null) {
                                reconnectTrigger.accept(this);
                            }
                        }
                    });
                }
            });
        }
    }

    @SneakyThrows
    public ChannelFuture reconnect() {
        if (channel != null && channel.isActive()) channel.close();
        this.lastReceivedTime = System.currentTimeMillis();
        this.lastSentTime = System.currentTimeMillis();
        Bootstrap b = new Bootstrap();
        b.group(ctx.getWorkerGroup())
                .channel(ctx.getType().clientSocketCls)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new LineBasedFrameDecoder(65536));
                        p.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        p.addLast(new StringEncoder(StandardCharsets.UTF_8));
                        p.addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                lastReceivedTime = System.currentTimeMillis();
                                TrackerClient.this.handleMessage(msg);
                                if (System.currentTimeMillis() - getLastSentTime() > 4900) {
                                    sendPacket(new NoInfoPacket());
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                LOGGER.error("Throw: ", cause);
                                ctx.close();
                            }
                        });
                        p.addLast(new PacketEncoder());
                    }
                });

        return b.connect(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                this.channel = future.channel();
            } else {
                LOGGER.error("Connection failed", future.cause());
            }
        });
    }

}
