package org.jdownloader.jna.windows;

public interface User32 extends com.sun.jna.platform.win32.User32 {
    public User32 INSTANCE = (User32) com.sun.jna.Native.loadLibrary("user32", User32.class);
}
