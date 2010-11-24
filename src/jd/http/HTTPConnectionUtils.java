package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class HTTPConnectionUtils {

    private static byte R = (byte) 13;
    private static byte N = (byte) 10;

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
                if (bigbuffer.position() >= 1) {
                    /*
                     * \n only line termination, for fucking buggy non rfc
                     * servers
                     */
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 1) == N) {
                        break;
                    }
                }
                if (bigbuffer.position() >= 2) {
                    /* \r\n, correct line termination */
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 2) == R && bigbuffer.get(position - 1) == N) {
                        break;
                    }
                }
            } else {
                if (bigbuffer.position() >= 4) {
                    /* RNRN for header<->content divider */
                    position = bigbuffer.position();
                    complete = bigbuffer.get(position - 4) == R;
                    complete &= bigbuffer.get(position - 3) == N;
                    complete &= bigbuffer.get(position - 2) == R;
                    complete &= bigbuffer.get(position - 1) == N;
                    if (complete) break;
                }
            }
        }
        bigbuffer.flip();
        return bigbuffer;
    }
}
