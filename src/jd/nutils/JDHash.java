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
package jd.nutils;

import java.io.File;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;

import org.appwork.utils.Hash;
import org.appwork.utils.formatter.HexFormatter;

/**
 * TODO: Remove with next major update and change to {@link org.appwork.utils.Hash}
 */
public class JDHash {
    public static String HASH_TYPE_MD5    = Hash.HASH_TYPE_MD5;
    public static String HASH_TYPE_SHA1   = Hash.HASH_TYPE_SHA1;
    public static String HASH_TYPE_SHA256 = Hash.HASH_TYPE_SHA256;
    public static String HASH_TYPE_SHA512 = Hash.HASH_TYPE_SHA512;

    public static String getMD5(final String arg) {
        return Hash.getStringHash(arg, HASH_TYPE_MD5);
    }

    public static String getMD5(final File arg) {
        return Hash.getFileHash(arg, HASH_TYPE_MD5);
    }

    public static String getSHA1(final String arg) {
        return Hash.getStringHash(arg, HASH_TYPE_SHA1);
    }

    public static String getSHA1(final File arg) {
        return Hash.getFileHash(arg, HASH_TYPE_SHA1);
    }

    public static String getSHA256(final String arg) {
        return Hash.getStringHash(arg, HASH_TYPE_SHA256);
    }

    public static String getSHA256(final File arg) {
        return Hash.getFileHash(arg, HASH_TYPE_SHA256);
    }

    public static String getSHA512(final String arg) {
        return Hash.getStringHash(arg, HASH_TYPE_SHA512);
    }

    public static String getSHA512(final File arg) {
        return Hash.getFileHash(arg, HASH_TYPE_SHA512);
    }

    /**
     * returns crc32 checksum from input
     *
     * @author raztoki
     * @param input
     * @return
     * @throws Exception
     */
    public static String getCRC32(final String input) throws Exception {
        if (input == null) {
            return null;
        }
        try {
            byte bytes[] = input.getBytes();
            final Checksum cs = new CRC32();
            cs.update(bytes, 0, bytes.length);
            long checksum = cs.getValue();
            final HashInfo hi = new HashInfo(HexFormatter.byteArrayToHex(new byte[] { (byte) (checksum >>> 24), (byte) (checksum >>> 16), (byte) (checksum >>> 8), (byte) checksum }), TYPE.CRC32);
            return hi.getHash();
        } catch (Exception e) {
            return null;
        }
    }
}
