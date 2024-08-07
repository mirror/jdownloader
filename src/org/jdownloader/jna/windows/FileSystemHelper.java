package org.jdownloader.jna.windows;

import java.io.File;

import org.appwork.utils.Files;

import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;

public class FileSystemHelper {
    /**
     * https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-getdiskfreespacea
     *
     * https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-getdrivetypea
     *
     *
     * @param file
     * @return
     */
    public static int getAllocationUnitSize(final File file) {
        try {
            String rootPath = Files.guessRoot(file).getPath();
            if (!rootPath.endsWith("\\")) {
                // A trailing backslash is required
                rootPath += "\\";
            }
            final int driveType = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetDriveType(rootPath);
            switch (driveType) {
            case WinBase.DRIVE_FIXED:
            case WinBase.DRIVE_REMOTE:
                break;
            default:
                System.out.println(driveType);
                return -1;
            }
            final DWORDByReference lpSectorsPerCluster = new DWORDByReference();
            final DWORDByReference lpBytesPerSector = new DWORDByReference();
            final DWORDByReference lpNumberOfFreeClusters = new DWORDByReference();
            final DWORDByReference lpTotalNumberOfClusters = new DWORDByReference();
            if (!com.sun.jna.platform.win32.Kernel32.INSTANCE.GetDiskFreeSpace(rootPath, lpSectorsPerCluster, lpBytesPerSector, lpNumberOfFreeClusters, lpTotalNumberOfClusters)) {
                return -1;
            }
            return lpSectorsPerCluster.getValue().intValue() * lpBytesPerSector.getValue().intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
