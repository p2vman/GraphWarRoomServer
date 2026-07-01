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
    private Channel channel = null;
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

    public TrackerClient(String ip, int port, EventLoopGroupType.EventLoopContext ctx) throws IOException {
        this.lastReceivedTime = System.currentTimeMillis();
        this.lastSentTime = System.currentTimeMillis();
        this.ctx = ctx;
        this.ip = ip;
        this.port = port;
        reconnect();
        ctx.getWorkerGroup().scheduleAtFixedRate(this::keep, 0, 2, TimeUnit.SECONDS);
    }

    public void login() throws InterruptedException {
        var f = sendAsyncMessage("23E(S_%24%40)!Xc");
        if (f != null) {
            f.await();
        }
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
            reconnect();
            if (reconnectTrigger != null) {
                reconnectTrigger.accept(this);
            }
        }
    }

    @SneakyThrows
    public void reconnect() {
        if (channel != null && channel.isActive()) channel.close().awaitUninterruptibly();
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

        ChannelFuture f = b.connect(ip, port);
        try {
            if (!f.await(TimeUnit.SECONDS.toMillis(10))) {
                throw new IOException("Connection timed out");
            }
        } catch (InterruptedException | IOException e) {
            throw new IOException(e);
        }

        if (!f.isSuccess()) {
            throw new IOException(f.cause());
        }

        this.channel = f.channel();
    }

}
