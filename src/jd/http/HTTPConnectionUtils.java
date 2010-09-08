package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import jd.controlling.JDLogger;

public class HTTPConnectionUtils {
    public static InputStream newInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) { return -1; }
                return buf.get();
            }

            public synchronized int read(final byte[] bytes, final int off, int len) throws IOException {
                // Read only what's left
                if (!buf.hasRemaining()) { return -1; }
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }

    public static ByteBuffer readheader(final InputStream in, boolean onlyHTTPHeader) throws IOException {
        ByteBuffer bigbuffer = ByteBuffer.allocateDirect(4096);
        final byte[] minibuffer = new byte[1];
        int position;
        int c;
        boolean complete = false;
        while ((c = in.read(minibuffer)) >= 0) {
            if (bigbuffer.remaining() < 1) {
                final ByteBuffer newbuffer = ByteBuffer.allocateDirect((bigbuffer.capacity() * 2));
                bigbuffer.flip();
                newbuffer.put(bigbuffer);
                bigbuffer = newbuffer;
            }
            if (c > 0) bigbuffer.put(minibuffer);
            if (onlyHTTPHeader) {
                if (bigbuffer.position() >= 2) {
                    position = bigbuffer.position();
                    complete = bigbuffer.get(position - 2) == (byte) 13;
                    complete &= bigbuffer.get(position - 1) == (byte) 10;
                    if (complete) break;
                }
            } else {
                if (bigbuffer.position() >= 4) {
                    position = bigbuffer.position();
                    complete = bigbuffer.get(position - 4) == (byte) 13;
                    complete &= bigbuffer.get(position - 3) == (byte) 10;
                    complete &= bigbuffer.get(position - 2) == (byte) 13;
                    complete &= bigbuffer.get(position - 1) == (byte) 10;
                    if (complete) break;
                }
            }
        }
        bigbuffer.flip();
        return bigbuffer;
    }
}
