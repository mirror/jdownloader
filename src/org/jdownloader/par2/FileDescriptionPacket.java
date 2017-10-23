package org.jdownloader.par2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.appwork.utils.formatter.HexFormatter;

public class FileDescriptionPacket extends Packet {
    public static final byte[]     MAGIC = new byte[] { 'P', 'A', 'R', ' ', '2', '.', '0', '\0', 'F', 'i', 'l', 'e', 'D', 'e', 's', 'c' };
    protected final RawPacket      rawPacket;
    protected static final Charset ASCII = Charset.forName("ASCII");

    /**
     * 16 - The File ID
     *
     * 16 - The MD5 hash of the entire file
     *
     * 16 - The MD5-16k. That is, the MD5 hash of the first 16kB of the file
     *
     * 8 - Length of the file
     *
     * x*4 - Name of the file. This array is not guaranteed to be null terminated! Subdirectories are indicated by an HTML-style '/' (a.k.a.
     * the UNIX slash). The filename must be unique
     *
     * @param rawPacket
     */
    public ByteBuffer getFileID() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 0, 16);
    }

    public ByteBuffer getMD5() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 16, 16);
    }

    public ByteBuffer get16kMD5() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 32, 16);
    }

    public long getLength() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 48, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    @Override
    public String toString() {
        return "FileDescriptionPacket|Name:" + getName() + "|Length:" + getLength() + "|MD5:" + HexFormatter.byteBufferToHex(getMD5()) + "|FileID:" + HexFormatter.byteBufferToHex(getFileID());
    }

    public ByteBuffer getNameAsByteBuffer(final boolean ignoreNullTermination) {
        return getByteBuffer(56, rawPacket.getBody().length - 56, ignoreNullTermination);
    }

    public String getName() {
        return ASCII.decode(getNameAsByteBuffer(false)).toString();
    }

    public FileDescriptionPacket(RawPacket rawPacket) {
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
