package org.jdownloader.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Files;
import org.appwork.utils.os.CrossSystem;

public class JDFileUtils extends Files {
    private static final boolean supported = checkTrashSupported();

    private static boolean checkTrashSupported() {
        try {
            if (CrossSystem.isWindows()) {
                return com.sun.jna.platform.win32.W32FileUtils.getInstance().hasTrash();
            } else if (CrossSystem.isMac()) {
                return com.sun.jna.platform.mac.MacFileUtils.getInstance().hasTrash();
            } else {
                return false;
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param f
     */
    public static boolean isTrashSupported() {
        return supported;
    }

    public static void moveToTrash(File... files) throws IOException {
        try {
            if (isTrashSupported()) {
                final List<File> existing = new ArrayList<File>();
                FileNotFoundException exception = null;
                for (final File file : files) {
                    if (!file.exists()) {
                        if (exception == null) {
                            exception = new FileNotFoundException(file.getAbsolutePath());
                        }
                    } else {
                        existing.add(file);
                    }
                }
                if (existing.size() > 0) {
                    if (CrossSystem.isWindows()) {
                        com.sun.jna.platform.win32.W32FileUtils.getInstance().moveToTrash(existing.toArray(new File[0]));
                    } else if (CrossSystem.isMac()) {
                        com.sun.jna.platform.mac.MacFileUtils.getInstance().moveToTrash(existing.toArray(new File[0]));
                    }
                }
                if (exception != null) {
                    throw exception;
                }
            }
        } catch (final UnsatisfiedLinkError e) {
            // may cause java.lang.UnsatisfiedLinkError
            throw new IOException(e);
        }
    }
}
