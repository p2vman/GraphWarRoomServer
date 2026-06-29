package io.p2vman.graphwarserver.packet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PacketEncoder extends MessageToMessageEncoder<Packet> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketEncoder.class);
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, List<Object> out) {
        out.add(msg.serialize()+"\n");
    }
}
