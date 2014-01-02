package org.jdownloader.crosssystem.idlegetter;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;

public class ModernWindowsJnaIdleGetter extends IdleGetter {

    private BasicMousePointerIdleGetter fallback;

    @Override
    public long getIdleTimeSinceLastUserInput() {
        try {
            // I noticed that this call can take up to a Second. this happens randomly on my system.
            if (fallback != null) { return fallback.getIdleTimeSinceLastUserInput(); }
            User32.LASTINPUTINFO lastInputInfo = new User32.LASTINPUTINFO();
            User32.INSTANCE.GetLastInputInfo(lastInputInfo);
            return Kernel32.INSTANCE.GetTickCount() - lastInputInfo.dwTime;
        } catch (Error e) {
            // ClassNOtFoundError
            fallback = new BasicMousePointerIdleGetter();
            return fallback.getIdleTimeSinceLastUserInput();
        } catch (Throwable e) {
            fallback = new BasicMousePointerIdleGetter();
            return fallback.getIdleTimeSinceLastUserInput();
        }
    }

}
