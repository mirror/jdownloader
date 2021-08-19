package org.jdownloader.jna.windows;

import java.awt.Frame;

import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.swing.windowmanager.WindowsWindowManager;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;

public class JNAWindowsWindowManager extends WindowsWindowManager {
    private final User32 instance;

    public JNAWindowsWindowManager() {
        // load library
        instance = User32.INSTANCE;
    }

    @Override
    public void setExtendedState(Frame w, final WindowExtendedState state) {
        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_7) && w.isVisible()) {
            try {
                switch (state) {
                case ICONIFIED:
                    instance.CloseWindow(new HWND(Native.getComponentPointer(w)));
                    return;
                case NORMAL:
                    instance.ShowWindow(new HWND(Native.getComponentPointer(w)), User32.SHOW_WINDOW_SW_RESTORE);
                    return;
                default:
                    break;
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
        super.setExtendedState(w, state);
    }
}
