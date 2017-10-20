package org.jdownloader.par2;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.appwork.utils.formatter.HexFormatter;

/**
 * http://parchive.sourceforge.net/docs/specifications/parity-volume-spec/article-spec.html
 *
 * @author daniel
 *
 */
public class RawPacket {
    public static final byte[] MAGIC = new byte[] { 'P', 'A', 'R', '2', '\0', 'P', 'K', 'T' };
    protected final byte[]     recoverySetId;

    public byte[] getRecoverySetId() {
        return recoverySetId;
    }

    public byte[] getType() {
        return type;
    }

    public byte[] getBody() {
        return body;
    }

    protected final byte[] type;
    protected final byte[] md5;

    public boolean verifyMD5() throws NoSuchAlgorithmException {
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(getRecoverySetId());
        md5.update(getType());
        md5.update(getBody());
        final byte[] verify = md5.digest();
        return Arrays.equals(getMD5(), verify);
    }

    public byte[] getMD5() {
        return md5;
    }

    protected final byte[] body;

    protected RawPacket(final byte[] type, final byte[] md5, final byte[] recoverySetId, final byte[] body) {
        this.type = type;
        this.md5 = md5;
        this.recoverySetId = recoverySetId;
        this.body = body;
    }

    public static RawPacket readNext(final InputStream is) throws IOException, NoSuchAlgorithmException {
        final DataInputStream dis = new DataInputStream(is);
        final int read = dis.read();
        if (read == -1) {
            return null;
        }
        final byte[] magic = new byte[8];
        magic[0] = (byte) (read & 0xff);
        dis.readFully(magic, 1, 7);
        if (!Arrays.equals(MAGIC, magic)) {
            throw new IOException("Magic error!(" + HexFormatter.byteArrayToHex(magic) + ")");
        }
        final byte[] length_Bytes = new byte[8];
        dis.readFully(length_Bytes);
        final long length = ByteBuffer.wrap(length_Bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        if (length % 4 != 0) {
            throw new IOException("Length must be multiple of 4!(" + length + ")");
        }
        final byte[] md5 = new byte[16];
        dis.readFully(md5);
        final byte[] recoverySetId = new byte[16];
        dis.readFully(recoverySetId);
        final byte[] type = new byte[16];
        dis.readFully(type);
        final long bodyLength = length - 64;
        if (bodyLength > Integer.MAX_VALUE) {
            throw new IOException();
        }
        final byte[] body = new byte[(int) bodyLength];
        dis.readFully(body);
        return new RawPacket(type, md5, recoverySetId, body);
    }
}
