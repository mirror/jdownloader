package org.jdownloader.gui;

import java.awt.Window;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class GuiUtils {

    private static final User32 lib;
    static {
        User32 loadedLib = null;
        try {
            if (CrossSystem.isWindows()) {
                loadedLib = (User32) Native.loadLibrary("user32", User32.class);
            } else {
                loadedLib = null;
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        lib = loadedLib;
    }

    public static void flashWindow(final Window window, boolean flashTray) {
        if (CrossSystem.isWindows() && lib != null) {
            System.out.println("Flash: " + flashTray);
            User32.FLASHWINFO flash = new User32.FLASHWINFO();
            HWND hwnd = new HWND();
            hwnd.setPointer(Native.getComponentPointer(window));
            flash.hWnd = hwnd;
            flash.uCount = 100;
            flash.dwTimeout = 1000;
            if (flashTray) {
                flash.dwFlags = User32.FLASHW_TIMERNOFG | User32.FLASHW_ALL;
            } else {
                flash.dwFlags = User32.FLASHW_STOP;
            }
            flash.cbSize = flash.size();
            lib.FlashWindowEx(flash);
        } else if (CrossSystem.isMac()) {
            final com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
            if (application != null) application.requestUserAttention(flashTray);
        }
    }

    /**
     * Checks whether mainFrame or its children are active or not.
     * 
     * @param mainFrame
     * @return
     */
    public static boolean isActiveWindow(final Window mainFrame) {
        if (mainFrame == null) return false;
        return new EDTHelper<Boolean>() {

            @Override
            public Boolean edtRun() {
                // this methods does not work under windows if the jdownloader frame is flashing in task. in this case it is not really
                // active, but mainFRame.isActive returns true
                if (!mainFrame.isVisible()) {
                    System.out.println("Mainframe is invisible");
                    return false;
                }
                // frames can be active, but not visible... at least under win7 java 1.7
                if (mainFrame.isActive() && mainFrame.isVisible()) {
                    System.out.println("Mainframe is active");
                    return true;
                }

                for (final Window w : mainFrame.getOwnedWindows()) {
                    // frames can be active, but not visible... at least under win7 java 1.7
                    if (w.isActive() && w.isVisible()) {

                        System.out.println(w + " is active");
                        return true;
                    }

                }
                return false;
            }
        }.getReturnValue();

    }

}
