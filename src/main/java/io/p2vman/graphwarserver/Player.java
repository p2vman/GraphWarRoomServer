package io.p2vman.graphwarserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.p2vman.graphwarserver.misc.AttributeKey;
import io.p2vman.graphwarserver.misc.IAttributeMap;
import io.p2vman.graphwarserver.packet.Packet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class Player implements IAttributeMap {
    public static final io.netty.util.AttributeKey<Player> PLAYER_ATTRIBUTE_KEY = io.netty.util.AttributeKey.newInstance("player");
    private final @NonNull String name;
    @ToString.Exclude
    private final @NonNull Channel channel;
    @Setter
    private boolean logout;
    @Setter
    private boolean leader;
    @ToString.Exclude
    private final ReferenceList<Avatar> avatars = new ReferenceArrayList<>();
    @Setter
    private long last_message;
    @Setter
    private boolean readyToNextTurn = false;
    @Setter
    private boolean gameFinished = false;

    private final Object2ObjectMap<AttributeKey<?>, Object> attributes = new Object2ObjectOpenHashMap<>();

    public Player(final @NonNull String name, final @NonNull Channel channel) {
        this.name = name;
        this.channel = channel;
        this.last_message = System.currentTimeMillis();
    }

    public void sendMessageAndFlush(String message)
    {
        this.last_message = System.currentTimeMillis();
        channel.writeAndFlush(message + "\n");
    }

    public ChannelFuture sendAsyncMessage(String message)
    {
        this.last_message = System.currentTimeMillis();
        return channel.writeAndFlush(message + "\n");
    }

    public void sendPacketAndFlush(Packet packet) {
        this.last_message = System.currentTimeMillis();
        channel.writeAndFlush(packet);
    }

    public void sendPacket(Packet packet) {
        this.last_message = System.currentTimeMillis();
        channel.write(packet);
    }

    public ChannelFuture sendAsyncPacket(Packet packet) {
        this.last_message = System.currentTimeMillis();
        return channel.writeAndFlush(packet);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Player p) {
            return p.name.equals(this.name);
        }
        else if (obj instanceof String s) {
            return s.equals(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31;
    }

    public boolean hasAvatar(int id) {
        for (Avatar avatar : avatars) {
            if (avatar.getId() == id) return true;
        }
        return false;
    }

    public void disconnect() {
        channel.close();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(AttributeKey<T> key) {
        return (T) attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T put(AttributeKey<T> key, T value) {
        return (T) attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(AttributeKey<T> key) {
        return (T) attributes.remove(key);
    }

    public boolean contains(AttributeKey<?> key) {
        return attributes.containsKey(key);
    }
}
