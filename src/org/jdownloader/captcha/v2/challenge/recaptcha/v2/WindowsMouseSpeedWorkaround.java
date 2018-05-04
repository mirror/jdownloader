package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import org.appwork.utils.os.CrossSystem;

import com.sun.jna.Pointer;

public class WindowsMouseSpeedWorkaround {
    private Integer mouseSpeed = null;

    public Integer getMouseSpeed() {
        if (CrossSystem.isWindows()) {
            final com.sun.jna.Pointer mouseSpeedPtr = new com.sun.jna.Memory(4);
            mouseSpeed = org.jdownloader.jna.windows.User32.INSTANCE.SystemParametersInfo(0x0070, 0, mouseSpeedPtr, 0) ? mouseSpeedPtr.getInt(0) : null;
            return mouseSpeed;
        } else {
            return null;
        }
    }

    public boolean setMouseSpeed() {
        return setMouseSpeed(this.mouseSpeed);
    }

    public boolean setMouseSpeed(final Integer mouseSpeed) {
        if (CrossSystem.isWindows() && mouseSpeed != null) {
            return (org.jdownloader.jna.windows.User32.INSTANCE.SystemParametersInfo(0x0071, 0, Pointer.createConstant(mouseSpeed.intValue()), 0x02));
        } else {
            return false;
        }
    }
}
