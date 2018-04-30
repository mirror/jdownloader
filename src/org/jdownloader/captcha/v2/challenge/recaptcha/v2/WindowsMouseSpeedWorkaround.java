package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import org.appwork.utils.os.CrossSystem;

public class WindowsMouseSpeedWorkaround {
    private Integer mouseSpeed = null;

    public void loadMouseSpeed() {
        if (CrossSystem.isWindows()) {
            final com.sun.jna.Pointer mouseSpeedPtr = new com.sun.jna.Memory(4);
            mouseSpeed = org.jdownloader.jna.windows.User32.INSTANCE.SystemParametersInfo(0x0070, 0, mouseSpeedPtr, 0) ? mouseSpeedPtr.getInt(0) : null;
        }
    }

    public void saveMouseSpeed() {
        final Integer mouseSpeed = this.mouseSpeed;
        if (CrossSystem.isWindows() && mouseSpeed != null) {
            org.jdownloader.jna.windows.User32.INSTANCE.SystemParametersInfo(0x0071, 0, mouseSpeed, 0x02);
        }
    }
}
