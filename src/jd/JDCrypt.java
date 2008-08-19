//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd;

/*
 import javax.crypto.Cipher;
 import javax.crypto.SecretKey;
 import javax.crypto.SecretKeyFactory;
 import javax.crypto.spec.PBEKeySpec;
 import javax.crypto.spec.PBEParameterSpec;

 import sun.misc.BASE64Decoder;
 import sun.misc.BASE64Encoder;
 */
/**
 * EInfache verschlüsselung für die Linklisten
 * 
 * @author astaldo
 * 
 */
public class JDCrypt {
    /*
     * private final static transient String password = "jDownloader"; private
     * final static transient byte[] salt = { (byte) 0xc9,(byte) 0xc9, (byte)
     * 0xc9, (byte) 0xc9, (byte) 0xc9, (byte) 0xc9,(byte) 0xc9, (byte) 0xc9};
     * private static Cipher encryptCipher; private static Cipher decryptCipher;
     * private static BASE64Encoder encoder = new sun.misc.BASE64Encoder();
     * private static BASE64Decoder decoder = new sun.misc.BASE64Decoder(); /**
     * Verwendete Zeichendecodierung private static String charset = "UTF16";
     */

    /**
     * Initialisiert den Verschlüsselungsmechanismus
     * 
     * @param pass
     *            char[]
     * @param salt
     *            byte[]
     * @param iterations
     *            int
     * @throws SecurityException
     */
    /*
     * private static void init() throws SecurityException { try { final
     * PBEParameterSpec ps = new PBEParameterSpec(salt, 20); final
     * SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
     * final SecretKey k = kf.generateSecret(new
     * PBEKeySpec(password.toCharArray())); encryptCipher =
     * Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
     * encryptCipher.init(Cipher.ENCRYPT_MODE, k, ps); decryptCipher =
     * Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
     * decryptCipher.init(Cipher.DECRYPT_MODE, k, ps); } catch (Exception e) {
     * throw new SecurityException("Could not initialize CryptoLibrary: " +
     * e.getMessage()); } }
     */

    /**
     * Entschlüsselt eine Zeichenkette, welche mit der Methode encrypt
     * verschlüsselt wurde.
     * 
     * @param str
     *            Description of the Parameter
     * @return String the encrypted string.
     * @exception SecurityException
     *                Description of the Exception
     */
    public static String decrypt(String str) throws SecurityException {
        return str;
    }

    /**
     * Verschlüsselt eine Zeichenkette
     * 
     * @param str
     *            Description of the Parameter
     * @return String the encrypted string.
     * @exception SecurityException
     *                Description of the Exception
     */
    // encryption is temp disabled
    public static String encrypt(String str) throws SecurityException {
        return str;
        // if(encryptCipher == null || decryptCipher == null)
        // init();
        // try {
        // byte[] b = str.getBytes(charset);
        // byte[] enc = encryptCipher.doFinal(b);
        // return encoder.encode(enc);
        // }
        // catch (Exception e) {
        // throw new SecurityException("Could not encrypt: " + e.getMessage());
        // }
        //    
    }

    // if(encryptCipher == null || decryptCipher == null)
    // init();
    // try {
    // byte[] dec = decoder.decodeBuffer(str);
    // byte[] b = decryptCipher.doFinal(dec);
    // return new String(b, charset);
    // }
    // catch (Exception e) {
    // throw new SecurityException("Could not decrypt: " + e.getMessage());
    // }
    // }
}
