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

    private int     lineIndex    = 0;
    private int     lineSize     = 0;
    private byte    lineBuffer[] = null;
    private boolean eof          = false;

    private void readNextLine() throws IOException {
        buffer.reset();
        lineIndex = 0;
        lineSize = client.readLine(inputStream, buffer);
        lineBuffer = buffer.toByteArray();
        if (lineSize == 1 && lineBuffer[0] == (byte) 96) {
            eof = true;
            parseTrailer();
            return;
        }
    }

    /**
     * TODO: optimize to use larger reads (readline works with read()) and support read(byte[] b, int off, int len)
     */
    @Override
    public synchronized int read() throws IOException {
        while (true) {
            if (eof) {
                return -1;
            } else if (lineIndex == lineSize) {
                readNextLine();
                if (eof) {
                    return -1;
                }
            }
            final int c = lineBuffer[lineIndex++] & 0xff;
            return c;
        }
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
