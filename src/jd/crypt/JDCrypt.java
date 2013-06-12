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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jdownloader.logging.LogController;

/**
 * JDCrypt class provides a few easy to use functions to encrypt or decrypt data. Use {@link jd.utils.JDHexUtils} to split Keys in Hex form to byte arrays. ALog
 * AES CBC Mode is used.
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
            return cipher.doFinal(string.getBytes("UTF-8"));
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
            return new String(cipher.doFinal(b), "UTF-8");
        } catch (Exception e) {
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            try {
                final Cipher cipher = Cipher.getInstance("AES/CBC/nopadding");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                return new String(cipher.doFinal(b), "UTF-8");
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

}
