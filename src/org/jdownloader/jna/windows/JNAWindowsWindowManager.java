package org.jdownloader.jna.windows;

import java.awt.Frame;
import java.awt.Window;

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
    public void setZState(Window w, FrameState state) {
        super.setZState(w, state);
        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_XP) && w.isVisible()) {
            switch (state) {
            case TO_FRONT:
            case TO_FRONT_FOCUSED:
                if (w instanceof Frame) {
                    try {
                        final HWND hWnd = User32.INSTANCE.FindWindow(null, ((Frame) w).getTitle());
                        if (hWnd != null) {
                            User32.INSTANCE.SetForegroundWindow(hWnd);
                        }
                    } catch (Exception e) {
                        LoggerFactory.getDefaultLogger().log(e);
                    }
                }
            }
        }
    }

    @Override
    public void setExtendedState(Frame w, final WindowExtendedState state) {
        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_XP) && w.isVisible()) {
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
