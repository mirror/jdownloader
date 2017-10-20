package org.jdownloader.par2;

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
    public byte[] getFileID() {
        final byte[] ret = new byte[16];
        System.arraycopy(getRawPacket().getBody(), 0, ret, 0, 16);
        return ret;
    }

    @Override
    public String toString() {
        return "UnicodeFilenamePacket|Name:" + getName() + "|FileID:" + HexFormatter.byteArrayToHex(getFileID());
    }

    public byte[] getNameAsBytes(final boolean ignoreNullTermination) {
        final RawPacket rawPacket = getRawPacket();
        final byte[] ret;
        if (ignoreNullTermination) {
            ret = new byte[rawPacket.getBody().length - 16];
        } else {
            int length = rawPacket.getBody().length;
            for (int index = 16; index < rawPacket.getBody().length; index++) {
                if (rawPacket.getBody()[index] == 0) {
                    length = index;
                    break;
                }
            }
            ret = new byte[length - 16];
        }
        System.arraycopy(rawPacket.getBody(), 16, ret, 0, ret.length);
        return ret;
    }

    public String getName() {
        return new String(getNameAsBytes(false), UTF16);
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
