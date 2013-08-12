package org.jdownloader.crosssystem.idlegetter;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;

public class ModernWindowsJnaIdleGetter extends IdleGetter {

    private BasicMousePointerIdleGetter fallback;

    @Override
    public long getIdleTimeSinceLastUserInput() {
        try {
            if (fallback != null) { return fallback.getIdleTimeSinceLastUserInput(); }
            User32.LASTINPUTINFO lastInputInfo = new User32.LASTINPUTINFO();
            User32.INSTANCE.GetLastInputInfo(lastInputInfo);
            return Kernel32.INSTANCE.GetTickCount() - lastInputInfo.dwTime;
        } catch (Exception e) {
            fallback = new BasicMousePointerIdleGetter();
            return fallback.getIdleTimeSinceLastUserInput();
        }
    }

}
