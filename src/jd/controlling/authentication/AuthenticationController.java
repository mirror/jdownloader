package jd.controlling.authentication;

import java.util.ArrayList;

import jd.controlling.authentication.AuthenticationInfo.Type;
import jd.http.Browser;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.logging.Log;

public class AuthenticationController {
    private static final AuthenticationController INSTANCE = new AuthenticationController();

    /**
     * get the only existing instance of AuthenticationController. This is a singleton
     * 
     * @return
     */
    public static AuthenticationController getInstance() {
        return AuthenticationController.INSTANCE;
    }

    private AuthenticationControllerSettings config;
    private ArrayList<AuthenticationInfo>    list;
    private ChangeEventSender                eventSender = new ChangeEventSender();

    /**
     * Create a new instance of AuthenticationController. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private AuthenticationController() {
        config = JsonConfig.create(AuthenticationControllerSettings.class);
        list = cleanup(config.getList());
        if (list == null) list = new ArrayList<AuthenticationInfo>();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public void onShutdown(final Object shutdownRequest) {
                config.setList(list);
            }

            @Override
            public String toString() {
                return "save auths...";
            }
        });
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    public synchronized java.util.List<AuthenticationInfo> list() {
        return new ArrayList<AuthenticationInfo>(list);
    }

    /* remove invalid entries...without hostmask or without logins */
    private ArrayList<AuthenticationInfo> cleanup(ArrayList<AuthenticationInfo> input) {
        if (input == null) return null;
        ArrayList<AuthenticationInfo> ret = new ArrayList<AuthenticationInfo>(input.size());
        for (AuthenticationInfo item : input) {
            if (StringUtils.isEmpty(item.getHostmask())) continue;
            if (StringUtils.isEmpty(item.getPassword()) && StringUtils.isEmpty(item.getPassword())) continue;
            ret.add(item);
        }
        return ret;
    }

    public void add(AuthenticationInfo a) {
        if (a == null) return;
        synchronized (this) {
            ArrayList<AuthenticationInfo> newList = new ArrayList<AuthenticationInfo>(list);
            newList.add(a);
            list = newList;
            config.setList(list);
        }
        eventSender.fireEvent(new ChangeEvent(this));
    }

    public void remove(AuthenticationInfo a) {
        if (a == null) return;
        synchronized (this) {
            ArrayList<AuthenticationInfo> newList = new ArrayList<AuthenticationInfo>(list);
            newList.remove(a);
            list = newList;
            config.setList(list);
        }
        eventSender.fireEvent(new ChangeEvent(this));
    }

    public void remove(java.util.List<AuthenticationInfo> selectedObjects) {
        if (selectedObjects == null) return;
        synchronized (this) {
            ArrayList<AuthenticationInfo> newList = new ArrayList<AuthenticationInfo>(list);
            newList.removeAll(selectedObjects);
            list = newList;
            config.setList(list);
        }
        eventSender.fireEvent(new ChangeEvent(this));
    }

    public String[] getLogins(String url) {
        if (StringUtils.isEmpty(url)) return null;
        AuthenticationInfo.Type type = null;
        if (url.startsWith("ftp")) {
            type = Type.FTP;
        } else if (url.startsWith("http")) {
            type = Type.HTTP;
        } else {
            Log.L.info("Unknown Protocoll: " + url);
            return null;
        }
        java.util.List<AuthenticationInfo> llist = list;
        AuthenticationInfo bestMatch = null;
        String urlHost = Browser.getHost(url, true);
        for (AuthenticationInfo info : llist) {
            if (!info.isEnabled()) continue;
            String authHost = info.getHostmask();
            if (info.getType().equals(type) && !StringUtils.isEmpty(authHost)) {
                boolean contains = false;
                if (authHost.length() > urlHost.length()) {
                    /* hostMask of AuthenticationInfo is longer */
                    contains = authHost.contains(urlHost);
                } else {
                    /* hostMask of urlHost is longer */
                    contains = urlHost.contains(authHost);
                }
                if (contains) {
                    if (bestMatch == null) {
                        /* our first hit */
                        bestMatch = info;
                    } else if (authHost.length() <= urlHost.length() && info.getHostmask().length() > bestMatch.getHostmask().length()) {
                        /*
                         * best match in case we have a longer contains length but the authHost must not be longer than urlHost
                         */
                        bestMatch = info;
                    }
                }
            }
        }
        if (bestMatch == null) return null;
        return new String[] { bestMatch.getUsername(), bestMatch.getPassword() };
    }
}
