package org.jdownloader.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.appwork.utils.Files;
import org.appwork.utils.os.CrossSystem;

import com.sun.jna.platform.mac.MacFileUtils;
import com.sun.jna.platform.win32.W32FileUtils;

public class JDFileUtils extends Files {
    /**
     * @param f
     */

    public static boolean isTrashSupported() {
        if (CrossSystem.isWindows()) {
            return W32FileUtils.getInstance().hasTrash();
        } else if (CrossSystem.isMac()) { return MacFileUtils.getInstance().hasTrash(); }

        return false;
    }

    public static void moveToTrash(File... files) throws IOException {
        // .moveToTrash()

        if (CrossSystem.isWindows()) {
            for (File f : files) {
                if (!f.exists()) { throw new FileNotFoundException(f.getAbsolutePath()); }
            }
            W32FileUtils.getInstance().moveToTrash(files);
        } else if (CrossSystem.isMac()) {
            for (File f : files) {
                if (!f.exists()) { throw new FileNotFoundException(f.getAbsolutePath()); }
            }
            MacFileUtils.getInstance().moveToTrash(files);

        }

    }
}
