package org.jdownloader.par2;

import java.nio.charset.Charset;

public class UnicodeCommentPacket extends Packet {
    public static final byte[]     MAGIC = new byte[] { 'P', 'A', 'R', ' ', '2', '.', '0', '\0', 'C', 'o', 'm', 'm', 'U', 'n', 'i', '\0' };
    protected final RawPacket      rawPacket;
    protected static final Charset UTF16 = Charset.forName("UTF-16");

    /**
     * 16 - If an ASCII comment packet exists in the file and is just a translation of the Unicode in this comment, this is the MD5 Hash of
     * the ASCII comment packet. Otherwise, it is zeros
     *
     * x*4 - The comment. NB: This is not a null terminated string!
     *
     * @param rawPacket
     */
    public UnicodeCommentPacket(RawPacket rawPacket) {
        this.rawPacket = rawPacket;
    }

    @Override
    public String toString() {
        return "UnicodeCommentPacket|Comment:" + getComment();
    }

    public byte[] getAsciiCommentMD5() {
        final byte[] ret = new byte[16];
        System.arraycopy(getRawPacket().getBody(), 0, ret, 0, 16);
        return ret;
    }

    public byte[] getCommentAsBytes(final boolean ignoreNullTermination) {
        final RawPacket rawPacket = getRawPacket();
        if (ignoreNullTermination) {
            return rawPacket.getBody();
        } else {
            int length = rawPacket.getBody().length;
            for (int index = 16; index < rawPacket.getBody().length; index++) {
                if (rawPacket.getBody()[index] == 0) {
                    length = index;
                    break;
                }
            }
            final byte[] ret = new byte[length - 16];
            System.arraycopy(rawPacket.getBody(), 16, ret, 0, ret.length);
            return ret;
        }
    }

    public String getComment() {
        return new String(getCommentAsBytes(false), UTF16);
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
