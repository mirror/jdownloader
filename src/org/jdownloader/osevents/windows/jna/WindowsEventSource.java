package org.jdownloader.osevents.windows.jna;

import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.osevents.EventSource;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;

public abstract class WindowsEventSource implements EventSource {

    private LogSource logger;

    public WindowsEventSource() {
        logger = LogController.getInstance().getLogger(WindowsEventSource.class.getName());
    }

    /**
     * @see <a href="http://msdn.microsoft.com/en-us/library/aa376889.aspx">WM_ENDSESSION reference</a>
     */
    public static final int  WM_ENDSESSION      = 0x16;
    /**
     * @see http://msdn.microsoft.com/en-us/library/windows/desktop/aa376890(v=vs.85).aspx
     */
    public static final int  WM_QUERYENDSESSION = 0x11;
    private static final int WH_CALLWNDPROCRET  = 0x04;

    public static class CWPSSTRUCT extends Structure {
        public LPARAM lParam;
        public WPARAM wParam;
        public DWORD  message;
        public HWND   hwnd;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "lParam", "wParam", "message", "hwnd" });
        }
    }

    public interface WinHookProc extends WinUser.HOOKPROC {
        /**
         * @see <a href="http://msdn.microsoft.com/en-us/library/windows/desktop/ms644975(v=vs.85).aspx">CallWndProc callback function</a>
         * @param nCode
         *            is an action parameter. anything less than zero indicates I should ignore this call.
         * @param wParam
         *            Specifies whether the message was sent by the current thread. If the message was sent by the current thread, it is
         *            nonzero; otherwise, it is zero
         * @param hookProcStruct
         *            A pointer to a CWPSTRUCT structure that contains details about the message.
         * @return If nCode is less than zero, the hook procedure must return the value returned by CallNextHookEx.
         */
        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, CWPSSTRUCT hookProcStruct);
    }

    public final class EventHookProc implements WinHookProc {

        public WinUser.HHOOK hhook;

        @Override
        public LRESULT callback(int nCode, WPARAM wParam, CWPSSTRUCT hookProcStruct) {
            if (nCode >= 0) {
                System.out.println(hookProcStruct.message + " " + wParam + " - " + hookProcStruct);
                // tell the OS it's OK to shut down
                if (hookProcStruct.message.longValue() == WindowsEventSource.WM_QUERYENDSESSION) {
                    System.out.println("SHUTDOWN WM_QUERYENDSESSION");
                    if (onQueryEndSession()) {
                        return new LRESULT(1);
                    } else {
                        return new LRESULT(1);
                    }

                }
                // process the actual shutting down message
                if (hookProcStruct.message.longValue() == WindowsEventSource.WM_ENDSESSION) {
                    System.out.println("SHUTDOWN WM_ENDSESSION");
                    onEndSession();
                    return new LRESULT(0); // return success
                }
            }
            // pass the callback on to the next hook in the chain
            return User32.INSTANCE.CallNextHookEx(hhook, nCode, wParam, hookProcStruct.getPointer());
        }
    }

    private Thread thread;

    @Override
    public void init() {

        try {
            JFrame frame = JDGui.getInstance().getMainFrame();
            // get the window handle for the main window/frame
            final HWND hwnd = new HWND();
            hwnd.setPointer(Native.getComponentPointer(frame));
            thread = new Thread("WindowsEventCatcher") {
                public void run() {

                    // clear the error value
                    Native.setLastError(0);

                    // retrieve the threadID associated with the main window/frame
                    int windowThreadID = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null);
                    if (windowThreadID == 0) {
                        int x = Native.getLastError();
                        throw new IllegalStateException("error calling GetWindowThreadProcessId when installing machine-shutdown handler " + x);
                    }

                    // clear the error value

                    final EventHookProc proc = new EventHookProc();

                    proc.hhook = User32.INSTANCE.SetWindowsHookEx(4, new EventHookProc(), null, windowThreadID/* dwThreadID */);

                    // null indicates failure
                    if (proc.hhook == null) {
                        int x = Native.getLastError();
                        throw new IllegalStateException("error calling SetWindowsHookEx when installing machine-shutdown handler " + x);
                    }
                    logger.info("Installed WIndows Event hook procedure");
                    // synchronized (this) {
                    // while (true) {
                    // try {
                    // wait();
                    // } catch (InterruptedException e) {
                    // e.printStackTrace();
                    // }
                    // }
                    // }
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            };
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            logger.log(e);
        }

    }

    public abstract void onEndSession();

    public abstract boolean onQueryEndSession();
}
