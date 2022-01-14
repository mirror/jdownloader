package org.jdownloader.plugins.controller.host;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

public class PluginFinder {
    private final HashMap<String, LazyHostPlugin> hostMappings            = new HashMap<String, LazyHostPlugin>();
    private final HashMap<String, PluginForHost>  pluginCaches            = new HashMap<String, PluginForHost>();
    private final static Set<String>              BROKENPLUGINS           = new HashSet<String>();
    private final static AtomicLong               PLUGINSLASTMODIFICATION = new AtomicLong(-1);
    private final LogInterface                    logger;

    public PluginFinder() {
        this(null);
    }

    public PluginFinder(final LogInterface logger) {
        if (logger == null) {
            this.logger = LogController.CL(true);
        } else {
            this.logger = logger;
        }
        blacklistBrokenPlugins();
    }

    protected synchronized void blacklistBrokenPlugins() {
        synchronized (BROKENPLUGINS) {
            final long lastmodification = HostPluginController.getInstance().getLastModification();
            if (PLUGINSLASTMODIFICATION.get() != lastmodification) {
                try {
                    final String checkDomain = UniqueAlltimeID.create() + ".com";
                    while (true) {
                        final String assignHost = assignHost(checkDomain);
                        if (assignHost == null) {
                            break;
                        } else {
                            final PluginForHost plugin = pluginCaches.get(assignHost);
                            if (plugin != null) {
                                final LazyHostPlugin lazyP = plugin.getLazyP();
                                logger.severe("Please fix rewriteHost in HostPlugin:" + lazyP);
                                BROKENPLUGINS.add(lazyP.toString());
                            } else {
                                logger.severe("Please fix rewriteHost in HostPlugin:" + assignHost);
                            }
                        }
                    }
                } finally {
                    PLUGINSLASTMODIFICATION.set(lastmodification);
                }
            }
        }
    }

    public synchronized String assignHost(String host) {
        final LazyHostPlugin ret = _assignHost(host);
        if (ret != null) {
            return ret.getHost();
        } else {
            return null;
        }
    }

