package org.jdownloader.jna.windows;

import java.awt.Frame;

import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.swing.windowmanager.WindowsWindowManager;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;

public class JNAWindowsWindowManager extends WindowsWindowManager {
    public JNAWindowsWindowManager() {

        // load library
        User32 inst = User32.INSTANCE;
    }

    @Override
    public void setExtendedState(Frame w, final WindowExtendedState state) {
        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_2000)) {
            try {
                switch (state) {
                case ICONIFIED:
                    User32.INSTANCE.CloseWindow(new HWND(Native.getComponentPointer(w)));
                    return;
                case NORMAL:

                    User32.INSTANCE.ShowWindow(new HWND(Native.getComponentPointer(w)), User32.SHOW_WINDOW_SW_RESTORE);

                    return;

                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);

            }
        }
        super.setExtendedState(w, state);
    }

    @Override
    protected void setExtendedState(Frame w, int frameExtendedState) {
        super.setExtendedState(w, frameExtendedState);
    }
}
