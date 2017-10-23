package org.jdownloader.par2;

import java.nio.ByteBuffer;

public abstract class Packet {
    public abstract byte[] getType();

    public abstract RawPacket getRawPacket();

    protected ByteBuffer getByteBuffer(final int offset, final int len, final boolean ignoreNullTermination) {
        final byte[] body = getRawPacket().getBody();
        if (ignoreNullTermination) {
            return ByteBuffer.wrap(body, offset, len);
        } else {
            final int max = offset + len;
            for (int index = offset; index < max; index++) {
                if (body[index] == 0) {
                    return ByteBuffer.wrap(body, offset, index - offset);
                }
            }
            return ByteBuffer.wrap(body, offset, len);
        }
    }
}
