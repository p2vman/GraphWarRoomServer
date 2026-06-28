package io.p2vman.graphwarserver;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.p2vman.graphwarserver.packet.Packet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class Player {
    public static final AttributeKey<Player> PLAYER_ATTRIBUTE_KEY = AttributeKey.newInstance("player");
    private final String name;
    @ToString.Exclude
    private final Channel channel;
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

    public Player(final String name, final Channel channel) {
        this.name = name;
        this.channel = channel;
        this.last_message = System.currentTimeMillis();
    }

    public void sendMessageAndFlush(String message)
    {
        String out = message + "\n";

        if (channel != null)
        {
            this.last_message = System.currentTimeMillis();
            channel.writeAndFlush(out);
        }
    }

    public ChannelFuture sendAsyncMessage(String message)
    {
        String out = message + "\n";

        if (channel != null)
        {
            this.last_message = System.currentTimeMillis();
            return channel.writeAndFlush(out);
        }
        return null;
    }

    public void sendPacketAndFlush(Packet packet) {
        if (channel != null) {
            this.last_message = System.currentTimeMillis();
            channel.writeAndFlush(packet);
        }
    }

    public void sendPacket(Packet packet) {
        if (channel != null) {
            this.last_message = System.currentTimeMillis();
            channel.write(packet);
        }
    }

    public ChannelFuture sendAsyncPacket(Packet packet) {
        if (channel != null) {
            this.last_message = System.currentTimeMillis();
            return channel.writeAndFlush(packet);
        }
        return null;
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
        System.out.println(StackWalker.getInstance().getCallerClass());
        channel.close();
    }
}
