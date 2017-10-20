package org.jdownloader.par2;

import java.nio.charset.Charset;

public class AsciiCommentPacket extends Packet {
    public static final byte[]     MAGIC = new byte[] { 'P', 'A', 'R', ' ', '2', '.', '0', '\0', 'C', 'o', 'm', 'm', 'A', 'S', 'C', 'I' };
    protected final RawPacket      rawPacket;
    protected static final Charset ASCII = Charset.forName("ASCII");

    /**
     * x*4 - ASCII The comment. NB: This is not a null terminated string!
     *
     * @param rawPacket
     */
    public AsciiCommentPacket(RawPacket rawPacket) {
        this.rawPacket = rawPacket;
    }

    @Override
    public String toString() {
        return "AsciiCommentPacket|Comment:" + getComment();
    }

    public byte[] getCommentAsBytes(final boolean ignoreNullTermination) {
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

    public String getComment() {
        return new String(getCommentAsBytes(false), ASCII);
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
