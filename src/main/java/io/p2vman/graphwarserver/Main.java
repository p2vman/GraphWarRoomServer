package io.p2vman.graphwarserver;

import com.influxdb.client.write.Point;
import io.netty.channel.EventLoopGroup;
import io.p2vman.graphwarserver.events.OnCommandsRegisterEvent;
import io.p2vman.graphwarserver.events.OnPlayerLoginEvent;
import io.p2vman.graphwarserver.metircs.MetricsClient;
import io.p2vman.graphwarserver.packet.sc.ChatMessagePacket;
import io.p2vman.graphwarserver.tracker.TrackerClient;
import io.p2vman.graphwarserver.tracker.TrackerWrapper;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.engio.mbassy.listener.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Main implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try (Main main = new Main()) {
            main.main();
        }
    }

    @AllArgsConstructor
    public static class CustomThreadFactory implements ThreadFactory {
        private final @NonNull Config.Loop loop;
        private final @NonNull EventLoopGroupType type;
        private int index = 0;

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            synchronized (this) {
                thread.setName(loop.name_pattern
                        .replace("${index}", ""+index++)
                        .replace("${type}", type.name())
                );
            }
            thread.setDaemon(loop.daemon);
            return thread;
        }
    }

    @Getter
    private final EventLoopGroupType type;
    @Getter
    private final EventLoopGroup boss;
    @Getter
    private final EventLoopGroup worker;
    @Getter
    private final PortPool pool;
    @Getter
    private final EventLoopGroupType.EventLoopContext ctx;

    private final ObjectList<BasicServer> servers;

    public Main() throws Exception {
        File file = new File("./config.yaml");
        if (!file.exists()) {
            Files.copy(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("config.yaml")), file.toPath());
        }

        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(Config.class, options);
        Yaml yaml = new Yaml(constructor);

        Config config;
        try (FileReader reader = new FileReader(file)) {
            config = yaml.load(reader);
        }

        type = EventLoopGroupType.getAvailable();
        boss = type.newEventLoop(new CustomThreadFactory(config.eventLops.boos, type, 0), config.eventLops.boos.threads);
        worker = type.newEventLoop(new CustomThreadFactory(config.eventLops.worker, type, 0), config.eventLops.worker.threads);
        ctx = new EventLoopGroupType.EventLoopContext(type, boss, worker);
        MetricsClient.init(config.metrics);
        pool = new PortPool();
        for (Integer port : config.ports) {
            pool.addPort(port);
        }
        servers = new ObjectArrayList<>();
        for (Config.Server server : config.servers) {
            TrackerClient client = new TrackerClient(config.tracker.host, config.tracker.port, ctx);
            client.login();
            TrackerWrapper wrapper = new TrackerWrapper(client);
            int port = pool.bind();
            wrapper.bind(port);
            wrapper.bind(server.name);
            servers.add(new BasicServer(port, ctx, wrapper));
        }
    }

    public void main() throws Exception {
        for (BasicServer server : this.servers) {
            server.getEventbus().subscribe(this);
            server.run((channel) -> {
                var tracker = server.getTracker();
                tracker.createRoom(null).addListener(future -> {
                    tracker.sendRoomStatus(server.funcType, server.avatars.size());
                });
                ctx.getWorkerGroup().scheduleAtFixedRate(() -> {
                    MetricsClient.getInstance().runWithBlocking(blocking -> {
                        Point point = Point.measurement("servers")
                                .addTag("server", Objects.requireNonNull(tracker.getName()))
                                .addField("online", server.players.size())
                                .addField("avatars", server.avatars.size());
                        blocking.writePoint(point);
                    });
                }, 0, 1, TimeUnit.SECONDS);
            });
        }
        while (true) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        }
    }

    @Handler
    public void onCommandsRegister(OnCommandsRegisterEvent event) {
        var dispatcher = event.getDispatcher();

    }

    @Handler
    public void onPlayerLogin(OnPlayerLoginEvent event) {
        event.getPlayer().sendPacketAndFlush(new ChatMessagePacket(-1, "This is a test server written from scratch, so please report any bugs to https://github.com/p2vman/GraphWarRoomServer"));
    }

    @Override
    public void close() throws Exception {

    }
}
