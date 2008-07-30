package jd.utils;

public class OSDetector {

    private static byte OS_ID = -1;
    public static final byte OS_LINUX_OTHER = 6;
    public static final byte OS_MAC_OTHER = 5;
    public static final byte OS_WINDOWS_2000 = 2;
    public static final byte OS_WINDOWS_NT = 3;
    public static final byte OS_WINDOWS_OTHER = 4;
    public static final byte OS_WINDOWS_VISTA = 1;
    public static final byte OS_WINDOWS_XP = 0;

    private static void getOS() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("windows xp") > -1) {
            OS_ID = OS_WINDOWS_XP;
        } else if (OS.indexOf("windows vista") > -1) {
            OS_ID = OS_WINDOWS_VISTA;
        } else if (OS.indexOf("windows 2000") > -1) {
            OS_ID = OS_WINDOWS_2000;
        } else if (OS.indexOf("nt") > -1) {
            OS_ID = OS_WINDOWS_NT;

        } else if (OS.indexOf("windows") > -1) {
            OS_ID = OS_WINDOWS_OTHER;

        } else if (OS.indexOf("mac") >= 0) {
            OS_ID = OS_MAC_OTHER;
        } else {
            OS_ID = OS_LINUX_OTHER;
        }

    }

    public static byte getOSID() {
        if (OS_ID < 0) {
            OSDetector.getOS();
        }
        return OS_ID;

    }

    public static boolean isLinux() {
        byte id = OSDetector.getOSID();
        switch (id) {
        case OS_LINUX_OTHER:

            return true;

        }
        return false;
    }

    public static boolean isMac() {
        byte id = OSDetector.getOSID();
        switch (id) {
        case OS_MAC_OTHER:

            return true;

        }
        return false;
    }

    public static boolean isWindows() {
        byte id = OSDetector.getOSID();
        switch (id) {
        case OS_WINDOWS_XP:
        case OS_WINDOWS_VISTA:
        case OS_WINDOWS_2000:
        case OS_WINDOWS_NT:
        case OS_WINDOWS_OTHER:
            return true;

        }
        return false;
    }

}
