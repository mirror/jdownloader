//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils;

public final class OSDetector {

    /**
     * Don't let anyone instantiate this class.
     */
    private OSDetector() {
    }

    private static final byte OS_ID;
    private static final String OS_STRING;

    public static final byte OS_LINUX_OTHER = 6;
    public static final byte OS_MAC_OTHER = 5;
    public static final byte OS_WINDOWS_OTHER = 4;
    public static final byte OS_WINDOWS_NT = 3;
    public static final byte OS_WINDOWS_2000 = 2;
    public static final byte OS_WINDOWS_XP = 0;
    public static final byte OS_WINDOWS_2003 = 7;
    public static final byte OS_WINDOWS_VISTA = 1;
    public static final byte OS_WINDOWS_7 = 8;

    static {
        OS_STRING = System.getProperty("os.name");
        final String OS = OS_STRING.toLowerCase();
        if (OS.contains("windows 7")) {
            OS_ID = OS_WINDOWS_7;
        } else if (OS.contains("windows xp")) {
            OS_ID = OS_WINDOWS_XP;
        } else if (OS.contains("windows vista")) {
            OS_ID = OS_WINDOWS_VISTA;
        } else if (OS.contains("windows 2000")) {
            OS_ID = OS_WINDOWS_2000;
        } else if (OS.contains("windows 2003")) {
            OS_ID = OS_WINDOWS_2003;
        } else if (OS.contains("nt")) {
            OS_ID = OS_WINDOWS_NT;
        } else if (OS.contains("windows")) {
            OS_ID = OS_WINDOWS_OTHER;
        } else if (OS.contains("mac")) {
            OS_ID = OS_MAC_OTHER;
        } else {
            OS_ID = OS_LINUX_OTHER;
        }
    }

    public static String getOSString() {
        return OS_STRING;
    }

    public static byte getOSID() {
        return OS_ID;
    }

    public static boolean isLinux() {
        return OSDetector.getOSID() == OS_LINUX_OTHER;
    }

    public static boolean isMac() {
        return OSDetector.getOSID() == OS_MAC_OTHER;
    }

    public static boolean isWindows() {
        switch (OSDetector.getOSID()) {
        case OS_WINDOWS_XP:
        case OS_WINDOWS_VISTA:
        case OS_WINDOWS_2000:
        case OS_WINDOWS_2003:
        case OS_WINDOWS_NT:
        case OS_WINDOWS_OTHER:
        case OS_WINDOWS_7:
            return true;
        }
        return false;
    }

    /**
     * Is gnome running?
     */
    public static boolean isGnome() {
        if (!isLinux()) return false;

        // checks gdm session
        final String gdmSession = System.getenv("GDMSESSION");
        if (gdmSession != null && gdmSession.toLowerCase().contains("gnome")) return true;

        // checks desktop session
        final String desktopSession = System.getenv("DESKTOP_SESSION");
        if (desktopSession != null && desktopSession.toLowerCase().contains("gnome")) return true;

        // checks gnome desktop id
        final String gnomeDesktopSessionId = System.getenv("GNOME_DESKTOP_SESSION_ID");
        if (gnomeDesktopSessionId != null && gnomeDesktopSessionId.trim().length() > 0) return true;

        return false;
    }

    /**
     * Is KDE running?
     */
    public static boolean isKDE() {
        if (!isLinux()) return false;

        // checks gdm session
        final String gdmSession = System.getenv("GDMSESSION");
        if (gdmSession != null && gdmSession.toLowerCase().contains("kde")) return true;

        // checks desktop session
        final String desktopSession = System.getenv("DESKTOP_SESSION");
        if (desktopSession != null && desktopSession.toLowerCase().contains("kde")) return true;

        // checks window manager
        final String windowManager = System.getenv("WINDOW_MANAGER");
        if (windowManager != null && windowManager.trim().toLowerCase().endsWith("kde")) return true;

        return false;
    }

}
