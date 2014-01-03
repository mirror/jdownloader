package org.jdownloader.crosssystem.idlegetter;

public class ModernWindowsJnaIdleGetter extends IdleGetter {

    private volatile BasicMousePointerIdleGetter fallback;

    @Override
    public long getIdleTimeSinceLastUserInput() {
        try {
            // I noticed that this call can take up to a Second. this happens randomly on my system.
            if (fallback != null) { return fallback.getIdleTimeSinceLastUserInput(); }
            com.sun.jna.platform.win32.User32.LASTINPUTINFO lastInputInfo = new com.sun.jna.platform.win32.User32.LASTINPUTINFO();
            com.sun.jna.platform.win32.User32.INSTANCE.GetLastInputInfo(lastInputInfo);
            return com.sun.jna.platform.win32.Kernel32.INSTANCE.GetTickCount() - lastInputInfo.dwTime;
        } catch (Throwable e) {
            fallback = new BasicMousePointerIdleGetter();
            return fallback.getIdleTimeSinceLastUserInput();
        }
    }

}
