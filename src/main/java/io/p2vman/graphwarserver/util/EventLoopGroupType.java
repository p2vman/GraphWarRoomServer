package io.p2vman.graphwarserver.util;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public enum EventLoopGroupType {
    Nio(NioSocketChannel.class, NioServerSocketChannel.class, (factory, cores) -> new MultiThreadIoEventLoopGroup(cores, factory, NioIoHandler.newFactory()), () -> true),
    Epoll(EpollSocketChannel.class, EpollServerSocketChannel.class, (factory, cores) -> new MultiThreadIoEventLoopGroup(cores, factory, EpollIoHandler.newFactory()), io.netty.channel.epoll.Epoll::isAvailable),
    IoUring(IoUringSocketChannel.class, IoUringServerSocketChannel.class, (factory, cores) -> new MultiThreadIoEventLoopGroup(cores, factory, IoUringIoHandler.newFactory()), io.netty.channel.uring.IoUring::isAvailable),;

    public final Class<? extends ServerSocketChannel> serverSocketCls;
    public final Class<? extends SocketChannel> clientSocketCls;
    private final BiFunction<ThreadFactory, Integer, MultiThreadIoEventLoopGroup> loop_factory;
    private final Supplier<Boolean> isAvailable;

    EventLoopGroupType(@NonNull Class<? extends SocketChannel> clientSocketCls, @NonNull Class<? extends ServerSocketChannel>  loopSocketClass, @NonNull BiFunction<ThreadFactory, Integer, @NonNull MultiThreadIoEventLoopGroup> loop_factory, @NonNull Supplier<Boolean> isAvailable) {
        this.serverSocketCls = loopSocketClass;
        this.loop_factory = loop_factory;
        this.isAvailable = isAvailable;
        this.clientSocketCls = clientSocketCls;
    }

    public MultiThreadIoEventLoopGroup newEventLoop(int cores) {
        return this.newEventLoop(null, cores);
    }

        public MultiThreadIoEventLoopGroup newEventLoop(ThreadFactory factory, int cores) {
        return loop_factory.apply(factory, cores);
    }

    public MultiThreadIoEventLoopGroup newEventLoop() {
        return this.newEventLoop(null, 0);
    }


    public boolean isAvailable() {
        return isAvailable.get();
    }

    public static EventLoopGroupType getAvailable() {
        if (IoUring.isAvailable()) {
            return EventLoopGroupType.IoUring;
        } else if (Epoll.isAvailable()) {
            return EventLoopGroupType.Epoll;
        }
        return EventLoopGroupType.Nio;
    }

    public static EventLoopGroupType getAvailableOf(@NonNull EventLoopGroupType... types) {
        for (EventLoopGroupType type : types) {
            if (type.isAvailable()) return type;
        }
        return EventLoopGroupType.Nio;
    }

    public EventLoopContext asContext(int boos, int worker) {
        return new EventLoopContext(this, this.newEventLoop(boos), this.newEventLoop(worker));
    }

    public EventLoopContext asContext(ThreadFactory factory, int boos, int worker) {
        return new EventLoopContext(this, this.newEventLoop(factory, boos), this.newEventLoop(factory, worker));
    }

    @AllArgsConstructor
    @Getter
    public static class EventLoopContext {
        private final EventLoopGroupType type;
        private final EventLoopGroup boosGroup;
        private final EventLoopGroup workerGroup;
    }
}