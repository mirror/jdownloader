package org.jdownloader.auth;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.http.Authentication;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.DefaultAuthenticanFactory;
import jd.http.Request;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.logging.LogController;

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

    private final AuthenticationControllerSettings         config;
    private final CopyOnWriteArrayList<AuthenticationInfo> authenticationInfos;
    private final ChangeEventSender                        eventSender = new ChangeEventSender();
    private final LogSource                                logger;

    /**
     * Create a new i nstance of AuthenticationController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private AuthenticationController() {
        logger = LogController.getInstance().getLogger(AuthenticationController.class.getName());
        config = JsonConfig.create(AuthenticationControllerSettings.class);
        final CopyOnWriteArrayList<AuthenticationInfo> list = cleanup(config.getList());
        if (list == null) {
            this.authenticationInfos = new CopyOnWriteArrayList<AuthenticationInfo>();
        } else {
            this.authenticationInfos = list;
        }
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                config.setList(AuthenticationController.this.authenticationInfos);
            }

            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public String toString() {
                return "ShutdownEvent: Save AuthController";
            }
        });
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    public java.util.List<AuthenticationInfo> list() {
        return authenticationInfos;
    }

    /* remove invalid entries...without hostmask or without logins */
    private CopyOnWriteArrayList<AuthenticationInfo> cleanup(List<AuthenticationInfo> infos) {
        if (infos == null || infos.size() == 0) {
            return null;
        }
        final CopyOnWriteArrayList<AuthenticationInfo> ret = new CopyOnWriteArrayList<AuthenticationInfo>();
        for (final AuthenticationInfo info : infos) {
            if (StringUtils.isEmpty(info.getHostmask())) {
                continue;
            }
            if (info.getType() == null) {
                continue;
            }
            if (StringUtils.isAllEmpty(info.getPassword(), info.getPassword())) {
                continue;
            }
            ret.add(info);
        }
        return ret;
    }

    public boolean add(AuthenticationInfo a) {
        if (a != null && authenticationInfos.addIfAbsent(a)) {
            config.setList(authenticationInfos);
            eventSender.fireEvent(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(AuthenticationInfo a) {
        if (a != null && authenticationInfos.remove(a)) {
            config.setList(authenticationInfos);
            eventSender.fireEvent(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(java.util.List<AuthenticationInfo> selectedObjects) {
        if (selectedObjects != null && authenticationInfos.removeAll(selectedObjects)) {
            config.setList(authenticationInfos);
            eventSender.fireEvent(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public Login getBestLogin(URL url, final String realm) {
        final List<Login> ret = getSortedLoginsList(url, realm);
        if (ret != null && ret.size() > 0) {
            return ret.get(0);
        } else {
            return null;
        }
    }

    public List<AuthenticationFactory> getSortedAuthenticationFactories(final URL url, final String realm) {
        final List<AuthenticationFactory> ret = new ArrayList<AuthenticationFactory>();
        final List<Login> logins = getSortedLoginsList(url, realm);
        if (logins != null) {
            for (final Login login : logins) {
                ret.add(new DefaultAuthenticanFactory(login.getHost(), login.getRealm(), login.getUsername(), login.getPassword()) {
                    @Override
                    public boolean retry(Authentication authentication, Browser browser, Request request) {
                        if (containsAuthentication(authentication) && request.getAuthentication() == authentication && !requiresAuthentication(request)) {
                            login.validate();
                        }
                        return super.retry(authentication, browser, request);
                    };
                });
            }
        }
        return ret;
    }

    public List<Login> getSortedLoginsList(final URL url, final String realm) {
        final AuthenticationInfo.Type type;
        if (StringUtils.equalsIgnoreCase(url.getProtocol(), "ftp")) {
            type = Type.FTP;
        } else if (StringUtils.equalsIgnoreCase(url.getProtocol(), "https") || StringUtils.equalsIgnoreCase(url.getProtocol(), "http")) {
            type = Type.HTTP;
        } else {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Unknown Protocoll: " + url);
            return null;
        }
        final List<AuthenticationInfo> selection = new ArrayList<AuthenticationInfo>();
        final String urlHost = Browser.getHost(url, true);
        for (final AuthenticationInfo info : authenticationInfos) {
            if (!info.isEnabled()) {
                continue;
            }
            if (realm != null && !StringUtils.equalsIgnoreCase(realm, info.getRealm())) {
                continue;
            }
            final String authHost = info.getHostmask();
            if (info.getType().equals(type) && !StringUtils.isEmpty(authHost)) {
                final boolean contains;
                if (authHost.length() > urlHost.length()) {
                    /* hostMask of AuthenticationInfo is longer */
                    contains = authHost.contains(urlHost);
                } else {
                    /* hostMask of urlHost is longer */
                    contains = urlHost.contains(authHost);
                }
                if (contains) {
                    selection.add(info);
                }
            }
        }
        try {
            Collections.sort(selection, new Comparator<AuthenticationInfo>() {
                private String getRealm(AuthenticationInfo ai) {
                    final String realm = ai.getRealm();
                    if (realm == null) {
                        return "";
                    } else {
                        return realm;
                    }
                }

                @Override
                public int compare(AuthenticationInfo o1, AuthenticationInfo o2) {
                    int ret = Integer.compare(getRealm(o2).length(), getRealm(o1).length());
                    if (ret == 0) {
                        ret = Integer.compare(o2.getHostmask().length(), o1.getHostmask().length());
                    }
                    if (ret == 0) {
                        ret = Long.compare(o2.getLastValidated(), o1.getLastValidated());
                    }
                    if (ret == 0) {
                        ret = Long.compare(o2.getCreated(), o1.getCreated());
                    }
                    return ret;
                }
            });
        } catch (Throwable e) {
            logger.log(e);
        }
        final List<Login> ret = new ArrayList<Login>();
        for (final AuthenticationInfo info : selection) {
            ret.add(new Login(info.getType(), info.getHostmask(), info.getRealm(), info.getUsername(), info.getPassword()) {
                @Override
                public void validate() {
                    info.setLastValidated(System.currentTimeMillis());
                };
            });
        }
        return ret;
    }

    public void validate(Login login, String url) {
        if (StringUtils.isEmpty(url)) {
            return;
        }
        AuthenticationInfo.Type type = null;
        if (url.startsWith("ftp")) {
            type = Type.FTP;
        } else if (url.startsWith("http")) {
            type = Type.HTTP;
        } else {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Unknown Protocoll: " + url);
            return;
        }
        String urlHost = Browser.getHost(url, true);
        for (AuthenticationInfo info : authenticationInfos) {
            if (!info.isEnabled()) {
                continue;
            }
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
                    if (StringUtils.equals(info.getUsername(), login.getUsername()) && StringUtils.equals(info.getPassword(), login.getPassword())) {
                        info.setLastValidated(System.currentTimeMillis());
                    }
                }
            }
        }
    }
}
