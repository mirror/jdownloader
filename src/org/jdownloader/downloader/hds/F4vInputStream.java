package org.jdownloader.downloader.hds;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import jd.http.URLConnectionAdapter;

/**
 * http://svn.wordrider.net/svn/freerapid-plugins/trunk/src/adobehds/cz/vity/freerapid/plugins/services/adobehds/
 * 
 * 
 */
public class F4vInputStream extends InputStream {

    private final DataInputStream in;
    private int                   dataAvailable = -1;

    private URLConnectionAdapter  urlConnectionAdapter;

    public F4vInputStream(final URLConnectionAdapter urlConnectionAdapter) throws IOException {
        this.urlConnectionAdapter = urlConnectionAdapter;
        this.in = new DataInputStream(urlConnectionAdapter.getInputStream());
    }

    @Override
    public int read() throws IOException {
        final byte[] b = new byte[1];
        final int len = read(b, 0, 1);
        if (len == -1) {
            return -1;
        }
        return b[0] & 0xff;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(final byte[] b, final int off, int len) throws IOException {
        if (dataAvailable == -1) {
            init();
        }
        if (dataAvailable <= 0) {
            return -1;
        }
        len = Math.min(len, dataAvailable);
        final int bytesRead = in.read(b, off, len);
        if (bytesRead == -1) {
            throw new IOException("Unexpected EOF");
        }
        dataAvailable -= bytesRead;
        return bytesRead;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            in.close();
        } finally {
            urlConnectionAdapter.disconnect();
        }
    }

    private void init() throws IOException {
        while (true) {
            // http://download.macromedia.com/f4v/video_file_format_spec_v10_1.pdf
            // 1.3 F4V box format

            // The total size of the box in bytes, including this header. 0 indicates that the box extends until the end of the file
            int totalSize = in.readInt();
            // The type of the box, usually as 4CC
            final int boxType = in.readInt();
            // The box payload immediately follows the box header. The size of the payload in bytes is equal to the total size of the
            // box minus either 8 bytes or 16 bytes, depending on the size of the header.
            // For more information, see section 4.2 of ISO/IEC 14496-12:2008.
            if (totalSize == 1) {
                // IF TotalSize == 1 UI64 The total 64-bit length of the box in bytes, including this header
                totalSize = (int) in.readLong() - 16;
            } else {
                totalSize -= 8;
            }
            System.out.println("Box: " + new String(new byte[] { (byte) (boxType >> 24), (byte) (boxType >> 16), (byte) (boxType >> 8), (byte) (boxType >> 0) }) + " size: " + totalSize);
            if (boxType == F4vBox.MDAT) {
                dataAvailable = totalSize;
                return;
            } else if (boxType == F4vBox.AFRA) {
                // can probably be used to resume a download. contains time byte references
                if (in.skipBytes(totalSize) != totalSize) {
                    throw new EOFException();
                }

            } else {
                System.out.println("SKip");
                if (in.skipBytes(totalSize) != totalSize) {
                    throw new EOFException();
                }
            }
        }

    }

}