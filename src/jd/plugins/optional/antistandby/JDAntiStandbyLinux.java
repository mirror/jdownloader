package jd.plugins.optional.antistandby;

import jd.controlling.JDLogger;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusInterfaceName;
import org.freedesktop.dbus.UInt32;

public class JDAntiStandbyLinux {

    private static JDAntiStandbyLinux INSTANCE = new JDAntiStandbyLinux();
    private DBusConnection c = null;
    private GnomeSessionManager sm = null;
    private UInt32 cookie = null;

    private JDAntiStandbyLinux() {
        /* TODO: change, rewrite unix-socket to use dynamic path for library */
        try {
            c = DBusConnection.getConnection(DBusConnection.SESSION);
            sm = (GnomeSessionManager) c.getRemoteObject("org.gnome.SessionManager", "/org/gnome/SessionManager", GnomeSessionManager.class);
        } catch (Exception e) {
            JDLogger.exception(e);
            if (c != null) {
                c.disconnect();
            }
            c = null;
            sm = null;
        }
    }

    public synchronized void stop() {
        if (!possible()) return;
        c.disconnect();
        c = null;
        sm = null;
    }

    public synchronized boolean possible() {
        return c != null && sm != null;
    }

    public static JDAntiStandbyLinux getInstance() {
        return INSTANCE;
    }

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
    }

    public synchronized boolean enable() {
        if (!possible()) return false;
        if (cookie != null) return true;
        cookie = sm.Inhibit("JDownloader", new UInt32(0), "active", new UInt32(1 | 4));
        return cookie.intValue() != 0;
    }

    public synchronized boolean isEnabled() {
        return cookie != null;
    }

    public synchronized void disable() {
        if (!possible() && cookie == null) return;
        sm.Uninhibit(cookie);
        cookie = null;
    }
}
