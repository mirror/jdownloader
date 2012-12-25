//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.crypt;

import java.io.InputStream;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jdownloader.logging.LogController;

import sun.security.pkcs.PKCS7;

/**
 * JDCrypt class provides a few easy to use functions to encrypt or decrypt data. Use {@link jd.utils.JDHexUtils} to split Keys in Hex form
 * to byte arrays. ALog AES CBC Mode is used.
 * 
 * @author unknown
 * 
 */
public final class JDCrypt {

    /**
     * Don't let anyone instantiate this class.
     */
    private JDCrypt() {
    }

    /**
     * Encrypt a string with the signature of jdownloader (128 bit)
     * 
     * @param string
     *            String to encrypt
     * @return
     */
    public static byte[] encrypt(final String string) {
        return encrypt(string, sign());
    }

    /**
     * Decrypt data with JDs signature (128 bit)
     * 
     * @param b
     *            data tu decrypt
     * @return
     */
    public static String decrypt(final byte[] b) {
        return decrypt(b, sign());

    }

    /**
     * Encrypts a String
     * 
     * @param string
     *            data to encrypt
     * @param key
     *            Key for encryption. Use 128 Bit (16 Byte) key
     * @return
     */
    public static byte[] encrypt(final String string, final byte[] key) {
        return encrypt(string, key, key);
    }

    /**
     * Encrypts a string
     * 
     * @param string
     *            String to encrypt
     * @param key
     *            to use (128Bit (16 Byte))
     * @param iv
     *            to use (128Bit (16 Byte))
     * @return
     */
    public static byte[] encrypt(final String string, final byte[] key, final byte[] iv) {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(string.getBytes());
        } catch (Exception e) {
            LogController.CL().log(e);
        }
        return null;
    }

    /**
     * Decrypt data which has been encrypted width {@link JDCrypt#encrypt(String, byte[], byte[])}
     * 
     * @param b
     *            data to decrypt
     * @param key
     *            to use (128Bit (16 Byte))
     * @param iv
     *            to use (128Bit (16 Byte))
     * @return
     */
    public static String decrypt(final byte[] b, final byte[] key, final byte[] iv) {
        try {
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b));
        } catch (Exception e) {
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            try {
                final Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                return new String(cipher.doFinal(b));
            } catch (Exception e1) {
                LogController.CL().log(e);
            }
        }
        return null;
    }

    /**
     * Decrypts data which has been encrypted with {@link JDCrypt#encrypt(String, byte[])}
     * 
     * @param b
     *            data to decrypt
     * @param key
     *            to use (128 Bit/16 Byte)
     * @return
     */
    public static String decrypt(final byte[] b, final byte[] key) {
        return decrypt(b, key, key);
    }

    /**
     * Returns the hash of the JD signature (1287 bit/16 Byte)
     * 
     * @return
     */
    private static byte[] sign() {
        final byte[] buf = new byte[12 * 1024];
        byte[] buffer = new byte[0];
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();

        final InputStream in = cl.getResourceAsStream(new String(new byte[] { 77, 69, 84, 65, 45, 73, 78, 70, 47, 74, 68, 79, 87, 78, 76, 79, 65, 46, 68, 83, 65 }));
        if (in != null) {
            try {
                int total = 0;

                while (true) {
                    int numRead = in.read(buf, total, buf.length - total);
                    if (numRead <= 0) {
                        break;
                    }
                    total += numRead;
                }
                final byte[] tmp = new byte[total];
                System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                System.arraycopy(buf, 0, tmp, buffer.length, total);
                buffer = tmp;
            } catch (final Exception e) {
            } finally {
                try {
                    in.close();
                } catch (Throwable e) {
                }
            }
        }
        try {
            return MessageDigest.getInstance("MD5").digest((new PKCS7(buffer)).getCertificates()[0].getSignature());
        } catch (Exception e) {
            LogController.CL().log(e);
        }
        return new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    }

}
