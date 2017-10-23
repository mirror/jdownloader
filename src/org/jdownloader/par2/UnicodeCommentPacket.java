package org.jdownloader.par2;

import java.nio.ByteBuffer;
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

    public ByteBuffer getAsciiCommentMD5() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 0, 16);
    }

    public ByteBuffer getCommentAsByteBuffer(final boolean ignoreNullTermination) {
        return getByteBuffer(16, rawPacket.getBody().length - 16, ignoreNullTermination);
    }

    public String getComment() {
        return UTF16.decode(getCommentAsByteBuffer(false)).toString();
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