    public synchronized LazyHostPlugin _assignHost(final String host) {
        if (host != null) {
            if (!hostMappings.containsKey(host)) {
                final LazyHostPlugin lazyPlugin = HostPluginController.getInstance().get(host);
                if (lazyPlugin != null) {
                    if (!lazyPlugin.isHasRewrite()) {
                        /* lazyPlugin has no customized rewriteHost */
                        hostMappings.put(host, lazyPlugin);
                        return lazyPlugin;
                    } else {
                        try {
                            PluginForHost plugin = pluginCaches.get(lazyPlugin.getHost());
                            if (plugin == null) {
                                plugin = lazyPlugin.getPrototype(null);
                                pluginCaches.put(lazyPlugin.getHost(), plugin);
                            }
                            final String rewriteHost = plugin.rewriteHost(host);
                            if (StringUtils.equalsIgnoreCase(rewriteHost, host)) {
                                /* host equals rewriteHost */
                                hostMappings.put(host, lazyPlugin);
                                return lazyPlugin;
                            } else if (StringUtils.isNotEmpty(rewriteHost)) {
                                /* different rewriteHost, we need to call assignHost(rewriteHost) to check for further changes */
                                return _assignHost(rewriteHost);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                }
                for (final LazyHostPlugin lazyHostPlugin : HostPluginController.getInstance().list()) {
                    if (lazyHostPlugin.isHasRewrite()) {
                        synchronized (BROKENPLUGINS) {
                            if (BROKENPLUGINS.contains(lazyHostPlugin.toString())) {
                                continue;
                            }
                        }
                        try {
                            PluginForHost plugin = pluginCaches.get(lazyHostPlugin.getHost());
                            if (plugin == null) {
                                plugin = lazyHostPlugin.getPrototype(null);
                                pluginCaches.put(lazyHostPlugin.getHost(), plugin);
                            }
                            final String rewriteHost = plugin.rewriteHost(host);
                            if (StringUtils.equalsIgnoreCase(rewriteHost, host)) {
                                /* host equals rewriteHost */
                                hostMappings.put(host, lazyHostPlugin);
                                return lazyHostPlugin;
                            } else if (StringUtils.isNotEmpty(rewriteHost)) {
                                /* different rewriteHost, we need to call assignHost(rewriteHost) to check for further changes */
                                return _assignHost(rewriteHost);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                }
                hostMappings.put(host, null);
                logger.severe("Could not assign any host for: " + host);
                return null;
            }
            return hostMappings.get(host);
        }
        return null;
    }

    private boolean assign(final DownloadLink link, final PluginForHost pluginForHost) {
        final LogInterface previousLogger = LogController.setRebirthLogger(logger);
        try {
            if (pluginForHost.assignPlugin(link)) {
                try {
                    pluginForHost.onPluginAssigned(link);
                } catch (final Throwable e) {
                    logger.log(e);
                }
                return true;
            } else {
                return false;
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            LogController.setRebirthLogger(previousLogger);
        }
    }

    public synchronized PluginForHost assignPlugin(final DownloadLink link, final boolean assignPlugin) {
        final LazyHostPlugin lazyHostPlugin = _assignHost(link.getHost());
        if (lazyHostPlugin != null) {
            final String host = lazyHostPlugin.getHost();
            if (pluginCaches.containsKey(host)) {
                final PluginForHost pluginForHost = pluginCaches.get(host);
                if (pluginForHost != null) {
                    if (!assignPlugin || assign(link, pluginForHost)) {
                        return pluginForHost;
                    }
                }
            }
            try {
                final PluginForHost pluginForHost = lazyHostPlugin.getPrototype(null);
                pluginCaches.put(lazyHostPlugin.getHost(), pluginForHost);
                if (!assignPlugin || assign(link, pluginForHost)) {
                    return pluginForHost;
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        final String host = link.getHost();
        try {
            final LazyHostPlugin fallBackPlugin = HostPluginController.getInstance().getFallBackPlugin();
            if (fallBackPlugin != null) {
                logger.severe("Assign fallBackPlugin for: " + link.getHost() + ">" + host + "=" + link.getName());
                final PluginForHost pluginForHost = fallBackPlugin.getPrototype(null);
                pluginCaches.put(host, pluginForHost);
                if (!assignPlugin || assign(link, pluginForHost)) {
                    return pluginForHost;
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        logger.severe("Could not assign any plugin for link: " + link.getHost() + ">" + host + "=" + link.getName());
        return null;
    }

    public synchronized PluginForHost assignPlugin(final Account acc, final boolean assignPlugin) {
        final LazyHostPlugin lazyHostPlugin = _assignHost(acc.getHoster());
        if (lazyHostPlugin != null) {
            final String host = lazyHostPlugin.getHost();
            if (pluginCaches.containsKey(host)) {
                final PluginForHost pluginForHost = pluginCaches.get(host);
                if (pluginForHost != null && (pluginForHost.isPremiumEnabled() || pluginForHost.getLazyP().getClassName().endsWith("r.Offline"))) {
                    try {
                        if (!assignPlugin || pluginForHost.assignPlugin(acc)) {
                            return pluginForHost;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            }
            try {
                if (lazyHostPlugin != null && (lazyHostPlugin.isPremium() || lazyHostPlugin.getClassName().endsWith("r.Offline"))) {
                    final PluginForHost pluginForHost = lazyHostPlugin.getPrototype(null);
                    pluginCaches.put(host, pluginForHost);
                    try {
                        if (!assignPlugin || pluginForHost.assignPlugin(acc)) {
                            return pluginForHost;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        logger.severe("Could not assign any plugin for account: " + acc.getHoster() + "|" + acc.getUser());
        return null;
    }
}
