package jd.controlling;

import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

public class WindowsClipboardChangeDetector extends ClipboardMonitoring.ClipboardChangeDetector {

    private interface User32 extends StdCallLibrary {
        // https://msdn.microsoft.com/en-us/library/windows/desktop/ms649042%28v=vs.85%29.aspx
        int GetClipboardSequenceNumber();

        // https://msdn.microsoft.com/en-us/library/windows/desktop/ms649041%28v=vs.85%29.aspx
        HWND GetClipboardOwner();

        // https://msdn.microsoft.com/en-us/library/ms633520%28VS.85%29.aspx
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
    }

    private final User32 user32;
    private int          lastClipboardSequenceNumber = -1;

    protected WindowsClipboardChangeDetector(final AtomicBoolean skipChangeFlag) {
        super(skipChangeFlag);
        user32 = (User32) com.sun.jna.Native.loadLibrary("user32", User32.class);
    }

    protected String getClipboardOwnerWindowText() {
        final byte[] windowText = new byte[512];
        HWND hWnd = user32.GetClipboardOwner();
        user32.GetWindowTextA(hWnd, windowText, 512);
        final String ret = Native.toString(windowText);
        if (ret != null) {
            return ret.trim();
        } else {
            return null;
        }
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
                System.out.println("Clipboard Unchanged");
                return false;
            }
        }
        return super.hasChanges();
    }
}
