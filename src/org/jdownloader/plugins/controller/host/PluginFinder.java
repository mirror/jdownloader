package org.jdownloader.plugins.controller.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class PluginFinder {
    private final HashMap<String, PluginForHost> pluginCaches            = new HashMap<String, PluginForHost>();
    private Map<String, Object>                  hostMappings            = new HashMap<String, Object>();

    private static volatile List<LazyHostPlugin> ASSIGN_PLUGINS          = new ArrayList<LazyHostPlugin>();
    private static volatile Map<String, Object>  HOST_MAPPINGS           = new ConcurrentHashMap<String, Object>();

    private final static Set<String>             BROKEN_PLUGINS          = new HashSet<String>();
    private final static AtomicLong              PLUGINSLASTMODIFICATION = new AtomicLong(-1);
    private final LogInterface                   logger;
    private final static Object                  NULL                    = new Object();

    public PluginFinder() {
        this(null);
    }

    public PluginFinder(final LogInterface logger) {
        if (logger == null) {
            this.logger = LogController.CL(true);
        } else {
            this.logger = logger;
        }
        updateCache();
    }

    public LogInterface getLogger() {
        return logger;
    }

    public List<LazyHostPlugin> listAssignPlugins() {
        return ASSIGN_PLUGINS;
    }

    protected synchronized void updateAssignPluginsCache() {
        final List<LazyHostPlugin> ret = new ArrayList<LazyHostPlugin>();
        for (final LazyHostPlugin lazyHostPlugin : HostPluginController.getInstance().list()) {
            if (lazyHostPlugin.hasFeature(FEATURE.ASSIGN_PLUGIN)) {
                ret.add(lazyHostPlugin);
            }
        }
        ASSIGN_PLUGINS = ret;
    }

    protected synchronized void blacklistBrokenPlugins() {
        synchronized (BROKEN_PLUGINS) {
            BROKEN_PLUGINS.clear();
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
                        BROKEN_PLUGINS.add(lazyP.toString());
                    } else {
                        logger.severe("Please fix rewriteHost in HostPlugin:" + assignHost);
                    }
                }
            }
        }
    }

    protected void updateCache() {
        synchronized (PLUGINSLASTMODIFICATION) {
            final long lastmodification = HostPluginController.getInstance().getLastModification();
            if (PLUGINSLASTMODIFICATION.get() != lastmodification) {
                try {
                    hostMappings = HOST_MAPPINGS = new ConcurrentHashMap<String, Object>();
                    updateAssignPluginsCache();
                    blacklistBrokenPlugins();
                } finally {
                    PLUGINSLASTMODIFICATION.set(lastmodification);
                }
            }
            hostMappings = HOST_MAPPINGS;
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

    public synchronized PluginForHost getPlugin(LazyHostPlugin lazyPlugin) throws UpdateRequiredClassNotFoundException {
        PluginForHost plugin = pluginCaches.get(lazyPlugin.getHost());
        if (plugin == null) {
            plugin = lazyPlugin.getPrototype(null);
            pluginCaches.put(lazyPlugin.getHost(), plugin);
        }
        return plugin;
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
                            final PluginForHost plugin = getPlugin(lazyPlugin);
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
                        synchronized (BROKEN_PLUGINS) {
                            if (BROKEN_PLUGINS.contains(lazyHostPlugin.toString())) {
                                continue;
                            }
                        }
                        try {
                            final PluginForHost plugin = getPlugin(lazyHostPlugin);
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
                hostMappings.put(host, NULL);
                logger.severe("Could not assign any host for: " + host);
                return null;
            }
            final Object ret = hostMappings.get(host);
            if (ret instanceof LazyHostPlugin) {
                return (LazyHostPlugin) ret;
            }
        }
        return null;
    }

    private PluginForHost assign(final DownloadLink link, final PluginForHost pluginForHost) {
        final LogInterface previousLogger = LogController.setRebirthLogger(logger);
        try {
            final PluginForHost assigned = pluginForHost.assignPlugin(this, link);
            if (assigned != null) {
                try {
                    assigned.onPluginAssigned(link);
                } catch (final Throwable e) {
                    logger.log(e);
                }
                return assigned;
            } else {
                return null;
            }
        } catch (final Throwable e) {
            logger.log(e);
            return null;
        } finally {
            LogController.setRebirthLogger(previousLogger);
        }
    }

    public synchronized PluginForHost assignPlugin(final DownloadLink link, final boolean assignPlugin) {
        final LazyHostPlugin lazyHostPlugin = _assignHost(link.getHost());
        if (lazyHostPlugin != null) {
            final String host = lazyHostPlugin.getHost();
            if (pluginCaches.containsKey(host)) {
                PluginForHost pluginForHost = pluginCaches.get(host);
                if (pluginForHost != null) {
                    if (!assignPlugin || (pluginForHost = assign(link, pluginForHost)) != null) {
                        return pluginForHost;
                    }
                }
            }
            try {
                PluginForHost pluginForHost = getPlugin(lazyHostPlugin);
                if (!assignPlugin || (pluginForHost = assign(link, pluginForHost)) != null) {
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
                PluginForHost pluginForHost = getPlugin(fallBackPlugin);
                if (!assignPlugin || (pluginForHost = assign(link, pluginForHost)) != null) {
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
                if (pluginForHost != null && (pluginForHost.isPremiumEnabled() || pluginForHost.getLazyP().isOfflinePlugin())) {
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
                if (lazyHostPlugin != null && (lazyHostPlugin.isPremium() || lazyHostPlugin.isOfflinePlugin())) {
                    final PluginForHost pluginForHost = getPlugin(lazyHostPlugin);
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
