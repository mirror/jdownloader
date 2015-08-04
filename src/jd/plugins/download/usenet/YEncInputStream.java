package jd.plugins.download.usenet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class YEncInputStream extends InputStream {

    /**
     * http://www.yenc.org/yenc-draft.1.3.txt
     */

    private final InputStream           inputStream;
    private final ByteArrayOutputStream buffer;
    private final long                  size;
    private final String                name;
    private final int                   lineLength;
    private final int                   maxLineLength;
    private final boolean               isMultiPart;

    private final long                  partBegin;

    /**
     * returns the starting points, in bytes, of the block in the original file
     *
     * @return
     */
    public long getPartBegin() {
        return partBegin;
    }

    /**
     * returns the ending points, in bytes, of the block in the original file
     *
     * @return
     */
    public long getPartEnd() {
        return partEnd;
    }

    private final long partEnd;

    /**
     * is the original file multi-part yEnc encoded
     *
     * @return
     */
    public boolean isMultiPart() {
        return isMultiPart;
    }

    /**
     * returns the part-index of multi-part yEnc encoded original file
     *
     * @return
     */
    public int getPartIndex() {
        return partIndex;
    }

    private final int          partIndex;
    private final int          partTotal;
    private final SimpleUseNet client;

    /**
     * returns the number of total parts in a multi-part yEnc encoded original file
     *
     * return -1 for unknown total parts (older yEnc versions)
     *
     * @return
     */
    public int getPartTotal() {
        return partTotal;
    }

    protected YEncInputStream(SimpleUseNet client, ByteArrayOutputStream buffer) throws IOException {
        this.client = client;
        this.inputStream = client.getInputStream();
        this.buffer = buffer;
        String line = new String(buffer.toByteArray(), 0, buffer.size(), "ISO-8859-1");
        if (!line.startsWith("=ybegin")) {
            throw new IOException("missing =ybegin");
        }
        final String lineValue = getValue(line, "line");
        this.lineLength = lineValue != null ? Integer.parseInt(lineValue) : -1;
        maxLineLength = lineLength + 1;
        name = getValue(line, "name");
        final String sizeValue = getValue(line, "size");
        this.size = sizeValue != null ? Long.parseLong(sizeValue) : -1l;
        final String partValue = getValue(line, "part");
        partIndex = partValue != null ? Integer.parseInt(partValue) : -1;
        isMultiPart = partIndex != -1;
        if (isMultiPart) {
            final String totalValue = getValue(line, "total");
            partTotal = totalValue != null ? Integer.parseInt(totalValue) : -1;
        } else {
            partTotal = -1;
        }
        if (isMultiPart) {
            buffer.reset();
            line = client.readLine(buffer);
            if (!line.startsWith("=ypart")) {
                throw new IOException("missing =ypart");
            }
            final String beginValue = getValue(line, "begin");
            partBegin = beginValue != null ? Long.parseLong(beginValue) : -1;
            final String endValue = getValue(line, "end");
            partEnd = endValue != null ? Long.parseLong(endValue) : -1;
        } else {
            partBegin = -1;
            partEnd = -1;
        }
        readNextLine();
    }

    private int     lineIndex    = 0;
    private int     lineSize     = 0;
    private byte    lineBuffer[] = null;
    private boolean eof          = false;
    private String  crc32Value   = null;

    public String getFileCRC32() {
        return crc32Value;
    }

    private String pcrc32Value = null;

    public String getPartCRC32() {
        return pcrc32Value;
    }

    private void readNextLine() throws IOException {
        buffer.reset();
        lineIndex = 0;
        lineSize = client.readLine(inputStream, buffer);
        lineBuffer = buffer.toByteArray();
        if (lineSize >= 5) {
            final boolean yEnd = lineBuffer[0] == (byte) '=' && lineBuffer[1] == (byte) 'y' && lineBuffer[2] == (byte) 'e' && lineBuffer[3] == (byte) 'n' && lineBuffer[4] == (byte) 'd';
            if (yEnd) {
                eof = true;
                parseTrailer();
                return;
            }
        }
        if (lineSize > maxLineLength) {
            throw new IOException("line-size-error " + lineSize + "-" + maxLineLength);
        }
    }

    /**
     * TODO: optimize to use larger reads (readline works with read()) and support read(byte[] b, int off, int len)
     */
    @Override
    public synchronized int read() throws IOException {
        boolean special = false;
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
            if (c == 61) {
                special = true;
            } else {
                if (special) {
                    return ((byte) (c - 106)) & 0xff;
                } else {
                    return ((byte) (c - 42)) & 0xff;
                }
            }
        }
    }

    private void parseTrailer() throws IOException {
        final String trailer = new String(lineBuffer, 0, lineSize, "ISO-8859-1");
        final String sizeValue = getValue(trailer, "size");
        final long size = sizeValue != null ? Long.parseLong(sizeValue) : -1;
        if (isMultiPart()) {
            if (size != getPartSize()) {
                throw new IOException("part-size-error");
            }
            final String partValue = getValue(trailer, "part");
            final int partIndex = partValue != null ? Integer.parseInt(partValue) : -1;
            if (partIndex != getPartIndex()) {
                throw new IOException("part-index-error");
            }
            pcrc32Value = getValue(trailer, "pcrc32");
            crc32Value = getValue(trailer, "crc32");
        } else {
            if (size != getSize()) {
                throw new IOException("size-error");
            }
        }
        buffer.reset();
        client.readLine(inputStream, buffer);
        final String line = new String(buffer.toByteArray(), 0, buffer.size(), "ISO-8859-1");
        if (!".".equals(line)) {
            throw new IOException("missing body termination(.): " + line);
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

    /**
     * returns the complete filesize of the original file
     *
     * @return
     */
    public long getSize() {
        return size;
    }

    /**
     * returns the size of the current part
     *
     * @return
     */
    public long getPartSize() {
        if (isMultiPart) {
            return getPartEnd() - getPartBegin() + 1;
        }
        return -1;
    }

    /**
     * returns the line length of yEnc encoding
     *
     * @return
     */
    public int getLineLength() {
        return lineLength;
    }

    protected String getValue(final String line, final String key) {
        final String search = key + "=";
        final int start = line.indexOf(search);
        final int end;
        if ("name".equals(key)) {
            /* special handling for name(last key/value to allow spaces) */
            end = line.length();
        } else {
            final int index = line.indexOf(" ", start);
            if (index == -1) {
                end = line.length();
            } else {
                end = index;
            }
        }
        if (start != -1) {
            return line.substring(start + search.length(), end);
        }
        return null;
    }
}
