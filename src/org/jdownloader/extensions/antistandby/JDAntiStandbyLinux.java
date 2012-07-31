package org.jdownloader.extensions.antistandby;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.UInt32;
import org.jdownloader.logging.LogController;

public class JDAntiStandbyLinux {

    private static JDAntiStandbyLinux INSTANCE = new JDAntiStandbyLinux();
    private DBusConnection            c        = null;
    private GnomeSessionManager       sm       = null;
    private UInt32                    cookie   = null;

    public static void main(String[] args) {
        System.out.println(getInstance().enable());
        System.out.println(getInstance().isEnabled());
        getInstance().disable();
        System.out.println(getInstance().isEnabled());
        getInstance().stop();
    }

    private JDAntiStandbyLinux() {
        /* TODO: change, rewrite unix-socket to use dynamic path for library */
        try {
            addLibraryPath("/usr/lib/jni/");
            c = DBusConnection.getConnection(DBusConnection.SESSION);
            sm = (GnomeSessionManager) c.getRemoteObject("org.gnome.SessionManager", "/org/gnome/SessionManager", GnomeSessionManager.class);
        } catch (Exception e) {
            LogController.CL().log(e);
            try {
                c.disconnect();
            } catch (final Throwable e1) {
            }
            c = null;
            sm = null;
        }
    }

    public static void setLibraryPath(String path) throws Exception {
        System.setProperty("java.library.path", path);
        // set sys_paths to null
        final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
    }

    /* http://fahdshariff.blogspot.de/2011/08/changing-java-library-path-at-runtime.html */
    public static void addLibraryPath(String pathToAdd) throws Exception {
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        // get array of paths
        final String[] paths = (String[]) usrPathsField.get(null);

        // check if the path to add is already present
        for (String path : paths) {
            if (path.equals(pathToAdd)) { return; }
        }

        // add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }

    public synchronized void stop() {
        if (!possible()) return;
        try {
            c.disconnect();
        } catch (final Throwable e) {
        }
        c = null;
        sm = null;
    }

    public synchronized boolean possible() {
        return c != null && sm != null;
    }

    public static JDAntiStandbyLinux getInstance() {
        return INSTANCE;
    }

    /* http://people.gnome.org/~mccann/gnome-session/docs/gnome-session.html#org.gnome.SessionManager::SessionOver */
    @DBusInterfaceName("org.gnome.SessionManager")
    public interface GnomeSessionManager extends DBusInterface {
        /**
         * app_id: The application identifier
         * 
         * toplevel_xid: The toplevel X window identifier
         * 
         * reason: The reason for the inhibit
         * 
         * flags: Flags that spefify what should be inhibited
         * 
         * 1: Inhibit logging out
         * 
         * 2: Inhibit user switching
         * 
         * 4: Inhibit suspending the session or computer
         * 
         * 8: Inhibit the session being marked as idle
         * 
         * @param appID
         * @param toplevel_xid
         * @param reason
         * @param flags
         * @return
         */
        public UInt32 Inhibit(String appID, UInt32 toplevel_xid, String reason, UInt32 flags);

        public void Uninhibit(UInt32 inhibit_cookie);

        public boolean IsInhibited(UInt32 flags);
    }

    public synchronized boolean enable() {
        if (!possible()) return false;
        if (cookie != null) return true;
        cookie = sm.Inhibit("JDownloader", new UInt32(0), "active", new UInt32(1));
        return cookie.intValue() != 0;
    }

    public synchronized boolean isEnabled() {
        boolean ret = sm.IsInhibited(new UInt32(1));
        return cookie != null || ret;
    }

    public synchronized void disable() {
        if (!possible() && cookie == null) return;
        sm.Uninhibit(cookie);
        cookie = null;
    }
}
