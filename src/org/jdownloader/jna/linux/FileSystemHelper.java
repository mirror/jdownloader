package org.jdownloader.jna.linux;

import java.io.File;

import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.linux.LibC.Statvfs;

public class FileSystemHelper {

    public static int getAllocationUnitSize(final File file) {
        try {
            File path = file;
            while (path != null) {
                if (path.exists()) {
                    break;
                } else {
                    path = path.getParentFile();
                }
            }
            if (path == null) {
                return -1;
            }
            final Statvfs vfs = new Statvfs();
            if (LibC.INSTANCE.statvfs(path.getPath(), vfs) != 0) {
                return -1;
            }
            return vfs.f_bsize.intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
