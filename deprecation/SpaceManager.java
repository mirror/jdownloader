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
import java.lang.reflect.Method;

import jd.plugins.DownloadLink;

public class SpaceManager {

    public static boolean checkDownloadLink(DownloadLink downloadLink) {
        return SpaceManager.checkPath(new File(downloadLink.getFilePackage().getDownloadDirectory()), downloadLink.getDownloadSize());
    }

    public static boolean checkPath(File path, long size) {
        if (size > 0) {
            long space = SpaceManager.getUsableSpace(path);
            if (space != -1 && space - size < 1) return false;
        }
        return true;
    }

    public static long getUsableSpace(File f) {
        try {
            Method reflectOnUsableSpace = File.class.getMethod("getUsableSpace", (Class[]) null);
            Long ret = (Long) reflectOnUsableSpace.invoke(f);
            if (ret != null && ret.longValue() > 0) return ret.longValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
