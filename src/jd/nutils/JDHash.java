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

import org.appwork.utils.Hash;

/**
 * TODO: Remove with next major update and change to
 * {@link org.appwork.utils.Hash}
 */
public class JDHash {

    public static String HASH_TYPE_MD5  = "md5";

    public static String HASH_TYPE_SHA1 = "SHA-1";

    public static String getMD5(String arg) {
        return Hash.getStringHash(arg, HASH_TYPE_MD5);
    }

    public static String getMD5(File arg) {
        return Hash.getFileHash(arg, HASH_TYPE_MD5);
    }

    public static String getSHA1(String arg) {
        return Hash.getStringHash(arg, HASH_TYPE_SHA1);
    }

    public static String getSHA1(File arg) {
        return Hash.getFileHash(arg, HASH_TYPE_SHA1);
    }

}
