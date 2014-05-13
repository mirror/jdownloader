package jd.controlling.proxy;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import jd.config.SubConfiguration;
import jd.http.Request;
import jd.http.StaticProxy;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.DefaultEventSender;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProxyDialog;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InternetConnectionSettings;
import org.jdownloader.updatev2.ProxyClone;
import org.jdownloader.updatev2.ProxyData;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.browser.firefox.FirefoxProxySearchStrategy;
import com.btr.proxy.search.desktop.DesktopProxySearchStrategy;
import com.btr.proxy.search.env.EnvProxySearchStrategy;
import com.btr.proxy.search.java.JavaProxySearchStrategy;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.PacScriptParser;
import com.btr.proxy.selector.pac.PacScriptSource;
import com.btr.proxy.selector.pac.UrlPacScriptSource;
import com.btr.proxy.util.Logger;
import com.btr.proxy.util.Logger.LogBackEnd;
import com.btr.proxy.util.Logger.LogLevel;

public class ProxyController {

    private static final ProxyController INSTANCE = new ProxyController();

    public static final ProxyController getInstance() {
        return ProxyController.INSTANCE;
    }

    private java.util.List<SingleBasicProxySelectorImpl>              proxies          = new ArrayList<SingleBasicProxySelectorImpl>();
    private List<SingleDirectGatewaySelector>                         directs          = new ArrayList<SingleDirectGatewaySelector>();
    private java.util.List<PacProxySelectorImpl>                      pacs             = new ArrayList<PacProxySelectorImpl>();
    private AbstractProxySelectorImpl                                 defaultproxy     = null;

    private final NoProxySelector                                     none;

    private DefaultEventSender<ProxyEvent<AbstractProxySelectorImpl>> eventSender      = null;

    private InternetConnectionSettings                                config;

    private GeneralSettings                                           generalConfig    = null;

    private final Object                                              LOCK             = new Object();

    private LogSource                                                 logger;

    public static final String                                        USE_PROXY        = "USE_PROXY";

    public static final String                                        PROXY_HOST       = "PROXY_HOST";
    public static final String                                        PROXY_PASS       = "PROXY_PASS";
    public static final String                                        PROXY_PASS_SOCKS = "PROXY_PASS_SOCKS";
    public static final String                                        PROXY_PORT       = "PROXY_PORT";
    public static final String                                        PROXY_USER       = "PROXY_USER";
    public static final String                                        PROXY_USER_SOCKS = "PROXY_USER_SOCKS";
    public static final String                                        SOCKS_HOST       = "SOCKS_HOST";
    public static final String                                        SOCKS_PORT       = "SOCKS_PORT";
    public static final String                                        USE_SOCKS        = "USE_SOCKS";

