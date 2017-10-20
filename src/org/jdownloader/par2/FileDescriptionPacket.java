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
    public byte[] getFileID() {
        final byte[] ret = new byte[16];
        System.arraycopy(getRawPacket().getBody(), 0, ret, 0, 16);
        return ret;
    }

    public byte[] getMD5() {
        final byte[] ret = new byte[16];
        System.arraycopy(getRawPacket().getBody(), 16, ret, 0, 16);
        return ret;
    }

    public byte[] get16kMD5() {
        final byte[] ret = new byte[16];
        System.arraycopy(getRawPacket().getBody(), 32, ret, 0, 16);
        return ret;
    }

    public long getLength() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 48, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    @Override
    public String toString() {
        return "FileDescriptionPacket|Name:" + getName() + "|Length:" + getLength() + "|MD5:" + HexFormatter.byteArrayToHex(getMD5()) + "|FileID:" + HexFormatter.byteArrayToHex(getFileID());
    }

    public byte[] getNameAsBytes(final boolean ignoreNullTermination) {
        final RawPacket rawPacket = getRawPacket();
        final byte[] ret;
        if (ignoreNullTermination) {
            ret = new byte[rawPacket.getBody().length - 56];
        } else {
            int length = rawPacket.getBody().length;
            for (int index = 56; index < rawPacket.getBody().length; index++) {
                if (rawPacket.getBody()[index] == 0) {
                    length = index;
                    break;
                }
            }
            ret = new byte[length - 56];
        }
        System.arraycopy(rawPacket.getBody(), 56, ret, 0, ret.length);
        return ret;
    }

    public String getName() {
        return new String(getNameAsBytes(false), ASCII);
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
