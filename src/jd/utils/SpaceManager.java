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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jd.plugins.DownloadLink;

public class SpaceManager {

    public static boolean checkDownloadLink(DownloadLink downloadLink) {
        return SpaceManager.checkPath(new File(downloadLink.getFilePackage().getDownloadDirectory()), downloadLink.getDownloadSize());
    }

    public static boolean checkPath(File Path, long size) {
        if (size > 0) {
            long space = SpaceManager.getUsableSpace(Path);
            if (space > 0 && space - size < 1) { return false; }
        }
        return true;
    }

    public static long getUsableSpace(File f) {
        Method reflectOnUsableSpace;

        try {
            reflectOnUsableSpace = File.class.getMethod("getUsableSpace", (Class[]) null);
        } catch (NoSuchMethodException e) {
            reflectOnUsableSpace = null;
        }
        try {
            return ((Long) reflectOnUsableSpace.invoke(f)).longValue();
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        }
        return -1;
    }
}
