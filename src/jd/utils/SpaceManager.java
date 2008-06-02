package jd.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jd.plugins.DownloadLink;

public class SpaceManager {

    public static long getUsableSpace(File f) {
        Method reflectOnUsableSpace;

        try {
            reflectOnUsableSpace = File.class.getMethod("getUsableSpace", (Class[]) null);
        } catch (NoSuchMethodException e) {
            reflectOnUsableSpace = null;
        }
        try {
            return ((Long) reflectOnUsableSpace.invoke(f, null)).longValue();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean checkPath(File Path, long size) {
        if (size > 0) {
            long space = getUsableSpace(Path);
            if (space > 0 && (space - size) < 1) return false;
        }
        return true;
    }

    public static boolean checkDownloadLink(DownloadLink downloadLink) {
        return checkPath(new File(downloadLink.getFilePackage().getDownloadDirectory()), downloadLink.getDownloadMax());
    }
}
