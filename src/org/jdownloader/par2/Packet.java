package org.jdownloader.par2;

public abstract class Packet {
    public abstract byte[] getType();

    public abstract RawPacket getRawPacket();
}
