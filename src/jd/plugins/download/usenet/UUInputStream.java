package jd.plugins.download.usenet;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class UUInputStream extends InputStream {

    /**
     * https://en.wikipedia.org/wiki/Uuencoding
     */

    private final InputStream           inputStream;
    private final ByteArrayOutputStream buffer;
    private final String                name;

    private final SimpleUseNet          client;

    protected UUInputStream(SimpleUseNet client, ByteArrayOutputStream buffer) throws IOException {
        this.client = client;
        this.inputStream = client.getInputStream();
        this.buffer = buffer;
        String line = new String(buffer.toByteArray(), 0, buffer.size(), "ISO-8859-1");
        if (!line.matches("^begin \\d{3} .+")) {
            throw new IOException("missing uuEncode begin");
        }
        name = line.replaceFirst("begin \\d{3} ", "");
        readNextLine();
    }

    private int     lineSize     = 0;
    private byte    lineBuffer[] = null;
    private boolean eof          = false;

    private int     dataIndex    = 0;
    private int     dataLength   = -1;

    private void readNextLine() throws IOException {
        buffer.reset();
        lineSize = client.readLine(inputStream, buffer);
        lineBuffer = buffer.toByteArray();
        if (lineSize == 1) {
            if (lineBuffer[0] == (byte) 96) {
                eof = true;
                parseTrailer();
                return;
            } else {
                throw new IOException("unexpected single byte line");
            }
        }
        dataIndex = 0;
        dataLength = (lineBuffer[0] & 0xff) - 32;
        int writeIndex = 0;
        int readIndex = 1;
        while (true) {
            final int encodedLeft = dataLength - writeIndex;
            if (encodedLeft <= 0) {
                break;
            }
            if (encodedLeft >= 3) {
                final int a = lineBuffer[readIndex] & 0xff;
                final int b = lineBuffer[readIndex + 1] & 0xff;
                final int c = lineBuffer[readIndex + 2] & 0xff;
                final int d = lineBuffer[readIndex + 3] & 0xff;
                final int x = (((a - 32) & 63) << 2) | (((b - 32) & 63) >> 4);
                final int y = (((b - 32) & 63) << 4) | (((c - 32) & 63) >> 2);
                final int z = (((c - 32) & 63) << 6) | (((d - 32) & 63));
                lineBuffer[writeIndex++] = (byte) x;
                lineBuffer[writeIndex++] = (byte) y;
                lineBuffer[writeIndex++] = (byte) z;
            } else {
                if (encodedLeft >= 1) {
                    final int a = lineBuffer[readIndex] & 0xff;
                    final int b = lineBuffer[readIndex + 1] & 0xff;
                    final int x = (((a - 32) & 63) << 2) | (((b - 32) & 63) >> 4);
                    lineBuffer[writeIndex++] = (byte) x;
                }
                if (encodedLeft >= 2) {
                    final int b = lineBuffer[readIndex + 1] & 0xff;
                    final int c = lineBuffer[readIndex + 2] & 0xff;
                    final int y = (((b - 32) & 63) << 4) | (((c - 32) & 63) >> 2);
                    lineBuffer[writeIndex++] = (byte) y;
                }
            }
            readIndex += 4;
        }
    }

    /**
     * TODO: optimize to use larger reads (readline works with read()) and support read(byte[] b, int off, int len)
     */
    @Override
    public synchronized int read() throws IOException {
        if (eof) {
            return -1;
        } else if (dataIndex == dataLength) {
            readNextLine();
            if (eof) {
                return -1;
            }
        }
        final int c = lineBuffer[dataIndex++] & 0xff;
        return c;
    }

    /**
     * TODO: optimize to use larger reads for underlying inputstream
     * 
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException
     */

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eof) {
            return -1;
        } else if (dataIndex == dataLength) {
            readNextLine();
            if (eof) {
                return -1;
            }
        }
        int written = 0;
        while (dataIndex < dataLength && written < len) {
            b[off + written++] = (byte) (lineBuffer[dataIndex++] & 0xff);
        }
        return written;
    }

    private void parseTrailer() throws IOException {
        buffer.reset();
        client.readLine(inputStream, buffer);
        final String line = new String(buffer.toByteArray(), 0, buffer.size(), "ISO-8859-1");
        if (!"end".equals(line)) {
            throw new IOException("missing body termination(end): " + line);
        }
        readBodyEnd();
    }

    private void readBodyEnd() throws IOException {
        while (true) {
            buffer.reset();
            final int size = client.readLine(inputStream, buffer);
            if (size > 0) {
                final String line = new String(buffer.toByteArray(), 0, size, "ISO-8859-1");
                if (!".".equals(line)) {
                    throw new IOException("missing body termination(end): " + line);
                }
                break;
            } else if (size == -1) {
                throw new EOFException();
            }
        }
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * returns the name of the original file
     *
     * @return
     */
    public String getName() {
        return name;
    }

}
