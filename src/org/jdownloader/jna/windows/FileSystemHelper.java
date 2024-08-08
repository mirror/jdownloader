package org.jdownloader.jna.windows;

import java.io.File;
import java.io.IOException;

import org.appwork.utils.Files;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.logging.LogController;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.Winioctl;
import com.sun.jna.platform.win32.WinioctlUtil;
import com.sun.jna.ptr.IntByReference;

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
                return -1;
            }
            final DWORDByReference lpSectorsPerCluster = new DWORDByReference();
            final DWORDByReference lpBytesPerSector = new DWORDByReference();
            final DWORDByReference lpNumberOfFreeClusters = new DWORDByReference();
            final DWORDByReference lpTotalNumberOfClusters = new DWORDByReference();
            if (!com.sun.jna.platform.win32.Kernel32.INSTANCE.GetDiskFreeSpace(rootPath, lpSectorsPerCluster, lpBytesPerSector, lpNumberOfFreeClusters, lpTotalNumberOfClusters)) {
                throw new IOException("GetDiskFreeSpace:" + rootPath + "|" + Kernel32Util.formatMessage(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError()));
            }
            return lpSectorsPerCluster.getValue().intValue() * lpBytesPerSector.getValue().intValue();
        } catch (Exception e) {
            LogController.CL().log(e);
            return -1;
        }
    }

    @FieldOrder({ "sparseFlag" })
    public static class FILE_SET_SPARSE_BUFFER extends Structure {
        public boolean sparseFlag;

        public FILE_SET_SPARSE_BUFFER(final boolean sparseFlag) {
            super();
            this.sparseFlag = sparseFlag;
            write();
        }
    }

    public static final int FSCTL_SET_SPARSE = WinioctlUtil.CTL_CODE(Winioctl.FILE_DEVICE_FILE_SYSTEM, 49, Winioctl.METHOD_BUFFERED, Winioctl.FILE_SPECIAL_ACCESS);

    /**
     * https://learn.microsoft.com/en-us/windows/win32/api/winioctl/ni-winioctl-fsctl_set_sparse
     *
     * https://learn.microsoft.com/en-us/windows/win32/api/ioapiset/nf-ioapiset-deviceiocontrol
     *
     * @param file
     * @param sparseFlag
     * @return
     */
    public static boolean FSCTL_SET_SPARSE(final File file, final boolean sparseFlag) {
        HANDLE fileHandle = null;
        try {
            final OperatingSystem os = CrossSystem.getOS();
            if (!os.isMinimum(OperatingSystem.WINDOWS_8)) {
                // Windows Server 2003 and Windows XP: passing FALSE in the FILE_SET_SPARSE_BUFFER structure will cause this function call
                // to fail.The only way to clear this attribute is to overwrite the file
                // Windows Server 2008 R2, Windows 7, Windows Server 2008 and Windows Vista: A clear operation is valid only on files that
                // no longer have any sparse regions. Performing a clear operation on a file with sparse regions can have unpredictable
                // results.
                return false;
            }
            fileHandle = com.sun.jna.platform.win32.Kernel32.INSTANCE.CreateFile(file.getAbsolutePath(), WinNT.GENERIC_READ | WinNT.GENERIC_WRITE, 0, null, WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL, null);
            if (fileHandle == null) {
                throw new IOException("CreateFile:" + file + "|" + Kernel32Util.formatMessage(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError()));
            }
            final FILE_SET_SPARSE_BUFFER b = new FILE_SET_SPARSE_BUFFER(sparseFlag);
            final IntByReference lpBytes = new IntByReference();
            if (!Kernel32.INSTANCE.DeviceIoControl(fileHandle, FSCTL_SET_SPARSE, b.getPointer(), b.size(), null, 0, lpBytes, null)) {
                throw new IOException("DeviceIoControl:" + file + "|" + sparseFlag + "|" + Kernel32Util.formatMessage(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError()));
            }
            return true;
        } catch (Exception e) {
            LogController.CL().log(e);
            return false;
        } finally {
            Kernel32.INSTANCE.CloseHandle(fileHandle);
        }
    }
}
