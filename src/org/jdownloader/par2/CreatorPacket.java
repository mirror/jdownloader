package org.jdownloader.par2;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class CreatorPacket extends Packet {
    public static final byte[]     MAGIC = new byte[] { 'P', 'A', 'R', ' ', '2', '.', '0', '\0', 'C', 'r', 'e', 'a', 't', 'o', 'r', '\0' };
    protected final RawPacket      rawPacket;
    protected static final Charset ASCII = Charset.forName("ASCII");

    /**
     * x*4 - ASCII text identifying the client. This should also include a way to contact the client's creator - either through a URL or an
     * email address. NB: This is not a null terminated string!
     *
     * @param rawPacket
     */
    public CreatorPacket(RawPacket rawPacket) {
        this.rawPacket = rawPacket;
    }

    @Override
    public String toString() {
        return "CreatorPacket|Creator:" + getCreator();
    }

    public ByteBuffer getCreatorAsByteBuffer(final boolean ignoreNullTermination) {
        return getByteBuffer(0, rawPacket.getBody().length, ignoreNullTermination);
    }

    public String getCreator() {
        return ASCII.decode(getCreatorAsByteBuffer(false)).toString();
    }

    @Override
    public byte[] getType() {
        return MAGIC;
    }

    @Override
    public RawPacket getRawPacket() {
        return rawPacket;
    }
}