    public static List<HTTPProxy> autoConfig() {
        final java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        try {
            if (CrossSystem.isWindows()) {
                return HTTPProxy.getWindowsRegistryProxies();
            }
            /* we enable systemproxies to query them for a test getPage */
            System.setProperty("java.net.useSystemProxies", "true");
            List<Proxy> l = null;
            try {
                l = ProxySelector.getDefault().select(new URI("http://www.appwork.org"));
            } finally {
                System.setProperty("java.net.useSystemProxies", "false");
            }
            for (final Proxy p : l) {
                final SocketAddress ad = p.address();
                if (ad != null && ad instanceof InetSocketAddress) {
                    final InetSocketAddress isa = (InetSocketAddress) ad;
                    if (StringUtils.isEmpty(isa.getHostName())) {
                        continue;
                    }
                    switch (p.type()) {
                    case HTTP: {
                        final HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.HTTP);
                        pd.setHost(isa.getHostName());
                        pd.setPort(isa.getPort());
                        ret.add(pd);
                    }
                        break;
                    case SOCKS: {
                        final HTTPProxy pd = new HTTPProxy(HTTPProxy.TYPE.SOCKS5);
                        pd.setHost(isa.getHostName());
                        pd.setPort(isa.getPort());
                        ret.add(pd);
                    }
                        break;
                    }
                }
            }
        } catch (final Throwable e1) {
            Log.exception(e1);
        }
        return ret;
    }

    private ProxyController() {
        this.eventSender = new DefaultEventSender<ProxyEvent<AbstractProxySelectorImpl>>();
        /* init needed configs */
        this.config = JsonConfig.create(InternetConnectionSettings.PATH, InternetConnectionSettings.class);
        this.generalConfig = JsonConfig.create(GeneralSettings.class);
        this.logger = LogController.getInstance().getLogger(ProxyController.class.getName());
        /* init our NONE proxy */
        this.none = new NoProxySelector();
        this.loadProxySettings();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                ProxyController.this.saveProxySettings();
            }

            @Override
            public String toString() {
                return "ProxyController: save config";
            }
        });

        this.eventSender.addListener(new DefaultEventListener<ProxyEvent<AbstractProxySelectorImpl>>() {
            DelayedRunnable asyncSaving = new DelayedRunnable(5000l, 60000l) {
                                            @Override
                                            public void delayedrun() {
                                                ProxyController.this.saveProxySettings();
                                            }

                                            @Override
                                            public String getID() {
                                                return "ProxyController";
                                            }

                                        };

            @Override
            public void onEvent(final ProxyEvent<AbstractProxySelectorImpl> event) {
                this.asyncSaving.resetAndStart();
            }
        });
    }

    /**
     * add proxy to proxylist in case its not in it yet
     * 
     * @param proxy
     */
    public void addProxy(final AbstractProxySelectorImpl proxy) {
        if (proxy == null) {
            return;
        }

        synchronized (this.LOCK) {

            switch (proxy.getType()) {
            case HTTP:
            case SOCKS4:
            case SOCKS5:
                if (!new HashSet<AbstractProxySelectorImpl>(this.proxies).add(proxy)) {
                    return;
                }
                final ArrayList<SingleBasicProxySelectorImpl> nproxies = new ArrayList<SingleBasicProxySelectorImpl>(this.proxies);
                nproxies.add((SingleBasicProxySelectorImpl) proxy);
                this.proxies = nproxies;
                break;
            case DIRECT:
                if (!new HashSet<AbstractProxySelectorImpl>(this.directs).add(proxy)) {
                    return;
                }
                final ArrayList<SingleDirectGatewaySelector> ndirects = new ArrayList<SingleDirectGatewaySelector>(this.directs);
                ndirects.add((SingleDirectGatewaySelector) proxy);
                this.directs = ndirects;
                break;
            case PAC:
                if (!new HashSet<AbstractProxySelectorImpl>(this.pacs).add(proxy)) {
                    return;
                }
                final ArrayList<PacProxySelectorImpl> npacs = new ArrayList<PacProxySelectorImpl>(this.pacs);
                npacs.add((PacProxySelectorImpl) proxy);
                this.pacs = npacs;
                break;

            default:
                this.logger.info("Invalid Type " + proxy.getType());
                return;
            }

        }
        this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.ADDED, proxy));
    }

    public void addProxy(final List<HTTPProxy> nProxies) {
        if (nProxies == null) {
            return;
        }
        final ExtProxy ret = null;
        boolean changes = false;
        synchronized (this.LOCK) {
            final HashSet<AbstractProxySelectorImpl> dupe = new HashSet<AbstractProxySelectorImpl>(this.proxies);
            final ArrayList<SingleBasicProxySelectorImpl> nproxies = new ArrayList<SingleBasicProxySelectorImpl>(this.proxies);

            for (final HTTPProxy proxy : nProxies) {
                final AbstractProxySelectorImpl proxyFac = new SingleBasicProxySelectorImpl(proxy);
                if (proxyFac == null || !(proxyFac instanceof SingleBasicProxySelectorImpl) || !dupe.add(proxyFac)) {
                    continue;
                }

                changes = true;
                nproxies.add((SingleBasicProxySelectorImpl) proxyFac);
            }

            this.proxies = nproxies;
        }

        if (changes) {
            this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.ADDED, null));
            this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REFRESH, null));
        }
    }

    public boolean askForProxyAuth(AbstractProxySelectorImpl selector, final int flags, final boolean typeEditable, final HTTPProxy usedCopy, HTTPProxy orgReference, final URL url, final String msg, final String title) {
        HTTPProxy ret = null;
        boolean remember = false;
        if (usedCopy != null && !usedCopy.equals(orgReference)) {

            ret = orgReference;
        } else {
            synchronized (this) {

                if (orgReference instanceof ExtProxy && ((ExtProxy) orgReference).getFactory().isBanned(orgReference)) {
                    ret = null;

                } else if (usedCopy != null && !usedCopy.equals(orgReference)) {
                    // proxy has been updated already
                    ret = orgReference;
                } else {
                    try {

                        final ProxyDialog pd = new ProxyDialog(orgReference, msg) {
                            {
                                this.flagMask |= flags;
                            }

                            @Override
                            protected boolean isShowRemember() {
                                return true;
                            }

                            @Override
                            public void pack() {
                                getDialog().setMinimumSize(new Dimension(450, getPreferredSize().height));
                                super.pack();
                            }
                        };
                        pd.setTimeout(5 * 60 * 1000);
                        pd.setAuthRequired(true);
                        pd.setTypeEditable(typeEditable);
                        pd.setHostEditable(typeEditable);
                        pd.setPortEditable(typeEditable);
                        pd.setTitle(title);

                        ret = Dialog.getInstance().showDialog(pd);
                        remember = pd.isRememberChecked();
                    } catch (DialogClosedException e) {
                        ret = null;
                    } catch (DialogCanceledException e) {
                        ret = null;
                    }
                }
            }

        }

        if (selector instanceof PacProxySelectorImpl) {

            if (ret != null) {
                if (remember) {
                    ((PacProxySelectorImpl) selector).setUser(ret.getUser());
                    ((PacProxySelectorImpl) selector).setPassword(ret.getPass());
                    ((PacProxySelectorImpl) selector).setTempAuth(orgReference, ret.getUser(), ret.getPass());

                } else {
                    ((PacProxySelectorImpl) selector).setTempAuth(orgReference, ret.getUser(), ret.getPass());
                }
                return true;
            } else {
                ((PacProxySelectorImpl) selector).setTempAuth(orgReference, null, null);
                if (!selector.isBanned(orgReference)) {
                    this.setBan(selector, orgReference, null, 60 * 60 * 1000l, _JDT._.ProxyController_updateProxy_baned_auth());
                }

            }
        } else if (selector instanceof SingleBasicProxySelectorImpl) {
            if (ret != null) {
                if (remember) {
                    ((SingleBasicProxySelectorImpl) selector).setUser(ret.getUser());
                    ((SingleBasicProxySelectorImpl) selector).setPassword(ret.getPass());

                } else {
                    ((SingleBasicProxySelectorImpl) selector).setTempAuth(ret.getUser(), ret.getPass());
                }
                return true;
            } else {
                ((SingleBasicProxySelectorImpl) selector).setTempAuth(null, null);
                if (!selector.isBanned(orgReference)) {

                    this.setBan(selector, orgReference, null, 60 * 60 * 1000l, _JDT._.ProxyController_updateProxy_baned_auth());
                }
            }
        }
        return false;
    }

    public void collect(final String host, final int maxActive, final List<AbstractProxySelectorImpl> ret, final AbstractProxySelectorImpl selector) {
        if (selector.isProxyRotationEnabled()) {
            if (selector.isBanned(host)) {
                return;
            }
            final int active = selector.activeDownloadsbyHosts(host);
            if (active < maxActive) {
                ret.add(selector);
            }

        }
    }

    public void exportTo(final File saveTo) throws UnsupportedEncodingException, IOException {
        final ProxyExportImport save = new ProxyExportImport();

        final AbstractProxySelectorImpl ldefaultproxy = this.defaultproxy;
        {
            /* use own scope */
            final ArrayList<ProxyData> ret = new ArrayList<ProxyData>(this.proxies.size());
            final List<SingleBasicProxySelectorImpl> lproxies = this.proxies;
            for (final SingleBasicProxySelectorImpl proxy : lproxies) {
                final ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }

            final List<PacProxySelectorImpl> lpacs = this.pacs;
            for (final PacProxySelectorImpl proxy : lpacs) {
                final ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            save.setCustomProxyList(ret);
        }

        save.setNoneDefault(this.none == ldefaultproxy);
        save.setNoneRotationEnabled(this.none.isProxyRotationEnabled());

        IO.secureWrite(saveTo, JSonStorage.serializeToJson(save).getBytes("UTF-8"));
    }

    private List<HTTPProxy> getAvailableDirects() {
        final List<InetAddress> ips = HTTPProxyUtils.getLocalIPs();
        LogController.CL().info(ips.toString());
        final java.util.List<HTTPProxy> directs = new ArrayList<HTTPProxy>(ips.size());
        if (ips.size() > 1) {
            // we can use non if we have only one WAN ips anyway
            for (final InetAddress ip : ips) {
                directs.add(new HTTPProxy(ip));
            }
        }
        return directs;
    }

    /**
     * returns the default proxy for all normal browser activity as well as for premium usage
     * 
     * @return
     */
    public AbstractProxySelectorImpl getDefaultProxy() {
        return this.defaultproxy;
    }

    public DefaultEventSender<ProxyEvent<AbstractProxySelectorImpl>> getEventSender() {
        return this.eventSender;
    }

    public String getLatestProfilePath() {
        return this.config.getLatestProfile();
    }

    /**
     * returns a copy of current proxy list
     * 
     * @return
     */
    public java.util.List<AbstractProxySelectorImpl> getList() {
        final java.util.List<AbstractProxySelectorImpl> ret = new ArrayList<AbstractProxySelectorImpl>(this.directs.size() + this.proxies.size() + this.pacs.size() + 1);
        ret.add(this.none);

        ret.addAll(this.directs);
        ret.addAll(this.proxies);
        ret.addAll(this.pacs);
        return ret;
    }

    public NoProxySelector getNone() {
        return this.none;
    }

    public List<AbstractProxySelectorImpl> getPossibleProxies(String host, final boolean accountInUse, final int maxActive) {
        final List<AbstractProxySelectorImpl> ret = new ArrayList<AbstractProxySelectorImpl>();
        host = host.toLowerCase(Locale.ENGLISH);
        if (accountInUse) {
            final AbstractProxySelectorImpl ldefaultProxy = this.defaultproxy;
            if (ldefaultProxy.isAllowedByFilter(host)) {
                final int active = ldefaultProxy.activeDownloadsbyHosts(host);
                if (active < maxActive) {
                    ret.add(ldefaultProxy);
                }
                return ret;
            }
            // no proxy found. search first one in rotation
            if (this.none.isProxyRotationEnabled()) {
                if (this.none.isAllowedByFilter(host)) {
                    final int active = this.none.activeDownloadsbyHosts(host);
                    if (active < maxActive) {
                        ret.add(this.none);
                    }
                    return ret;
                }
            }
            final List<SingleDirectGatewaySelector> ldirects = this.directs;

            for (final SingleDirectGatewaySelector info : ldirects) {
                if (!info.isAllowedByFilter(host)) {
                    continue;
                }
                this.collect(host, maxActive, ret, info);
                return ret;
            }
            final List<PacProxySelectorImpl> lpacs = this.pacs;
            for (final PacProxySelectorImpl info : lpacs) {
                if (!info.isAllowedByFilter(host)) {
                    continue;
                }
                this.collect(host, maxActive, ret, info);
                return ret;
            }

            final List<SingleBasicProxySelectorImpl> lProxies = this.proxies;
            for (final SingleBasicProxySelectorImpl info : lProxies) {
                if (!info.isAllowedByFilter(host)) {
                    continue;
                }
                this.collect(host, maxActive, ret, info);
                return ret;
            }
        } else {
            if (this.none.isProxyRotationEnabled()) {
                if (this.none.isAllowedByFilter(host)) {
                    final int active = this.none.activeDownloadsbyHosts(host);
                    if (active < maxActive) {
                        ret.add(this.none);
                    }
                }
            }
            final List<SingleDirectGatewaySelector> ldirects = this.directs;

            for (final SingleDirectGatewaySelector info : ldirects) {
                if (!info.isAllowedByFilter(host)) {
                    continue;
                }
                this.collect(host, maxActive, ret, info);
            }
            final List<PacProxySelectorImpl> lpacs = this.pacs;
            for (final PacProxySelectorImpl info : lpacs) {
                if (!info.isAllowedByFilter(host)) {
                    continue;
                }
                this.collect(host, maxActive, ret, info);
            }

            final List<SingleBasicProxySelectorImpl> lProxies = this.proxies;
            for (final SingleBasicProxySelectorImpl info : lProxies) {
                if (!info.isAllowedByFilter(host)) {
                    continue;
                }
                this.collect(host, maxActive, ret, info);
            }
        }
        return ret;
    }

    public boolean hasRotation() {
        if (this.none.isProxyRotationEnabled()) {
            return true;
        }
        final List<SingleDirectGatewaySelector> ldirects = this.directs;
        for (final SingleDirectGatewaySelector pi : ldirects) {
            if (pi.isProxyRotationEnabled()) {
                return true;
            }
        }
        final List<SingleBasicProxySelectorImpl> lproxies = this.proxies;
        for (final SingleBasicProxySelectorImpl pi : lproxies) {
            if (pi.isProxyRotationEnabled()) {
                return true;
            }
        }

        final List<PacProxySelectorImpl> lpacs = this.pacs;
        for (final PacProxySelectorImpl pi : lpacs) {
            if (pi.isProxyRotationEnabled()) {
                return true;
            }
        }
        return false;
    }

    public void importFrom(final File selected) throws IOException {
        final ProxyExportImport restore = JSonStorage.restoreFromString(IO.readFileToString(selected), new TypeRef<ProxyExportImport>() {
        });
        this.config.setLatestProfile(selected.getAbsolutePath());
        this.config.setCustomProxyList(restore.getCustomProxyList());
        this.config.setNoneDefault(restore.isNoneDefault());
        this.config.setNoneRotationEnabled(restore.isNoneRotationEnabled());
        this.loadProxySettings();
    }

    private void loadProxySettings() {
        AbstractProxySelectorImpl newDefaultProxy = null;
        boolean rotCheck = false;
        final java.util.List<SingleBasicProxySelectorImpl> proxies = new ArrayList<SingleBasicProxySelectorImpl>();
        final java.util.List<SingleDirectGatewaySelector> directs = new ArrayList<SingleDirectGatewaySelector>();
        final java.util.List<PacProxySelectorImpl> pacs = new ArrayList<PacProxySelectorImpl>();
        final HashSet<AbstractProxySelectorImpl> dupeCheck = new HashSet<AbstractProxySelectorImpl>();
        AbstractProxySelectorImpl proxy = null;
        logger.info("Load Proxy Settings");
        {
            /* restore customs proxies */
            /* use own scope */
            final java.util.List<ProxyData> ret = this.config.getCustomProxyList();

            logger.info("Customs: " + (ret == null ? 0 : ret.size()));
            if (ret != null) {
                /* config available */
                restore: for (final ProxyData proxyData : ret) {
                    try {
                        // HTTPProxy proxyTemplate = HTTPProxy.getHTTPProxy(proxyData.getProxy());
                        // if (proxyTemplate != null) {
                        logger.info(JSonStorage.serializeToJson(proxyData));
                        if (proxyData.isPac()) {
                            proxy = new PacProxySelectorImpl(proxyData);
                        } else {
                            ;
                            switch (proxyData.getProxy().getType()) {
                            case DIRECT:
                                proxy = new SingleDirectGatewaySelector(proxyData);
                                break;
                            case HTTP:
                            case SOCKS4:
                            case SOCKS5:
                                proxy = new SingleBasicProxySelectorImpl(proxyData);
                                break;
                            case NONE:

                                System.out.println(1);

                            default:
                                continue;
                            }
                        }
                        if (proxy == null) {
                            continue;
                        }
                        if (!dupeCheck.add(proxy)) {
                            continue restore;
                        }
                        dupeCheck.add(proxy);
                        if (proxy instanceof PacProxySelectorImpl) {
                            pacs.add((PacProxySelectorImpl) proxy);
                        } else if (proxy instanceof SingleDirectGatewaySelector) {
                            directs.add((SingleDirectGatewaySelector) proxy);
                        } else if (proxy instanceof SingleBasicProxySelectorImpl) {
                            proxies.add((SingleBasicProxySelectorImpl) proxy);
                        } else {
                            throw new WTFException("Unknown Type: " + proxy.getClass());
                        }

                        if (proxyData.isDefaultProxy()) {
                            newDefaultProxy = proxy;
                        }
                        if (proxy.isProxyRotationEnabled()) {
                            rotCheck = true;
                            // }
                        }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            } else {
                logger.info("Init Proxy Controller fresh");
                Logger.setBackend(new LogBackEnd() {

                    @Override
                    public void log(final Class<?> arg0, final LogLevel arg1, final String arg2, final Object... arg3) {

                        logger.log(Level.ALL, arg2, arg3);
                    }

                    @Override
                    public boolean isLogginEnabled(final LogLevel arg0) {

                        return true;
                    }
                });

                ArrayList<ProxySearchStrategy> strategies = new ArrayList<ProxySearchStrategy>();
                strategies.add(new DesktopProxySearchStrategy());
                strategies.add(new FirefoxProxySearchStrategy());
                strategies.add(new EnvProxySearchStrategy());
                strategies.add(new JavaProxySearchStrategy());

                for (ProxySearchStrategy s : strategies) {
                    logger.info("Selector: " + s);
                    ProxySelector selector;
                    try {
                        selector = s.getProxySelector();
                        if (selector == null)
                            continue;
                        if (selector instanceof PacProxySelector) {
                            Field field = PacProxySelector.class.getDeclaredField("pacScriptParser");
                            field.setAccessible(true);
                            PacScriptParser source = (PacScriptParser) field.get(selector);
                            PacScriptSource pacSource = source.getScriptSource();
                            if (pacSource != null && pacSource instanceof UrlPacScriptSource) {
                                field = UrlPacScriptSource.class.getDeclaredField("scriptUrl");
                                field.setAccessible(true);

                                Object pacURL = field.get(pacSource);
                                if (StringUtils.isNotEmpty((String) pacURL)) {
                                    PacProxySelectorImpl pac = new PacProxySelectorImpl((String) pacURL, null, null);
                                    pac.setProxyRotationEnabled(true);
                                    if (dupeCheck.add(pac)) {
                                        logger.info("Add pac: " + pacURL);
                                        pacs.add(pac);
                                        if (newDefaultProxy == null) {
                                            newDefaultProxy = pac;
                                            config.setNoneDefault(false);
                                        }
                                    }

                                }
                            }
                        } else {
                            List<Proxy> sproxies = selector.select(new URI("http://google.com"));
                            if (sproxies != null) {
                                for (Proxy p : sproxies) {
                                    HTTPProxy httpProxy = null;
                                    switch (p.type()) {
                                    case DIRECT:
                                        if (p.address() == null) {
                                            httpProxy = new HTTPProxy(TYPE.NONE);

                                        } else {

                                            httpProxy = new HTTPProxy(((InetSocketAddress) p.address()).getAddress());
                                            SingleDirectGatewaySelector dir = new SingleDirectGatewaySelector(httpProxy);
                                            dir.setProxyRotationEnabled(true);
                                            if (dupeCheck.add(dir)) {
                                                logger.info("Add Direct: " + httpProxy);
                                                directs.add(dir);
                                                if (newDefaultProxy == null) {
                                                    newDefaultProxy = dir;
                                                    config.setNoneDefault(false);
                                                }
                                            }

                                        }
                                        break;
                                    case HTTP:
                                        httpProxy = new HTTPProxy(TYPE.HTTP, ((InetSocketAddress) p.address()).getHostString(), ((InetSocketAddress) p.address()).getPort());
                                        SingleBasicProxySelectorImpl basic = new SingleBasicProxySelectorImpl(httpProxy);
                                        basic.setProxyRotationEnabled(true);
                                        if (dupeCheck.add(basic)) {
                                            logger.info("Add Basic: " + httpProxy);
                                            proxies.add(basic);
                                            if (newDefaultProxy == null) {
                                                newDefaultProxy = basic;
                                                config.setNoneDefault(false);
                                            }
                                        }
                                        break;
                                    case SOCKS:

                                        httpProxy = new HTTPProxy(TYPE.SOCKS5, ((InetSocketAddress) p.address()).getHostString(), ((InetSocketAddress) p.address()).getPort());
                                        SingleBasicProxySelectorImpl socks = new SingleBasicProxySelectorImpl(httpProxy);
                                        socks.setProxyRotationEnabled(true);
                                        if (dupeCheck.add(socks)) {
                                            logger.info("Add Basic: " + httpProxy);
                                            proxies.add(socks);
                                            if (newDefaultProxy == null) {
                                                newDefaultProxy = socks;
                                                config.setNoneDefault(false);
                                            }
                                        }
                                        break;
                                    }

                                }
                            }
                        }

                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }

                /* convert from old system */
                final List<HTTPProxy> reto = this.restoreFromOldConfig();
                restore: for (final HTTPProxy proxyData : reto) {
                    try {
                        switch (proxyData.getType()) {
                        case DIRECT:
                            proxy = new SingleDirectGatewaySelector(proxyData);
                            break;
                        case HTTP:
                        case SOCKS4:
                        case SOCKS5:
                            proxy = new SingleBasicProxySelectorImpl(proxyData);
                            break;

                        default:
                            continue;
                        }
                        if (proxy == null) {
                            continue;
                        }
                        if (!dupeCheck.add(proxy)) {
                            continue restore;
                        }

                        if (proxy instanceof SingleBasicProxySelectorImpl) {
                            logger.info("Old Restore: " + JSonStorage.serializeToJson(proxyData));
                            proxies.add((SingleBasicProxySelectorImpl) proxy);
                        } else {
                            throw new WTFException("Unknown Type: " + proxy.getClass());
                        }

                        /* in old system we only had one possible proxy */
                        newDefaultProxy = proxy;
                        if (proxy.isProxyRotationEnabled()) {
                            rotCheck = true;
                        }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            }
            /* import proxies from system properties */
            final List<HTTPProxy> sproxy = HTTPProxy.getFromSystemProperties();
            restore: for (final HTTPProxy proxyData : sproxy) {
                try {
                    proxy = new SingleBasicProxySelectorImpl(proxyData);

                    if (!dupeCheck.add(proxy)) {
                        continue restore;
                    }
                    logger.info("Add System Proxy: " + JSonStorage.serializeToJson(proxyData));
                    proxies.add((SingleBasicProxySelectorImpl) proxy);

                    /* in old system we only had one possible proxy */
                    newDefaultProxy = proxy;
                    if (proxy.isProxyRotationEnabled()) {
                        rotCheck = true;
                    }
                } catch (final Throwable e) {
                    Log.exception(e);
                }
            }
        }
        {
            /* use own scope */
            final List<HTTPProxy> availableDirects = this.getAvailableDirects();
            final java.util.List<ProxyData> ret = this.config.getDirectGatewayList();
            if (ret != null) {
                // restore directs
                restore: for (final ProxyData proxyData : ret) {
                    /* check if the local IP is still avilable */
                    try {

                        proxy = new SingleDirectGatewaySelector(proxyData);
                        if (proxy == null) {
                            continue;
                        }

                        if (!dupeCheck.add(proxy)) {
                            continue restore;
                        }
                        dupeCheck.add(proxy);

                        boolean localIPAvailable = false;
                        for (final HTTPProxy p : availableDirects) {
                            if (p.equals(((SingleDirectGatewaySelector) proxy).getProxy())) {
                                localIPAvailable = true;
                                break;
                            }
                        }
                        if (localIPAvailable == false) {
                            /* local ip no longer available */
                            continue restore;
                        }
                        logger.info("Add Direct " + JSonStorage.serializeToJson(proxyData));
                        directs.add((SingleDirectGatewaySelector) proxy);

                        if (proxyData.isDefaultProxy()) {
                            newDefaultProxy = proxy;
                        }
                        if (proxy.isProxyRotationEnabled()) {
                            rotCheck = true;
                        }

                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            }
        }

        if (StringUtils.isNotEmpty(this.config.getLocalPacScript())) {
            boolean localEntry = false;
            for (final PacProxySelectorImpl p : pacs) {
                if (p.getPACUrl().startsWith("local://")) {
                    localEntry = true;
                    break;
                }
            }

            if (!localEntry) {
                final PacProxySelectorImpl pac = new PacProxySelectorImpl("local://AdvancedConfig.LocalePacScript", null, null);
                pac.setProxyRotationEnabled(true);
                logger.info("Add Local pac");
                pacs.add(pac);
            }

        }

        if (this.config.isNoneDefault()) {
            /* check if the NONE Proxy is our default proxy */
            if (newDefaultProxy != null) {
                Log.L.severe("NONE default but already got different default?!");
            }
            newDefaultProxy = this.none;
        }
        /* is NONE Proxy included in rotation */
        this.none.setProxyRotationEnabled(this.config.isNoneRotationEnabled());
        this.none.setFilter(this.config.getNoneFilter());

        if (this.none.isProxyRotationEnabled()) {
            rotCheck = true;
        }
        if (!rotCheck) {
            // we need at least one rotation
            this.none.setProxyRotationEnabled(true);
            this.config.setNoneRotationEnabled(true);
        }
        if (newDefaultProxy == null || newDefaultProxy == this.none) {
            this.config.setNoneDefault(true);
        }
        this.setDefaultProxy(newDefaultProxy);
        /* set new proxies live */
        this.directs = directs;
        this.proxies = proxies;
        this.pacs = pacs;
        this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(ProxyController.this, ProxyEvent.Types.REFRESH, null));
    }

    /** removes given proxy from proxylist */
    public void remove(final AbstractProxySelectorImpl proxy) {
        if (proxy == null) {
            return;
        }
        boolean removed = false;
        synchronized (this.LOCK) {
            if (proxy instanceof SingleBasicProxySelectorImpl) {

                final java.util.List<SingleBasicProxySelectorImpl> nproxies = new ArrayList<SingleBasicProxySelectorImpl>(this.proxies);
                if (nproxies.remove(proxy)) {
                    removed = true;
                    if (proxy == this.defaultproxy) {
                        this.setDefaultProxy(this.none);
                    }
                }
                this.proxies = nproxies;
            } else if (proxy instanceof PacProxySelectorImpl) {
                final java.util.List<PacProxySelectorImpl> npacs = new ArrayList<PacProxySelectorImpl>(this.pacs);
                if (npacs.remove(proxy)) {
                    removed = true;
                    if (proxy == this.defaultproxy) {
                        this.setDefaultProxy(this.none);
                    }
                }
                this.pacs = npacs;
            } else {
                throw new WTFException("bad Type: " + proxy.getClass());
            }

        }
        if (removed) {
            this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REMOVED, proxy));
        }
    }

    private List<HTTPProxy> restoreFromOldConfig() {
        final java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        final SubConfiguration oldConfig = SubConfiguration.getConfig("DOWNLOAD", true);
        if (oldConfig.getBooleanProperty(ProxyController.USE_PROXY, false)) {
            /* import old http proxy settings */
            final String host = oldConfig.getStringProperty(ProxyController.PROXY_HOST, "");
            final int port = oldConfig.getIntegerProperty(ProxyController.PROXY_PORT, 8080);
            final String user = oldConfig.getStringProperty(ProxyController.PROXY_USER, "");
            final String pass = oldConfig.getStringProperty(ProxyController.PROXY_PASS, "");
            if (!StringUtils.isEmpty(host)) {
                final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
                if (!StringUtils.isEmpty(user)) {
                    pr.setUser(user);
                }
                if (!StringUtils.isEmpty(pass)) {
                    pr.setPass(pass);
                }
                ret.add(pr);
            }
        }
        if (oldConfig.getBooleanProperty(ProxyController.USE_SOCKS, false)) {
            /* import old socks5 settings */
            final String user = oldConfig.getStringProperty(ProxyController.PROXY_USER_SOCKS, "");
            final String pass = oldConfig.getStringProperty(ProxyController.PROXY_PASS_SOCKS, "");
            final String host = oldConfig.getStringProperty(ProxyController.SOCKS_HOST, "");
            final int port = oldConfig.getIntegerProperty(ProxyController.SOCKS_PORT, 1080);
            if (!StringUtils.isEmpty(host)) {
                final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
                if (!StringUtils.isEmpty(user)) {
                    pr.setUser(user);
                }
                if (!StringUtils.isEmpty(pass)) {
                    pr.setPass(pass);
                }
                ret.add(pr);
            }
        }
        return ret;
    }

    private void saveProxySettings() {
        final AbstractProxySelectorImpl ldefaultproxy = this.defaultproxy;
        {
            /* use own scope */
            final ArrayList<ProxyData> ret = new ArrayList<ProxyData>(this.proxies.size());
            final List<SingleBasicProxySelectorImpl> lproxies = this.proxies;
            for (final AbstractProxySelectorImpl proxy : lproxies) {
                final ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }

            final List<PacProxySelectorImpl> lPacs = this.pacs;
            for (final PacProxySelectorImpl proxy : lPacs) {
                final ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            this.config.setCustomProxyList(ret);
        }
        {
            /* use own scope */
            final ArrayList<ProxyData> ret = new ArrayList<ProxyData>();
            final List<SingleDirectGatewaySelector> ldirects = this.directs;
            for (final AbstractProxySelectorImpl proxy : ldirects) {
                final ProxyData pd = proxy.toProxyData();
                pd.setDefaultProxy(proxy == ldefaultproxy);
                ret.add(pd);
            }
            this.config.setDirectGatewayList(ret);
        }
        this.config.setNoneDefault(this.none == ldefaultproxy);
        this.config.setNoneRotationEnabled(this.none.isProxyRotationEnabled());
        this.config.setNoneFilter(this.none.getFilter());
        this.config._getStorageHandler().write();
    }

    public void setBan(final AbstractProxySelectorImpl proxy, final HTTPProxy usedProxy, final String bannedDomain, final long proxyHostBanTimeout, final String explain) {
        proxy.addBan(new ProxyBan(usedProxy, bannedDomain, proxyHostBanTimeout <= 0 ? -1 : proxyHostBanTimeout + System.currentTimeMillis(), explain));
    }

    /**
     * sets current default proxy
     * 
     * @param def
     */
    public void setDefaultProxy(final AbstractProxySelectorImpl def) {
        if (def != null && this.defaultproxy == def) {
            return;
        }
        if (def == null) {
            this.defaultproxy = this.none;
        } else {
            this.defaultproxy = def;
        }
        this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REFRESH, null));
        // exportUpdaterProxy();
    }

    public void setProxyRotationEnabled(final AbstractProxySelectorImpl p, final boolean enabled) {
        if (p == null) {
            return;
        }

        if (p.isProxyRotationEnabled() == enabled) {
            return;
        }
        p.setProxyRotationEnabled(enabled);

        this.eventSender.fireEvent(new ProxyEvent<AbstractProxySelectorImpl>(this, ProxyEvent.Types.REFRESH, null));
    }

    public String toProxyID(final HTTPProxy usedProxy) {
        return usedProxy.getUser() + ":" + usedProxy.getPass() + "@" + usedProxy.getHost() + ":" + usedProxy.getPort() + "(Native: " + usedProxy.isPreferNativeImplementation() + ")";
    }

    public boolean updateProxy(final AbstractProxySelectorImpl selector, final Request request, final int retryCounter) {
        final List<String> proxyAuths = request.getHttpConnection().getHeaderFields("proxy-authenticate");
        final StaticProxy staticProxy = request.getProxy();
        if (staticProxy == null) {
            return false;
        }

        try {
            return updateProxy(selector, proxyAuths, staticProxy.getLocalClone(), staticProxy.getOrgReference(), new URL(request.getUrl()), retryCounter);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProxy(final AbstractProxySelectorImpl selector, List<String> proxyAuths, HTTPProxy usedProxy, HTTPProxy orgReference, final URL url, final int retryCounter) {
        if (retryCounter > 10) {
            return false;
        }

        if (!usedProxy.equals(orgReference)) {
            // proxy has been updated already
            return true;
        }

        try {

            // pd = new ProxyDialog(usedProxy, T.T.TranslationProxyDialogAuthRequired(url.getHost()));
            // pd.setTitle(_AWU.T.proxydialog_title());

            if (proxyAuths != null) {
                for (final String authMethod : proxyAuths) {
                    if ("NTLM".equalsIgnoreCase(authMethod)) {
                        if (usedProxy.isPreferNativeImplementation() == false) {
                            /* enable nativeImplementation for NTLM */
                            usedProxy.setPreferNativeImplementation(true);
                            return true;
                        }
                        break;
                    }
                }
            }
            synchronized (this) {

                if (!usedProxy.equals(orgReference)) {
                    // proxy has been updated already
                    return true;
                }

                this.askForProxyAuth(selector, UIOManager.LOGIC_COUNTDOWN, false, usedProxy, orgReference, url, _JDT._.ProxyController_updateProxy_proxy_auth_required_msg(url.getHost()), _JDT._.ProxyController_updateProxy_proxy_auth_required_title());

            }
            return false;
        } catch (final Throwable e) {
            this.logger.log(e);
        }
        return false;
    }

    public HTTPProxy updateProxyAuthForUpdater(int retries, final HTTPProxy updaterProxy, final List<String> proxyAuths, final URL url) {
        if (retries++ > 10)
            return null;
        if (updaterProxy == null) {
            return null;
        }
        if (updaterProxy != null && updaterProxy instanceof ProxyClone) {
            HTTPProxy orgRef = ((ProxyClone) updaterProxy).getOrgReference();

            AbstractProxySelectorImpl selector = null;
            if (orgRef instanceof ExtProxy) {
                selector = ((ExtProxy) orgRef).getFactory();
            }
            while (this.updateProxy(selector, proxyAuths, updaterProxy, orgRef, url, retries)) {

                if (!orgRef.equals(updaterProxy)) {
                    // there has been an update and the orgref changed. return the changed proxy
                    return new ProxyClone(orgRef);
                } else {
                    // no change. ask again
                    if (retries++ > 10)
                        return null;
                    continue;
                }
            }

        } else {
            // the proxy is either null, or not from this proxycontroller. it may be a proxy that has been created before JD classes could
            // launch, or a proxy that has been entered manually be the user.
            // do nothing in this case. the updater will use its own routines
        }
        return null;

    }

    public List<HTTPProxy> getProxiesByUrl(URL url) {

        ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        List<AbstractProxySelectorImpl> selectors = ProxyController.getInstance().getPossibleProxies(url.getHost(), false, Integer.MAX_VALUE);
        if (selectors != null) {
            for (AbstractProxySelectorImpl selector : selectors) {
                List<HTTPProxy> lst = selector.getProxiesByUrl(url.toString());
                if (lst != null) {
                    for (HTTPProxy p : lst) {
                        if (!selector.isBanned(p)) {
                            ret.add(p);
                        }
                    }
                }
            }
        }
        return ret;
    }

}
