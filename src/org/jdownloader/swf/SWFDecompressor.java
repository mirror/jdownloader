package org.jdownloader.swf;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.Inflater;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

public class SWFDecompressor {
    public SWFDecompressor() {
        super();
    }

    public byte[] decompress(String s) { // ~2000ms
        byte[] enc = null;
        URLConnectionAdapter con = null;
        try {
            con = new Browser().openGetConnection(s);
            final InputStream input = con.getInputStream();
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                final byte[] buffer = new byte[512];
                int amount;
                while ((amount = input.read(buffer)) != -1) { // ~1500ms
                    result.write(buffer, 0, amount);
                }
            } finally {
                try {
                    result.close();
                } catch (Throwable e2) {
                }
                enc = result.toByteArray();
            }
        } catch (Throwable e3) {
            e3.getStackTrace();
            return null;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return uncompress(enc);
    }

    /**
     * Strips the uncompressed header bytes from a swf file byte array
     *
     * @param bytes
     *            of the swf
     * @return bytes array minus the uncompressed header bytes
     */
    private byte[] strip(byte[] bytes) {
        byte[] compressable = new byte[bytes.length - 8];
        System.arraycopy(bytes, 8, compressable, 0, bytes.length - 8);
        return compressable;
    }

    private byte[] uncompress(byte[] b) {
        Inflater decompressor = new Inflater();
        decompressor.setInput(strip(b));
        ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length - 8);
        byte[] buffer = new byte[1024];

        try {
            while (true) {
                int count = decompressor.inflate(buffer);
                if (count == 0 && decompressor.finished()) {
                    break;
                } else if (count == 0) {
                    return null;
                } else {
                    bos.write(buffer, 0, count);
                }
            }
        } catch (Throwable t) {
        } finally {
            decompressor.end();
        }

        byte[] swf = new byte[8 + bos.size()];
        System.arraycopy(b, 0, swf, 0, 8);
        System.arraycopy(bos.toByteArray(), 0, swf, 8, bos.size());
        swf[0] = 70; // F
        return swf;
    }

}
