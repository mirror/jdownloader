package jd.crypt;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import sun.security.pkcs.PKCS7;

public class JDCrypt {

    public static byte[] encrypt(String string) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            IvParameterSpec ivSpec = new IvParameterSpec(sign());
            SecretKeySpec skeySpec = new SecretKeySpec(sign(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(string.getBytes());

        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(byte[] b) {
        Cipher cipher;
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(sign());
            SecretKeySpec skeySpec = new SecretKeySpec(sign(), "AES");

            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b));
        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    private static byte[] sign() {
        InputStream in = null;
        byte[] buf = new byte[12 * 1024];
        byte[] buffer = new byte[0];
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        in = cl.getResourceAsStream(new String(new byte[] { 77, 69, 84, 65, 45, 73, 78, 70, 47, 74, 68, 79, 87, 78, 76, 79, 65, 46, 68, 83, 65 }));
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
                byte[] tmp = new byte[total];
                System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                System.arraycopy(buf, 0, tmp, buffer.length, total);
                buffer = tmp;

            } catch (Exception e) {
            } finally {
                try {
                    in.close();
                } catch (IOException e) {

                    // e.printStackTrace();
                }
            }
        }
        try {
            PKCS7 signatureBlock = new PKCS7(buffer);

            byte signature[] = signatureBlock.getCertificates()[0].getSignature();
            MessageDigest md = MessageDigest.getInstance("MD5");
            signature = md.digest(signature);
            return signature;

        } catch (Exception e) {
            e.printStackTrace();

        }

        return new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    }

}
