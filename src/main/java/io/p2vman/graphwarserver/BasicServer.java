package io.p2vman.graphwarserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.p2vman.graphwarserver.card.CardGenerator;
import io.p2vman.graphwarserver.card.StandardCardGenerator;
import io.p2vman.graphwarserver.commands.CommandContext;
import io.p2vman.graphwarserver.commands.Dispatcher;
import io.p2vman.graphwarserver.handlers.HandshakeHandler;
import io.p2vman.graphwarserver.packet.*;
import io.p2vman.graphwarserver.packet.sc.*;
import io.p2vman.graphwarserver.tracker.ServerStatusTracker;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BasicServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicServer.class);
    @Getter
    private EventLoopGroup boss_group;
    @Getter
    private EventLoopGroup worker_group;
    @Getter
    private final int port;
    @Getter
    private Channel channel;

    private static final AtomicInteger playerID = new AtomicInteger(0);
    public final AtomicBoolean accept_clients = new AtomicBoolean(true);

    public final ObjectList<Player> players = new ObjectArrayList<>();
    public final Int2ObjectMap<Avatar> avatars = new Int2ObjectArrayMap<>();

    public FuncType funcType = FuncType.NORMAL_FUNC;
    private final ServerStatusTracker tracker;

    private final Random random;

    private GameState state = GameState.PRE_GAME;

    private long timeTurnStarted;

    private final Dispatcher dispatcher;

    private final CardGenerator generator;

    public BasicServer(int port, ServerStatusTracker tracker) {
        this.port = port;
        this.tracker = tracker;
        this.random = new SecureRandom();
        this.dispatcher = new Dispatcher();
        this.generator = new StandardCardGenerator(random, this);
    }

    public void run(Consumer<Channel> callback) {
        var group = EventLoopGroupType.getAvailable();
        this.boss_group = group.newEventLoop();
        this.worker_group = group.newEventLoop(4);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss_group, worker_group)
                    .channel(group.serverSocketCls)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        protected void initChannel(SocketChannel ch)
                        {
                            ChannelPipeline p = ch.pipeline();

                            //p.addLast(new IdleStateHandler(
                            //        1, 0, 0,
                            //        TimeUnit.SECONDS));

                            p.addLast(new LineBasedFrameDecoder(65536));
                            p.addLast(new StringDecoder(StandardCharsets.UTF_8));
                            p.addLast(new StringEncoder(StandardCharsets.UTF_8));
                            p.addLast(new PacketEncoder());
                            p.addLast("handler", new HandshakeHandler(BasicServer.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture f = b.bind(port).sync();
            channel = f.channel();
            callback.accept(channel);
            worker_group.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
            worker_group.scheduleAtFixedRate(this::keep, 0, 2, TimeUnit.SECONDS);
            channel.closeFuture().sync();
        } catch (Exception e) {
            LOGGER.warn("", e);
            boss_group.close();
            worker_group.close();
        }

    }

    private boolean starting = false;
    private int time = 0;

    public synchronized void tick() {
        try {
            if (avatars.isEmpty()) return;

            if (state == GameState.PRE_GAME) {
                if (!starting) {
                    boolean st = true;
                    for (Avatar value : avatars.values()) {
                        if (!value.isReady()) {
                            st = false;
                            break;
                        }
                    }
                    if (st) {
                        starting = true;
                        time = 5;
                    }
                } else {
                    this.sendPacketAll(new ChatMessagePacket(-1, String.format("Game starting: %s", time)));
                    time--;
                    if (time <= 0) {
                        starting = false;
                        startGame();
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
        }
    }

    private void reorderPlayers() {
        ObjectList<Avatar> team1 = new ObjectArrayList<>();
        ObjectList<Avatar> team2 = new ObjectArrayList<>();

        for (Avatar player : avatars.values()) {
            if (player.getTeam() == Team.LEFT) {
                team1.add(player);
            } else {
                team2.add(player);
            }
        }

        ObjectList<Avatar> reordered = new ObjectArrayList<>(players.size());

        int team1Index = 0;
        int team2Index = 0;

        boolean team1Turn = random.nextBoolean();

        while (team1Index < team1.size() || team2Index < team2.size()) {
            if (team1Turn) {
                if (team1Index < team1.size()) {
                    reordered.add(team1.get(team1Index++));
                } else {
                    reordered.add(team2.get(team2Index++));
                }
            } else {
                if (team2Index < team2.size()) {
                    reordered.add(team2.get(team2Index++));
                } else {
                    reordered.add(team1.get(team1Index++));
                }
            }

            team1Turn = !team1Turn;
        }


        sendPacketAll(new ReorderPacket(reordered));
    }

    public void startGame() {
        accept_clients.set(false);
        LOGGER.info("Starting game: no longer accepting new connections");
        reorderPlayers();

        var b = this.generator.generateCircles();
        var d = this.generator.generateSoldiers(b, this.avatars.values());
        LOGGER.warn("b={},d={}", b, d);

        this.sendPacketAll(new StartGamePacket(b.length,
                Arrays.asList(b),
                Arrays.asList(d), 0));
        state = GameState.GAME;
    }

    public synchronized void keep() {
        var time = System.currentTimeMillis();
        for (Player player : players) {
            if ((player.getLast_message() + TimeUnit.SECONDS.toMillis(4)) < time) {
                player.setLast_message(System.currentTimeMillis());
                player.sendPacketAndFlush(new NoInfoPacket());
            }
        }
    }

    public Avatar addPlayer(Player player) {
        var name = player.getName();
        for (Avatar value : avatars.values()) {
            if (name.equals(value.getName())) {
                player.disconnect();
                return null;
            }
        }
        if (avatars.size() > 10) {
            player.disconnect();
            return null;
        }
        var avatar = new Avatar(name, playerID.getAndIncrement());
        var teams = Team.values();
        avatar.setTeam(teams[random.nextInt(teams.length)]);
        if (avatars.isEmpty()) {
            player.setLeader(true);
        }

        player.getAvatars().add(avatar);
        this.avatars.put(avatar.getId(), avatar);
        {
            for (Avatar a : avatars.values()) if (!a.getName().equals(name)) {
                var packet = new AddPlayerPacket(a.getId(), a.getName(), a.getTeam(), false, a.getNum_soldiers(), a.isReady());
                player.sendPacketAndFlush(packet);
            }
            var packet = new SetModePacket(funcType);
            player.sendPacketAndFlush(packet);
        }
        var packet = new AddPlayerPacket(avatar.getId(), avatar.getName(), avatar.getTeam(), false, avatar.getNum_soldiers(), avatar.isReady());
        this.sendPacketAllExclude(packet, player);
        player.sendPacketAndFlush(packet.withLocal(true));
        return avatar;
    }

    public synchronized boolean onPlayerLogin(Player player) {
        if (this.players.contains(player)) {
            return false;
        }
        this.players.add(player);
        this.tracker.onPlayerLogin(this, player);
        var avatar = this.addPlayer(player);
        player.getAvatars().add(avatar);
        this.tracker.sync(this);
        starting = false;
        return true;
    }

    public synchronized void onPlayerLogOut(Player player) {
        if (player.isLogout()) return;
        this.players.remove(player);
        for (Avatar avatar : player.getAvatars()) {
            this.avatars.remove(avatar.getId());
            this.sendPacketAll(new RemoveAvatarPacket(avatar.getId()));
        }
        LOGGER.info("Player {} log out", player.getName());
        this.tracker.onPlayerLogOut(this, player);
        this.tracker.sync(this);
        player.setLogout(true);
        if (state == GameState.GAME) {
            if (checkTurn()) {
                turn();
            }
        }
    }

    public synchronized void changeFuncType(Player player) {
        funcType = funcType.next();
        for (Avatar avatar : avatars.values())
            if (avatar.isReady()) {
                avatar.setReady(false);
                var packet = new SetReadyPacket(avatar.getId(), false);
                for (Player player2 : players) player2.sendPacketAndFlush(packet);
            }

        var packet = new SetModePacket(funcType);
        for (Player player2 : players) player2.sendPacketAndFlush(packet);
        this.tracker.changeFuncType(this, player, funcType);
        this.tracker.sync(this);
        setAllNoReady();
    }

    public synchronized void setReady(Player player, int avatar_id, boolean ready) {
        var can = player.isLeader() || player.hasAvatar(avatar_id);

        if (can) {
            this.avatars.get(avatar_id).setReady(ready);
            sendPacketAll(new SetReadyPacket(avatar_id, ready));
        }
    }

    public void sendPacketAll(Packet packet) {
        for (Player player : this.players) {
            player.sendPacketAndFlush(packet);
        }
    }

    public void sendPacketAllExclude(Packet packet, Player exclude) {
        for (Player player : this.players) if (player != exclude) {
            player.sendPacketAndFlush(packet);
        }
    }

    public synchronized void removeAvatar(Player player, int id) {
        boolean can = player.isLeader() || player.hasAvatar(id);
        if (can && this.avatars.containsKey(id)) {
            this.avatars.remove(id);
            sendPacketAll(new RemoveAvatarPacket(id));
        }
        setAllNoReady();
    }

    public synchronized void addAvatar(Player player, String name) {
        if (avatars.size() > 10) {
            sendError(player, "rif");
            return;
        }
        for (Avatar value : avatars.values()) {
            if (name.equals(value.getName())) {
                sendError(player, "nae");
                return;
            }
        }
        var avatar = new Avatar(name, playerID.getAndIncrement());
        var teams = Team.values();
        avatar.setTeam(teams[random.nextInt(teams.length)]);
        if (avatars.isEmpty()) {
            player.setLeader(true);
        }
        player.getAvatars().add(avatar);
        this.avatars.put(avatar.getId(), avatar);
        var packet = new AddPlayerPacket(avatar.getId(), avatar.getName(), avatar.getTeam(), false, avatar.getNum_soldiers(), avatar.isReady());

        this.sendPacketAllExclude(packet, player);
        player.sendPacketAndFlush(packet.withLocal(true));

        this.tracker.sync(this);
        setAllNoReady();
    }

    public void sendError(Player player, String message) {
        player.sendPacketAndFlush(new ChatMessagePacket(-1, message));
        LOGGER.warn("error: {}", message);
    }

    public void handleMessageAsync(Player player, int input_id, String message) {
        for (Avatar avatar : player.getAvatars()) {
            if (avatar.getId() == input_id) {
                var ctx = new CommandContext(player, this);
                if (!this.dispatcher.handleCommand(message, ctx)) {
                    this.sendPacketAll(new ChatMessagePacket(input_id, message));
                }
                break;
            }
        }
    }

    public synchronized void setTeam(Player player, Team team, int target) {
        if (team == null) {
            sendError(player, "it");
            return;
        }
        var can = player.isLeader() || player.hasAvatar(target);
        if (can) {
            Avatar avatar = this.avatars.get(target);
            avatar.setTeam(team);
            this.sendPacketAll(new SetTeamPacket(target, team));
        }
        setAllNoReady();
    }

    public synchronized void handleFunctionPreview(Player player, int input_id, String function) {
        if (player.hasAvatar(input_id)) {
            this.sendPacketAll(new FunctionPreviewPacket(input_id, function));
        }
    }

    public synchronized void fireFunction(Player player, int input_id, String function) {
        if (player.hasAvatar(input_id)) {
            this.sendPacketAll(new FireFunctionPacket(input_id, function));
        }
    }

    public synchronized void readyNextTurn(Player player) {
        player.setReadyToNextTurn(true);
        if (checkTurn()) {
            turn();
        }
    }

    public synchronized void checkTimeUp(Player player) {
        if (System.currentTimeMillis() - this.timeTurnStarted > 59999) {
            turn();
        }
    }

    public boolean checkTurn() {
        for (Player player1 : players) {
            if (!player1.isReadyToNextTurn()) return false;
        }
        return true;
    }

    public void turn() {
        for (Player player1 : players) {
            player1.setReadyToNextTurn(false);
        }
        sendPacketAll(new NextTurnPacket());
        this.timeTurnStarted = System.currentTimeMillis();
    }

    public synchronized void gameFinished(Player player) {
        player.setGameFinished(true);
        for (Player player1 : players) {
            if (!player1.isGameFinished()) return;
        }
        for (Player player1 : players) {
            player1.setGameFinished(false);
        }
        sendPacketAll(new GameFinishedPacket());
        setAllNoReady();
        doPreGame();
    }

    public void doPreGame() {
        state = GameState.PRE_GAME;
        accept_clients.set(true);
    }

    public void setAllNoReady() {
        for (Player player : players) {
            for (Avatar avatar : player.getAvatars()) {
                if (avatar.isReady()) {
                    avatar.setReady(false);
                    sendPacketAll(new SetReadyPacket(avatar.getId(), false));
                }
            }
        }
    }
}  
