package jd;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class JDCrypt {
    private final static transient String password   = "jDownloader";
    private final static transient byte[] salt = { (byte) 0xc9,(byte) 0xc9, (byte) 0xc9, (byte) 0xc9, (byte) 0xc9, (byte) 0xc9,(byte) 0xc9, (byte) 0xc9};
    private static Cipher                 encryptCipher;
    private static Cipher                 decryptCipher;
    private static BASE64Encoder encoder = new sun.misc.BASE64Encoder();
    private static BASE64Decoder decoder = new sun.misc.BASE64Decoder();
    /** Verwendete Zeichendecodierung */
    private static String                 charset = "UTF16";

    /**
     * Initialisiert den Verschlüsselungsmechanismus
     * 
     * @param pass char[]
     * @param salt byte[]
     * @param iterations int
     * @throws SecurityException
     */
    private static void init()
            throws SecurityException {
        try {
            final PBEParameterSpec ps = new PBEParameterSpec(salt, 20);
            final SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            final SecretKey k = kf.generateSecret(new PBEKeySpec(password.toCharArray()));
            encryptCipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, k, ps);
            decryptCipher = Cipher.getInstance("PBEWithMD5AndDES/CBC/PKCS5Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, k, ps);
        }
        catch (Exception e) {
            throw new SecurityException("Could not initialize CryptoLibrary: "
                    + e.getMessage());
        }
    }

    /**
     * Verschlüsselt eine Zeichenkette
     * 
     * @param str Description of the Parameter
     * @return String the encrypted string.
     * @exception SecurityException Description of the Exception
     */
    public static String encrypt(String str) throws SecurityException {
        if(encryptCipher == null || decryptCipher == null)
            init();
        try {
            byte[] b = str.getBytes(charset);
            byte[] enc = encryptCipher.doFinal(b);
            return encoder.encode(enc);
        }
        catch (Exception e) {
            throw new SecurityException("Could not encrypt: " + e.getMessage());
        }
    }

    /**
     * Entschlüsselt eine Zeichenkette, welche mit der Methode encrypt
     * verschlüsselt wurde.
     * 
     * @param str Description of the Parameter
     * @return String the encrypted string.
     * @exception SecurityException Description of the Exception
     */
    public static String decrypt(String str) throws SecurityException {
        if(encryptCipher == null || decryptCipher == null)
            init();
        try {
            byte[] dec = decoder.decodeBuffer(str);
            byte[] b = decryptCipher.doFinal(dec);
            return new String(b, charset);
        }
        catch (Exception e) {
            throw new SecurityException("Could not decrypt: " + e.getMessage());
        }
    }
}
