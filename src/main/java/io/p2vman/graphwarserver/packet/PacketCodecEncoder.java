package io.p2vman.graphwarserver.packet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class PacketCodecEncoder extends MessageToMessageEncoder<CodecPacket> {
    @Override
    protected void encode(ChannelHandlerContext ctx, CodecPacket msg, List<Object> out) throws Exception {
        out.add(msg.getType()+"&"+msg.getCodec().encode(msg));
    }
}
