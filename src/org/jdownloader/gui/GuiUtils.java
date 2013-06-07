package org.jdownloader.gui;

import java.awt.Window;

import org.appwork.utils.os.CrossSystem;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class GuiUtils {

    public static void flashWindow(Window window, boolean flashTray, boolean flashWindow) {
        if (CrossSystem.isWindows()) {
            System.out.println("Flash: " + flashTray);
            User32 lib = null;
            try {
                lib = (User32) Native.loadLibrary("user32", User32.class);
                User32.FLASHWINFO flash = new User32.FLASHWINFO();
                HWND hwnd = new HWND();
                hwnd.setPointer(Native.getComponentPointer(window));
                flash.hWnd = hwnd;
                flash.uCount = 100;
                flash.dwTimeout = 1000;
                if (flashTray || flashWindow) {
                    flash.dwFlags = (flashTray ? User32.FLASHW_TRAY : 0) | (flashWindow ? User32.FLASHW_CAPTION : 0);
                } else {
                    flash.dwFlags = User32.FLASHW_STOP;
                }
                flash.cbSize = flash.size();
                lib.FlashWindowEx(flash);

            } catch (UnsatisfiedLinkError e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Flashing not supported on your System");
        }
    }

}
