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

package jd.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author astaldo/JD-Team
 */
public class JDHash {
    public static String HASH_TYPE_MD5 = "md5";
    public static String HASH_TYPE_SHA1 = "SHA-1";

    /**
     * public static String getLocalHash(File f) Gibt einen MD% Hash der file
     * zurück
     * 
     * @author JD-Team
     * @param f
     * @return Hashstring Md5
     */
    public static String getFileHash(File f, String type) {
        try {
            if (!f.exists()) { return null; }
            MessageDigest md;
            md = MessageDigest.getInstance(type);
            byte[] b = new byte[1024];
            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (byte element : digest) {
                String tmp = Integer.toHexString(element & 0xFF);
                if (tmp.length() < 2) {
                    tmp = "0" + tmp;
                }
                ret += tmp;
            }
            in.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gibt den MD5 hash eines Strings zurück
     * 
     * @param arg
     * @return MD% hash von arg
     */
    public static String getStringHash(String arg, String type) {
        if (arg == null) { return arg; }
        try {
            MessageDigest md = MessageDigest.getInstance(type);
            byte[] digest = md.digest(arg.getBytes());
            String ret = "";
            String tmp;
            for (byte d : digest) {
                tmp = Integer.toHexString(d & 0xFF);
                ret += tmp.length() < 2 ? "0" + tmp : tmp;
            }
            return ret;
        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    public static String getMD5(String arg) {
        return getStringHash(arg, HASH_TYPE_MD5);

    }

    public static String getMD5(File res) {
        return getFileHash(res, HASH_TYPE_MD5);
    }

}
