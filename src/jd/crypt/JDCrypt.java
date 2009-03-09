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

        return new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    }

}
