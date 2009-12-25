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

    private static final byte OS_ID; // = -1;
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
        //final String OS = getOSString().toLowerCase();
        final String OS = OS_STRING.toLowerCase();
        if (OS.indexOf("windows 7") > -1) {
            OS_ID = OS_WINDOWS_7;
        } else if (OS.indexOf("windows xp") > -1) {
            OS_ID = OS_WINDOWS_XP;
        } else if (OS.indexOf("windows vista") > -1) {
            OS_ID = OS_WINDOWS_VISTA;
        } else if (OS.indexOf("windows 2000") > -1) {
            OS_ID = OS_WINDOWS_2000;
        } else if (OS.indexOf("windows 2003") > -1) {
            OS_ID = OS_WINDOWS_2003;
        } else if (OS.indexOf("nt") > -1) {
            OS_ID = OS_WINDOWS_NT;
        } else if (OS.indexOf("windows") > -1) {
            OS_ID = OS_WINDOWS_OTHER;
        } else if (OS.indexOf("mac") > -1) {
            OS_ID = OS_MAC_OTHER;
        } else {
            OS_ID = OS_LINUX_OTHER;
        }
    }

    // private static void getOS() {
    // final String OS = getOSString().toLowerCase();
    // if (OS.indexOf("windows 7") > -1) {
    // OS_ID = OS_WINDOWS_7;
    // } else if (OS.indexOf("windows xp") > -1) {
    // OS_ID = OS_WINDOWS_XP;
    // } else if (OS.indexOf("windows vista") > -1) {
    // OS_ID = OS_WINDOWS_VISTA;
    // } else if (OS.indexOf("windows 2000") > -1) {
    // OS_ID = OS_WINDOWS_2000;
    // } else if (OS.indexOf("windows 2003") > -1) {
    // OS_ID = OS_WINDOWS_2003;
    // } else if (OS.indexOf("nt") > -1) {
    // OS_ID = OS_WINDOWS_NT;
    // } else if (OS.indexOf("windows") > -1) {
    // OS_ID = OS_WINDOWS_OTHER;
    // } else if (OS.indexOf("mac") > -1) {
    // OS_ID = OS_MAC_OTHER;
    // } else {
    // OS_ID = OS_LINUX_OTHER;
    // }
    // }

    // public static byte getOSID() {
    // if (OS_ID < 0) {
    // OSDetector.getOS();
    // }
    // return OS_ID;
    // }
    public static byte getOSID() {
        return OS_ID;
    }

    public static boolean isLinux() {
        switch (OSDetector.getOSID()) {
        case OS_LINUX_OTHER:
            return true;
        }
        return false;
    }

    public static boolean isMac() {
        switch (OSDetector.getOSID()) {
        case OS_MAC_OTHER:
            return true;
        }
        return false;
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
     * erkennt gnome.
     */
    public static boolean isGnome() {
        if (!isLinux()) return false;
        // prüft gdm session
        final String gdmSession = System.getenv("GDMSESSION");
        if (gdmSession != null && gdmSession.toLowerCase().contains("gnome")) { return true; }

        // prüft desktop session
        final String desktopSession = System.getenv("DESKTOP_SESSION");
        if (desktopSession != null && desktopSession.toLowerCase().contains("gnome")) { return true; }

        // prüft gnome desktop id
        final String gnomeDesktopSessionId = System.getenv("GNOME_DESKTOP_SESSION_ID");
        if (gnomeDesktopSessionId != null && gnomeDesktopSessionId.trim().length() > 0) { return true; }

        return false;
    }

    /**
     * erkennt KDE.
     */
    public static boolean isKDE() {
        if (!isLinux()) return false;

        // prüft gdm session
        final String gdmSession = System.getenv("GDMSESSION");
        if (gdmSession != null && gdmSession.toLowerCase().contains("kde")) { return true; }

        // prüft desktop session
        final String desktopSession = System.getenv("DESKTOP_SESSION");
        if (desktopSession != null && desktopSession.toLowerCase().contains("kde")) { return true; }

        // prüft window manager
        final String windowManager = System.getenv("WINDOW_MANAGER");
        if (windowManager != null && windowManager.trim().toLowerCase().endsWith("kde")) { return true; }

        return false;
    }

    public static String getOSString() {
//        if (OS_STRING == null) {
//            OS_STRING = System.getProperty("os.name");
//        }
        return OS_STRING;
    }

//    public static void setOSString(final String property) {
//        OS_STRING = property;
//    }

}
