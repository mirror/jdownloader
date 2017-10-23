package org.jdownloader.par2;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.appwork.utils.formatter.HexFormatter;

public class UnicodeFilenamePacket extends Packet {
    public static final byte[]     MAGIC = new byte[] { 'P', 'A', 'R', ' ', '2', '.', '0', '\0', 'U', 'n', 'i', 'F', 'i', 'l', 'e', 'N' };
    protected final RawPacket      rawPacket;
    protected static final Charset UTF16 = Charset.forName("UTF-16");

    /**
     * 16 - The File ID
     *
     * x*4 - The name of the file in Unicode. NB: This is not a null terminated array! This name must obey all the restrictions of the ASCII
     * filename in the File Description packet.
     *
     * @param rawPacket
     */
    public ByteBuffer getFileID() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 0, 16);
    }

    @Override
    public String toString() {
        return "UnicodeFilenamePacket|Name:" + getName() + "|FileID:" + HexFormatter.byteBufferToHex(getFileID());
    }

    public ByteBuffer getNameAsByteBuffer(final boolean ignoreNullTermination) {
        return getByteBuffer(16, getRawPacket().getBody().length - 16, ignoreNullTermination);
    }

    public String getName() {
        return UTF16.decode(getNameAsByteBuffer(false)).toString();
    }

    public UnicodeFilenamePacket(RawPacket rawPacket) {
        this.rawPacket = rawPacket;
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
