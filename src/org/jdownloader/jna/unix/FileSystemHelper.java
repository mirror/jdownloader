package org.jdownloader.jna.unix;

import java.io.File;

import org.jdownloader.logging.LogController;

import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.linux.LibC.Statvfs;

public class FileSystemHelper {
    private static boolean JNA_AVAILABLE = true;

    public static int getAllocationUnitSize(final File file) {
        if (!JNA_AVAILABLE) {
            return -1;
        }
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
        } catch (LinkageError e) {
            JNA_AVAILABLE = false;
            // java.lang.UnsatisfiedLinkError
            // leads to future
            // java.lang.NoClassDefFoundError
            LogController.CL().log(e);
        } catch (Exception e) {
            LogController.CL().log(e);
        }
        return -1;
    }
}
