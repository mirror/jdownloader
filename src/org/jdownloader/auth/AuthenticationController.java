package org.jdownloader.auth;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialogInterface;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

import jd.http.Authentication;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.CallbackAuthenticationFactory;
import jd.http.DefaultAuthenticanFactory;
import jd.http.Request;
import jd.http.URLUserInfoAuthentication;

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
                save(AuthenticationController.this.authenticationInfos);
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

    private void save(List<AuthenticationInfo> authenticationInfos) {
        config.setList(new ArrayList<AuthenticationInfo>(authenticationInfos));
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    public List<AuthenticationInfo> list() {
        return authenticationInfos;
    }

    /* remove invalid entries...without hostmask or without logins */
    private CopyOnWriteArrayList<AuthenticationInfo> cleanup(List<AuthenticationInfo> infos) {
        final CopyOnWriteArrayList<AuthenticationInfo> ret = new CopyOnWriteArrayList<AuthenticationInfo>();
        if (infos != null && infos.size() > 0) {
            for (final AuthenticationInfo info : infos) {
                if (StringUtils.isEmpty(info.getHostmask())) {
                    continue;
                } else if (info.getType() == null) {
                    continue;
                } else if (StringUtils.isAllEmpty(info.getUsername(), info.getPassword())) {
                    continue;
                } else {
                    ret.add(info);
                }
            }
        }
        return ret;
    }

    public boolean add(AuthenticationInfo a) {
        if (a != null && authenticationInfos.addIfAbsent(a)) {
            save(authenticationInfos);
            eventSender.fireEvent(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(AuthenticationInfo a) {
        if (a != null && authenticationInfos.remove(a)) {
            save(authenticationInfos);
            eventSender.fireEvent(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(List<AuthenticationInfo> selectedObjects) {
        if (selectedObjects != null && authenticationInfos.removeAll(selectedObjects)) {
            save(authenticationInfos);
            eventSender.fireEvent(new ChangeEvent(this));
            return true;
        } else {
            return false;
        }
    }

    public Login getBestLogin(final URL url, final String realm) {
        final List<Login> ret = getSortedLoginsList(url, realm);
        if (ret != null && ret.size() > 0) {
            return ret.get(0);
        } else {
            return null;
        }
    }

    public List<AuthenticationFactory> buildAuthenticationFactories(final URL url, final String realm) {
        final List<AuthenticationFactory> authenticationFactories = new ArrayList<AuthenticationFactory>();
        if (url.getUserInfo() != null) {
            authenticationFactories.add(new URLUserInfoAuthentication());
        }
        authenticationFactories.addAll(getSortedAuthenticationFactories(url, realm));
        authenticationFactories.add(new CallbackAuthenticationFactory() {
            protected Authentication remember = null;

            @Override
            protected Authentication askAuthentication(Browser browser, Request request, final String realm) {
                final LoginDialog loginDialog = new LoginDialog(UIOManager.LOGIC_COUNTDOWN, _JDT.T.Plugin_requestLogins_message(), _JDT.T.AuthExceptionGenericBan_toString(url.toExternalForm()), new AbstractIcon(IconKey.ICON_PASSWORD, 32));
                loginDialog.setTimeout(60 * 1000);
                final LoginDialogInterface handle = UIOManager.I().show(LoginDialogInterface.class, loginDialog);
                if (handle.getCloseReason() == CloseReason.OK) {
                    final Authentication ret = new DefaultAuthenticanFactory(request.getURL().getHost(), realm, handle.getUsername(), handle.getPassword()).buildAuthentication(browser, request);
                    addAuthentication(ret);
                    if (handle.isRememberSelected()) {
                        remember = ret;
                    }
                    return ret;
                } else {
                    return null;
                }
            }

            @Override
            public boolean retry(Authentication authentication, Browser browser, Request request) {
                if (containsAuthentication(authentication) && remember == authentication && request.getAuthentication() == authentication && !requiresAuthentication(request)) {
                    final AuthenticationInfo auth = new AuthenticationInfo();
                    auth.setRealm(authentication.getRealm());
                    auth.setUsername(authentication.getUsername());
                    auth.setPassword(authentication.getPassword());
                    auth.setHostmask(authentication.getHost());
                    auth.setType(Type.HTTP);
                    AuthenticationController.getInstance().add(auth);
                }
                return super.retry(authentication, browser, request);
            }
        });
        return authenticationFactories;
    }

    public List<AuthenticationFactory> getSortedAuthenticationFactories(final URL url, final String realm) {
        final List<AuthenticationFactory> ret = new ArrayList<AuthenticationFactory>();
        final List<Login> logins = getSortedLoginsList(url, realm);
        if (logins != null && logins.size() > 0) {
            for (final Login login : logins) {
                ret.add(new DefaultAuthenticanFactory(login.getHost(), login.getRealm(), login.getUsername(), login.getPassword()) {
                    protected boolean requiresAuthentication(final Request request) {
                        if (login.isAlwaysFlag()) {
                            return getWWWAuthenticate(request) != null;
                        } else {
                            return super.requiresAuthentication(request);
                        }
                    }

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
        final List<Login> ret = new ArrayList<Login>();
        final AuthenticationInfo.Type type;
        final String protocol = url.getProtocol();
        if (protocol != null && protocol.matches("(?i)^ftp$")) {
            type = Type.FTP;
        } else if (protocol != null && protocol.matches("(?i)^https?$")) {
            type = Type.HTTP;
        } else {
            LogController.getRebirthLogger(logger).info("Unknown Protocoll: " + url);
            return ret;
        }
        final List<AuthenticationInfo> infos = new ArrayList<AuthenticationInfo>();
        final String urlHost = url.getHost();
        for (final AuthenticationInfo info : authenticationInfos) {
            if (!info.isEnabled()) {
                continue;
            } else if (realm != null && !StringUtils.equalsIgnoreCase(realm, info.getRealm())) {
                continue;
            } else {
                final String hostMask = info.getHostmask();
                if (info.getType().equals(type) && !StringUtils.isEmpty(hostMask)) {
                    final boolean contains;
                    if (hostMask.matches(".*(\\*|\\[|\\(|\\||\\?|\\{).*")) {
                        String pattern = hostMask;
                        Boolean matches = null;
                        try {
                            // check with normal pattern
                            matches = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(urlHost).matches();
                        } catch (PatternSyntaxException e) {
                        }
                        if (!Boolean.TRUE.equals(matches)) {
                            // check again with simple pattern
                            try {
                                pattern = hostMask.replace("*", ".*");
                                matches = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(urlHost).matches();
                            } catch (PatternSyntaxException e2) {
                            }
                        }
                        contains = Boolean.TRUE.equals(matches);
                    } else if (hostMask.length() > urlHost.length()) {
                        /* hostMask of AuthenticationInfo is longer */
                        contains = StringUtils.containsIgnoreCase(hostMask, urlHost);
                    } else {
                        /* hostMask of urlHost is longer */
                        contains = StringUtils.containsIgnoreCase(urlHost, hostMask);
                    }
                    if (contains) {
                        infos.add(info);
                    }
                }
            }
        }
        try {
            Collections.sort(infos, new Comparator<AuthenticationInfo>() {
                private String getRealm(final AuthenticationInfo ai) {
                    final String realm = ai.getRealm();
                    if (realm == null) {
                        return "";
                    } else {
                        return realm;
                    }
                }

                private int compare(long x, long y) {
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                }

                private int compare(boolean x, boolean y) {
                    return (x == y) ? 0 : (x ? 1 : -1);
                }

                @Override
                public int compare(AuthenticationInfo o1, AuthenticationInfo o2) {
                    int ret = compare(getRealm(o2).length(), getRealm(o1).length());
                    if (ret == 0) {
                        ret = compare(o2.getHostmask().length(), o1.getHostmask().length());
                    }
                    if (ret == 0) {
                        ret = compare(o2.isAlwaysFlag(), o1.isAlwaysFlag());
                    }
                    if (ret == 0) {
                        ret = compare(o2.getLastValidated(), o1.getLastValidated());
                    }
                    if (ret == 0) {
                        ret = compare(o2.getCreated(), o1.getCreated());
                    }
                    return ret;
                }
            });
        } catch (Throwable e) {
            LogController.getRebirthLogger(logger).log(e);
        }
        for (final AuthenticationInfo info : infos) {
            ret.add(new Login(info.getType(), info.getHostmask(), info.getRealm(), info.getUsername(), info.getPassword(), info.isAlwaysFlag()) {
                @Override
                public void validate() {
                    info.setLastValidated(System.currentTimeMillis());
                };
            });
        }
        return ret;
    }
}
