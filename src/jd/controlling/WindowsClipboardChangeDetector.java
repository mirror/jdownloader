package jd.controlling;

import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public class WindowsClipboardChangeDetector extends ClipboardMonitoring.ClipboardChangeDetector {

    private interface User32 extends StdCallLibrary {
        // https://msdn.microsoft.com/en-us/library/windows/desktop/ms649042%28v=vs.85%29.aspx
        int GetClipboardSequenceNumber();

        // https://msdn.microsoft.com/en-us/library/windows/desktop/ms649041%28v=vs.85%29.aspx
        HWND GetClipboardOwner();

        // https://msdn.microsoft.com/de-de/library/windows/desktop/ms633522%28v=vs.85%29.aspx
        int GetWindowThreadProcessId(HWND hwnd, IntByReference pid);
    }

    private interface Kernel32 extends StdCallLibrary {
        public Pointer OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);

        boolean CloseHandle(Pointer handle);
    };

    private interface psapi extends StdCallLibrary {
        // https://msdn.microsoft.com/en-us/library/windows/desktop/ms683198%28v=vs.85%29.aspx
        int GetModuleFileNameExA(Pointer process, Pointer hModule, byte[] lpString, int nMaxCount);
    };

    private final User32   user32;
    private int            lastClipboardSequenceNumber = -1;
    private final psapi    psapi;
    private final Kernel32 kernel32;

    protected WindowsClipboardChangeDetector(final AtomicBoolean skipChangeFlag) {
        super(skipChangeFlag);
        user32 = (User32) com.sun.jna.Native.loadLibrary("user32", User32.class);
        psapi = (psapi) Native.loadLibrary("psapi", psapi.class);
        kernel32 = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
    }

    // http://stackoverflow.com/questions/7521693/converting-c-sharp-to-java-jna-getmodulefilename-from-hwnd
    protected String getClipboardOwnerWindowText() {
        final HWND hWnd = user32.GetClipboardOwner();
        if (hWnd != null) {
            final IntByReference pid = new IntByReference();
            user32.GetWindowThreadProcessId(hWnd, pid);
            final Pointer process = kernel32.OpenProcess(1040, false, pid.getValue());
            if (process != null) {
                try {
                    final Pointer zero = new Pointer(0);
                    final byte[] exePathname = new byte[1024];
                    final int result = psapi.GetModuleFileNameExA(process, zero, exePathname, 512);
                    final String ret = Native.toString(exePathname).substring(0, result);
                    if (ret != null) {
                        return ret.trim();
                    }
                } finally {
                    kernel32.CloseHandle(process);
                }
            }
        }
        return null;
    }

    @Override
    protected boolean hasChanges() {
        final int currentClipboardSequenceNumber = user32.GetClipboardSequenceNumber();
        if (currentClipboardSequenceNumber != 0) {
            if (currentClipboardSequenceNumber != lastClipboardSequenceNumber) {
                lastClipboardSequenceNumber = currentClipboardSequenceNumber;
                System.out.println("Clipboard Change Detected:" + getClipboardOwnerWindowText());
                return true;
            } else {
                return false;
            }
        }
        return super.hasChanges();
    }
}
