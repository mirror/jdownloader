package org.jdownloader.plugins.components.firefile;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.io.CipherIOException;
import org.bouncycastle.crypto.io.InvalidCipherTextIOException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;

/**
 * This class most likely copies CipherOutputStream, but includes a method to set the current cipher.
 *
 * @author Niccoo
 *
 */
public class FirefileCipherOutputStream extends FilterOutputStream {
    private BufferedBlockCipher bufferedBlockCipher;
    private StreamCipher        streamCipher;
    private AEADBlockCipher     aeadBlockCipher;
    private final byte[]        oneByte = new byte[1];
    private byte[]              buf;

    /**
     * Constructs a new FirefileCipherOutputStream from an OutputStream and leaves the cipher null. This required to set the cipher using
     * the setCipher() method.
     *
     * @param os
     */
    public FirefileCipherOutputStream(OutputStream os) {
        super(os);
        this.bufferedBlockCipher = null;
    }

    /**
     * Constructs a FirefileCipherOutputStream from an OutputStream and a BufferedBlockCipher.
     */
    public FirefileCipherOutputStream(OutputStream os, BufferedBlockCipher cipher) {
        super(os);
        this.bufferedBlockCipher = cipher;
    }

    /**
     * Constructs a FirefileCipherOutputStream from an OutputStream and a BufferedBlockCipher.
     */
    public FirefileCipherOutputStream(OutputStream os, StreamCipher cipher) {
        super(os);
        this.streamCipher = cipher;
    }

    /**
     * Constructs a FirefileCipherOutputStream from an OutputStream and a AEADBlockCipher.
     */
    public FirefileCipherOutputStream(OutputStream os, AEADBlockCipher cipher) {
        super(os);
        this.aeadBlockCipher = cipher;
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param b
     *            the byte.
     * @throws java.io.IOException
     *             if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        oneByte[0] = (byte) b;
        if (streamCipher != null) {
            out.write(streamCipher.returnByte((byte) b));
        } else {
            write(oneByte, 0, 1);
        }
    }

    /**
     * Writes b.length bytes from the specified byte array to this output stream.
     *
     *
     *
     * The write method of CipherOutputStream calls the write method of three arguments with the three arguments b, 0, and b.length.
     *
     * @param b
     *            the data.
     * @throws java.io.IOException
     *             if an I/O error occurs.
     * @see #write(byte[], int, int)
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes len bytes from the specified byte array starting at offset off to this output stream.
     *
     * @param b
     *            the data.
     * @param off
     *            the start offset in the data.
     * @param len
     *            the number of bytes to write.
     * @throws java.io.IOException
     *             if an I/O error occurs.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        ensureCapacity(len, false);
        if (bufferedBlockCipher != null) {
            int outLen = bufferedBlockCipher.processBytes(b, off, len, buf, 0);
            if (outLen != 0) {
                out.write(buf, 0, outLen);
            }
        } else if (aeadBlockCipher != null) {
            int outLen = aeadBlockCipher.processBytes(b, off, len, buf, 0);
            if (outLen != 0) {
                out.write(buf, 0, outLen);
            }
        } else {
            streamCipher.processBytes(b, off, len, buf, 0);
            out.write(buf, 0, len);
        }
    }

    /**
     * Ensure the ciphertext buffer has space sufficient to accept an upcoming output.
     *
     * @param updateSize
     *            the size of the pending update.
     * @param finalOutput
     *            true iff this the cipher is to be finalised.
     */
    private void ensureCapacity(int updateSize, boolean finalOutput) {
        int bufLen = updateSize;
        if (finalOutput) {
            if (bufferedBlockCipher != null) {
                bufLen = bufferedBlockCipher.getOutputSize(updateSize);
            } else if (aeadBlockCipher != null) {
                bufLen = aeadBlockCipher.getOutputSize(updateSize);
            }
        } else {
            if (bufferedBlockCipher != null) {
                bufLen = bufferedBlockCipher.getUpdateOutputSize(updateSize);
            } else if (aeadBlockCipher != null) {
                bufLen = aeadBlockCipher.getUpdateOutputSize(updateSize);
            }
        }
        if ((buf == null) || (buf.length < bufLen)) {
            buf = new byte[bufLen];
        }
    }

    /**
     * Flushes this output stream by forcing any buffered output bytes that have already been processed by the encapsulated cipher object to
     * be written out.
     *
     *
     *
     * Any bytes buffered by the encapsulated cipher and waiting to be processed by it will not be written out. For example, if the
     * encapsulated cipher is a block cipher, and the total number of bytes written using one of the write methods is less than the cipher's
     * block size, no bytes will be written out.
     *
     * @throws java.io.IOException
     *             if an I/O error occurs.
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Closes this output stream and releases any system resources associated with this stream.
     *
     *
     *
     * This method invokes the doFinal method of the encapsulated cipher object, which causes any bytes buffered by the encapsulated cipher
     * to be processed. The result is written out by calling the flush method of this output stream.
     *
     *
     *
     * This method resets the encapsulated cipher object to its initial state and calls the close method of the underlying output stream.
     *
     * @throws java.io.IOException
     *             if an I/O error occurs.
     * @throws InvalidCipherTextIOException
     *             if the data written to this stream was invalid ciphertext (e.g. the cipher is an AEAD cipher and the ciphertext tag check
     *             fails).
     */
    public void close() throws IOException {
        ensureCapacity(0, true);
        IOException error = null;
        try {
            if (bufferedBlockCipher != null) {
                int outLen = bufferedBlockCipher.doFinal(buf, 0);
                if (outLen != 0) {
                    out.write(buf, 0, outLen);
                }
            } else if (aeadBlockCipher != null) {
                int outLen = aeadBlockCipher.doFinal(buf, 0);
                if (outLen != 0) {
                    out.write(buf, 0, outLen);
                }
            } else if (streamCipher != null) {
                streamCipher.reset();
            }
        } catch (final InvalidCipherTextException e) {
            error = new InvalidCipherTextIOException("Error finalising cipher data", e);
        } catch (Exception e) {
            error = new CipherIOException("Error closing stream: ", e);
        }
        try {
            flush();
            out.close();
        } catch (IOException e) {
            // Invalid ciphertext takes precedence over close error
            if (error == null) {
                error = e;
            }
        }
        if (error != null) {
            throw error;
        }
    }

    /**
     * Sets the cipher to the one passed.
     *
     * @param BufferedBlockCipher
     *            c The cipher to use
     */
    public void setCipher(BufferedBlockCipher c) {
        this.bufferedBlockCipher = c;
    }
}