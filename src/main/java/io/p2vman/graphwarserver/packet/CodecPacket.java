package io.p2vman.graphwarserver.packet;

public interface CodecPacket {
    int getType();
    Codec<CodecPacket> getCodec();
}
