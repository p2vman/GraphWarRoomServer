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
import io.p2vman.graphwarserver.events.Event;
import io.p2vman.graphwarserver.events.OnCommandsRegisterEvent;
import io.p2vman.graphwarserver.events.OnPlayerLoginEvent;
import io.p2vman.graphwarserver.handlers.HandshakeHandler;
import io.p2vman.graphwarserver.packet.*;
import io.p2vman.graphwarserver.packet.sc.*;
import io.p2vman.graphwarserver.tracker.ITracker;
import io.p2vman.graphwarserver.util.EventLoopGroupType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BasicServer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicServer.class);
    @Getter
    private final EventLoopGroup boss_group;
    @Getter
    private final EventLoopGroup worker_group;
    @Getter
    private final int port;
    @Getter
    private Channel channel;

    private static final AtomicInteger playerID = new AtomicInteger(0);
    public final AtomicBoolean accept_clients = new AtomicBoolean(true);

    public final ObjectList<Player> players = new ObjectArrayList<>();

    public final ObjectList<Avatar> avatars = new ObjectArrayList<>();

    public FuncType funcType = FuncType.NORMAL_FUNC;

    private final Random random;

    private GameState state = GameState.PRE_GAME;

    private long timeTurnStarted;

    private final Dispatcher dispatcher;

    private final CardGenerator generator;

    private final EventLoopGroupType type;

    @Getter
    private final MBassador<Event> eventbus;
    @Getter
    private final ITracker tracker;

    public BasicServer(int port, EventLoopGroupType.EventLoopContext ctx, ITracker tapi) {
        this.eventbus = new MBassador<>(new BusConfiguration()
                .addFeature(Feature.SyncPubSub.Default())
                .addFeature(Feature.AsynchronousHandlerInvocation.Default())
                .addFeature(Feature.AsynchronousMessageDispatch.Default())
                .addPublicationErrorHandler(error -> {
                    LOGGER.error("Exception in event handler {}#{}", error.getHandler().getDeclaringClass().getName(), error.getHandler().getName(), error.getCause());
                }));
        this.port = port;;
        this.random = new SecureRandom();
        this.dispatcher = new Dispatcher();
        this.generator = new StandardCardGenerator(random, this);
        this.boss_group = ctx.getBoosGroup();
        this.worker_group = ctx.getWorkerGroup();
        this.type = ctx.getType();
        this.tracker = tapi;
    }

    public ChannelFuture run(Consumer<Channel> callback) {

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss_group, worker_group)
                    .channel(type.serverSocketCls)
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
                            p.addLast("handler", new HandshakeHandler(BasicServer.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture f = b.bind(port).sync();
            channel = f.channel();
            this.tracker.bind(port);
            callback.accept(channel);
            worker_group.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
            worker_group.scheduleAtFixedRate(this::keep, 0, 2, TimeUnit.SECONDS);
            this.eventbus.post(new OnCommandsRegisterEvent(this.dispatcher));
            return channel.closeFuture();
        } catch (Exception e) {
            LOGGER.warn("", e);
        }
        throw new RuntimeException();
    }

    private boolean starting = false;
    private int time = 0;

    public synchronized void tick() {
        try {
            if (avatars.isEmpty() || avatars.size()<2) {
                if (state == GameState.GAME) {
                    finishGame();
                }
                return;
            }

            if (state == GameState.PRE_GAME) {
                if (!starting) {
                    boolean st = true;
                    for (Avatar value : avatars) {
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
        ObjectList<Avatar> newAvatars = new ObjectArrayList<>();
        Team currentTeam = Team.LEFT;

        if (random.nextBoolean()) {
            currentTeam = Team.RIGHT;
        }

        ObjectList<Avatar> tempAvatars = new ObjectArrayList<>(avatars);

        while (!tempAvatars.isEmpty()) {
            ListIterator<Avatar> pitr = tempAvatars.listIterator();
            boolean found = false;

            while (pitr.hasNext()) {
                Avatar avatar = pitr.next();

                if (avatar.getTeam() == currentTeam) {
                    pitr.remove();
                    newAvatars.add(avatar);
                    currentTeam = (currentTeam == Team.LEFT) ? Team.RIGHT : Team.LEFT;
                    found = true;
                }
            }

            if (!found) {
                currentTeam = (currentTeam == Team.LEFT) ? Team.RIGHT : Team.LEFT;
            }
        }

        avatars.clear();
        avatars.addAll(newAvatars);

        sendPacketAll(new ReorderPacket(avatars));
    }

    public void startGame() {
        accept_clients.set(false);
        LOGGER.info("Starting game: no longer accepting new connections");
        reorderPlayers();

        var b = this.generator.generateCircles();
        var d = this.generator.generateSoldiers(b, this.avatars);
        LOGGER.warn("b={},d={}", b, d);

        tracker.hideRoom();
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
        for (Avatar value : avatars) {
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
        this.avatars.add(avatar);
        {
            for (Avatar a : avatars) if (!a.getName().equals(name)) {
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
        if (this.players.contains(player) || this.avatars.size() >= 10) {
            return false;
        }
        this.players.add(player);
        this.eventbus.publish(new OnPlayerLoginEvent(player, this));
        var avatar = this.addPlayer(player);
        if (avatar != null) {
            player.getAvatars().add(avatar);
        }
        sendStatus();
        setAllNoReady();
        //player.sendPacket(new Ch);
        return true;
    }

    public void sendStatus() {
        this.tracker.sendRoomStatus(funcType, avatars.size());
    }

    public synchronized void onPlayerLogOut(Player player) {
        if (player.isLogout()) return;
        this.players.remove(player);
        for (Avatar avatar : player.getAvatars()) {
            this.avatars.remove(avatar);
            this.sendPacketAll(new RemoveAvatarPacket(avatar.getId()));
        }
        LOGGER.info("Player {} log out", player.getName());
        sendStatus();
        player.setLogout(true);
        if (state == GameState.GAME) {
            if (checkTurn()) {
                turn();
            }
        }
        setAllNoReady();
    }

    public synchronized void changeFuncType(Player player) {
        funcType = funcType.next();
        for (Avatar avatar : avatars)
            if (avatar.isReady()) {
                avatar.setReady(false);
                var packet = new SetReadyPacket(avatar.getId(), false);
                for (Player player2 : players) player2.sendPacketAndFlush(packet);
            }

        var packet = new SetModePacket(funcType);
        for (Player player2 : players) player2.sendPacketAndFlush(packet);
        sendStatus();
        setAllNoReady();
    }

    public synchronized void setReady(Player player, int avatar_id, boolean ready) {
        var can = player.isLeader() || player.hasAvatar(avatar_id);

        if (can) {
            for (Avatar avatar : avatars) {
                if (avatar.getId() == avatar_id) {
                    avatar.setReady(ready);
                    sendPacketAll(new SetReadyPacket(avatar_id, ready));
                    break;
                }
            }
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
        if (can) {
            avatars.removeIf(avatar -> avatar.getId() == id);
            sendPacketAll(new RemoveAvatarPacket(id));
        }
        setAllNoReady();
    }

    public synchronized void addAvatar(Player player, String name) {
        if (avatars.size() > 10) {
            sendError(player, "rif");
            return;
        }
        for (Avatar value : avatars) {
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
        this.avatars.add(avatar);
        var packet = new AddPlayerPacket(avatar.getId(), avatar.getName(), avatar.getTeam(), false, avatar.getNum_soldiers(), avatar.isReady());

        this.sendPacketAllExclude(packet, player);
        player.sendPacketAndFlush(packet.withLocal(true));

        sendStatus();
        setAllNoReady();
    }

    public void sendError(Player player, String message) {
        player.sendPacketAndFlush(new ChatMessagePacket(-1, message));
        LOGGER.warn("error: {}", message);
    }

    public void handleMessageAsync(Player player, int input_id, String message) {
        for (Avatar avatar : player.getAvatars()) {
            if (avatar.getId() == input_id) {
                LOGGER.info("[{}:{}]: {}", player.getName(), input_id, message);
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
            for (Avatar avatar : avatars) {
                if (avatar.getId() == target) {
                    avatar.setTeam(team);
                    this.sendPacketAll(new SetTeamPacket(target, team));
                    break;
                }
            }
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
        finishGame();
    }

    public void finishGame() {
        sendPacketAll(new GameFinishedPacket());
        setAllNoReady();
        doPreGame();
    }

    public void doPreGame() {
        state = GameState.PRE_GAME;
        accept_clients.set(true);
        tracker.hideRoom().addListener(f -> {
            tracker.showRoom().addListener(future -> {
                if (future.isSuccess()) {
                    tracker.sendRoomStatus(funcType, avatars.size());
                }
            });
        });
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
        starting = false;
    }

    public synchronized void addSoldier(Player player, int id) {
        if (player.hasAvatar(id) || player.isLeader()) {
            var avatar = getAvatarById(id);
            if (avatar != null) {
                if (avatar.getNum_soldiers()<4) {
                    avatar.setNum_soldiers(avatar.getNum_soldiers()+1);
                    sendPacketAll(new AddSoldierPacket(id));
                }
            }
        }
    }

    public Avatar getAvatarById(int id) {
        for (Avatar avatar : avatars) {
            if (avatar.getId() == id) return avatar;
        }
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}