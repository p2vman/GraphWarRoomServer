package io.p2vman.graphwarserver;

import io.netty.channel.EventLoopGroup;
import io.p2vman.graphwarserver.events.OnCommandsRegisterEvent;
import io.p2vman.graphwarserver.events.OnPlayerLoginEvent;
import io.p2vman.graphwarserver.metircs.MetricsClient;
import io.p2vman.graphwarserver.packet.sc.ChatMessagePacket;
import io.p2vman.graphwarserver.plugin.Plugin;
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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
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

    private final List<Plugin> plugins;

    private final Config config;

    public Main() throws Exception {
        File file = new File("./config.yaml");
        if (!file.exists()) {
            Files.copy(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("config.yaml")), file.toPath());
        }

        LoaderOptions options = new LoaderOptions();
        Constructor constructor = new Constructor(Config.class, options);
        Yaml yaml = new Yaml(constructor);

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
            client.login().awaitUninterruptibly();
            TrackerWrapper wrapper = new TrackerWrapper(client);
            int port = pool.bind();
            wrapper.bind(port);
            wrapper.bind(server.name);
            var srv = new BasicServer(port, ctx, wrapper);
            servers.add(srv);
        }

        this.plugins = new ObjectArrayList<>();
        for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
            plugin.init(ctx);
            this.plugins.add(plugin);
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

                for (Plugin plugin : plugins) {
                    LOGGER.info("Load plugin: {}", plugin.getClass().getName());
                    plugin.onServerStarting(server);;
                }
            });
        }

        ctx.getWorkerGroup().scheduleAtFixedRate(() -> {
            MetricsClient.getInstance().run((client -> {
                List<MetricsClient.PointBuilder> points = new ObjectArrayList<>();
                for (BasicServer server : this.servers) {
                    server.sync(() -> {
                        points.add(new MetricsClient.PointBuilder("servers")
                                .addTag("server", Objects.requireNonNull(server.getTracker().getName()))
                                .addField("online", server.players.size())
                                .addField("avatars", server.avatars.size() - server.players.size()));
                    });
                }
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

                points.add(new MetricsClient.PointBuilder("MBean")
                        .addTag("server", this.config.name)
                        .addField("h_usage", memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024))
                        .addField("h_size", memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024))
                        .addField("c_usage", osBean.getSystemLoadAverage() * 100)
                        .addField("t_active", threadBean.getThreadCount())

                );
                client.send(points.toArray(new MetricsClient.PointBuilder[0]));
            }));
        }, 0, 1, TimeUnit.SECONDS);

        while (true) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        }
    }

    @Handler
    public void onCommandsRegister(OnCommandsRegisterEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register("skip", (ctx1, arguments, command) -> {
            var server = ctx1.getServer();
            var player = ctx1.getClient();
            if (server.getState() == GameState.GAME) {
                server.sync(() -> {
                    if (player.contains(BasicServer.PLAYER_GAME_SKIP_ATTRIBUTE_KEY) && player.get(BasicServer.PLAYER_GAME_SKIP_ATTRIBUTE_KEY)) {
                        ctx1.sendMessage("you have already voted for skip");
                        return;
                    }

                    player.put(BasicServer.PLAYER_GAME_SKIP_ATTRIBUTE_KEY, true);
                    ctx1.sendMessage("You voted for skip");
                    for (Player player1 : server.players) {
                        if (!player1.contains(BasicServer.PLAYER_GAME_SKIP_ATTRIBUTE_KEY) || !player1.get(BasicServer.PLAYER_GAME_SKIP_ATTRIBUTE_KEY)) {
                            return;
                        }
                    }
                    for (Player player1 : server.players) {
                        player1.put(BasicServer.PLAYER_GAME_SKIP_ATTRIBUTE_KEY, false);
                    }
                    LOGGER.info("Skip game");
                    server.finishGame();
                });
            } else {
                ctx1.sendMessage("this command works only during the game");
            }
            return 0;
        });
    }

    @Handler
    public void onPlayerLogin(OnPlayerLoginEvent event) {
        event.getPlayer().sendPacketAndFlush(new ChatMessagePacket(-1, "This server is written from scratch, so please report any bugs at https://github.com/p2vman/GraphWarRoomServer"));
        if (config.welcome_messages == null) return;
        for (String welcomeMessage : config.welcome_messages) {
            welcomeMessage = welcomeMessage.trim();
            if (welcomeMessage.isEmpty()) continue;
            event.getPlayer().sendPacketAndFlush(new ChatMessagePacket(-1, welcomeMessage));
        }
    }

    @Override
    public void close() throws Exception {

    }
}
