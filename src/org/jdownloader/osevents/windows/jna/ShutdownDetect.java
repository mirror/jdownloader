package org.jdownloader.osevents.windows.jna;

import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import org.jdownloader.osevents.OperatingSystemEventSender;
import org.jdownloader.osevents.OperatingSystemListener;
import org.jdownloader.osevents.ShutdownOperatingSystemVetoEvent;

import com.sun.jna.Callback;
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

public class ShutdownDetect {
    /**
     * @see <a href="http://msdn.microsoft.com/en-us/library/aa376889.aspx">WM_ENDSESSION reference</a>
     */
    public static final int WM_ENDSESSION      = 0x16;
    public static final int WM_QUERYENDSESSION = 0x11;

    public interface IShutdownListener {
        void onShutdown();
    }

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

    public static final class MyHookProc implements WinHookProc {
        public IShutdownListener listener;
        public WinUser.HHOOK     hhook;

        @Override
        public LRESULT callback(int nCode, WPARAM wParam, CWPSSTRUCT hookProcStruct) {
            if (nCode >= 0) {
                System.out.println(hookProcStruct.message);
                // tell the OS it's OK to shut down
                if (hookProcStruct.message.longValue() == WM_QUERYENDSESSION) {
                    System.out.println("WM_QUERYENDSESSION");
                    return new LRESULT(1);
                }
                // process the actual shutting down message
                if (hookProcStruct.message.longValue() == WM_ENDSESSION) {
                    System.out.println("WM_ENDSESSION");
                    listener.onShutdown();
                    return new LRESULT(0); // return success
                }
            }
            // pass the callback on to the next hook in the chain
            return User32.INSTANCE.CallNextHookEx(hhook, nCode, wParam, hookProcStruct.getPointer());
        }
    }

    public static void register(JFrame frame, final IShutdownListener listener) {
        Native.setCallbackExceptionHandler(new Callback.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Callback arg0, Throwable arg1) {
                arg1.printStackTrace();
            }
        });

        // get the window handle for the main window/frame
        final HWND hwnd = new HWND();
        hwnd.setPointer(Native.getComponentPointer(frame));

        // clear the error value
        Native.setLastError(0);

        // retrieve the threadID associated with the main window/frame
        int windowThreadID = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null);
        if (windowThreadID == 0) {
            int x = Native.getLastError();
            throw new IllegalStateException("error calling GetWindowThreadProcessId when installing machine-shutdown handler " + x);
        }

        // clear the error value
        Native.setLastError(0);

        final MyHookProc proc = new MyHookProc();
        proc.listener = listener;
        proc.hhook = User32.INSTANCE.SetWindowsHookEx(4/* WH_CALLWNDPROCRET */, new MyHookProc(), null, windowThreadID/* dwThreadID */);

        // null indicates failure
        if (proc.hhook == null) {
            int x = Native.getLastError();
            throw new IllegalStateException("error calling SetWindowsHookEx when installing machine-shutdown handler " + x);
        }
        System.out.println("Installed shutdown-detect hook procedure");
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Shutdown Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        OperatingSystemEventSender.getInstance().addListener(new OperatingSystemListener() {

            @Override
            public void onOperatingSystemSignal(String name, int number) {
            }

            @Override
            public void onOperatingSystemShutdownVeto(ShutdownOperatingSystemVetoEvent event) {
                System.out.println("VETO");
            }

            @Override
            public void onOperatingSystemSessionEnd() {
            }
        });
        // register(frame, new IShutdownListener() {
        // @Override
        // public void onShutdown() {
        // try {
        // File file;
        // PrintStream out = new PrintStream(file = new File("C:/shutdownTest.txt"));
        // System.out.println("shutting down");
        // out.println("shutting down");
        // out.close();
        // } catch (IOException ex) {
        // }
        // System.exit(1);
        // }
        // });
    }
}
