package org.jdownloader.par2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;

public class MainPacket extends Packet {
    public static final byte[] MAGIC = new byte[] { 'P', 'A', 'R', ' ', '2', '.', '0', '\0', 'M', 'a', 'i', 'n', '\0', '\0', '\0', '\0' };
    protected final RawPacket  rawPacket;

    /**
     * 8 - Slice size. Must be a multiple of 4.
     *
     * 4 - Number of files in the recovery set.
     *
     * x*16 - File IDs of all files in the recovery set. (See File Description packet.) These hashes are sorted by numerical value (treating
     * them as 16-byte unsigned integers).
     *
     * x*16 - File IDs of all files in the non-recovery set. (See File Description packet.) These hashes are sorted by numerical value
     * (treating them as 16-byte unsigned integers).
     *
     *
     * @param rawPacket
     */
    public MainPacket(RawPacket rawPacket) {
        this.rawPacket = rawPacket;
    }

    public int getNumberOfRecoveryFiles() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public int getNumberOfNonRecoveryFiles() {
        return (getRawPacket().getBody().length - 12 - (16 * getNumberOfRecoveryFiles())) / 16;
    }

    public long getSliceSize() {
        return ByteBuffer.wrap(getRawPacket().getBody(), 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public Enumeration<ByteBuffer> getRecoveryFileIDs() {
        return new Enumeration<ByteBuffer>() {
            private int       index = 0;
            private final int max   = getNumberOfRecoveryFiles();

            @Override
            public boolean hasMoreElements() {
                return index < max;
            }

            @Override
            public ByteBuffer nextElement() {
                if (hasMoreElements()) {
                    return ByteBuffer.wrap(getRawPacket().getBody(), 12 + (index++ * 16), 16);
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public byte[] getType() {
        return MAGIC;
    }

    @Override
    public String toString() {
        return "MainPacket|SliceSize:" + getSliceSize() + "|NumberOfRecoveryFiles:" + getNumberOfRecoveryFiles() + "|NumberOfNonRecoveryFiles:" + getNumberOfNonRecoveryFiles();
    }

    @Override
    public RawPacket getRawPacket() {
        return rawPacket;
    }
}
