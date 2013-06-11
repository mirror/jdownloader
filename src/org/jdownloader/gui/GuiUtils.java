package org.jdownloader.gui;

import java.awt.Window;

import javax.swing.JFrame;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class GuiUtils {

    public static void flashWindow(Window window, boolean flashTray, boolean flashWindow) {
        if (CrossSystem.isWindows() && false) {
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

    /**
     * Checks whether mainFrame or its children are active or not.
     * 
     * @param mainFrame
     * @return
     */
    public static boolean isActiveWindow(final JFrame mainFrame) {
        return new EDTHelper<Boolean>() {

            @Override
            public Boolean edtRun() {
                // this methods does not work under windows if the jdownloader frame is flashing in task. in this case it is not really
                // active, but mainFRame.isActive returns true
                if (!mainFrame.isVisible()) return false;
                if (mainFrame.isActive()) return true;

                for (final Window w : mainFrame.getOwnedWindows()) {
                    if (w.isActive()) { return true; }

                }
                return false;
            }
        }.getReturnValue();

    }

}
