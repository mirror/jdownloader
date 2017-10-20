package org.jdownloader.par2;

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

    public byte[] getCreatorAsBytes(final boolean ignoreNullTermination) {
        final RawPacket rawPacket = getRawPacket();
        if (ignoreNullTermination) {
            return rawPacket.getBody();
        } else {
            int length = rawPacket.getBody().length;
            for (int index = 0; index < rawPacket.getBody().length; index++) {
                if (rawPacket.getBody()[index] == 0) {
                    length = index;
                    break;
                }
            }
            final byte[] ret = new byte[length];
            System.arraycopy(rawPacket.getBody(), 0, ret, 0, ret.length);
            return ret;
        }
    }

    public String getCreator() {
        return new String(getCreatorAsBytes(false), ASCII);
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
