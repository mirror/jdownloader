package org.jdownloader.plugins.controller.host;

import java.util.HashMap;

import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class PluginFinder {

    private final HashMap<String, String>        hostMappings = new HashMap<String, String>();
    private final HashMap<String, PluginForHost> pluginCaches = new HashMap<String, PluginForHost>();

    private final LogSource                      logger;

    public PluginFinder() {
        this(null);
    }

    public PluginFinder(LogSource logger) {
        if (logger == null) {
            this.logger = LogController.CL(true);
        } else {
            this.logger = logger;
        }
    }

    public synchronized String assignHost(String host) {
        if (host != null) {
            if (!hostMappings.containsKey(host)) {
                final LazyHostPlugin lazyPlugin = HostPluginController.getInstance().get(host);
                if (lazyPlugin != null) {
                    if (!lazyPlugin.isHasRewrite()) {
                        /* lazyPlugin has no customized rewriteHost */
                        hostMappings.put(host, lazyPlugin.getHost());
                        return lazyPlugin.getHost();
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
                                hostMappings.put(host, rewriteHost);
                                return rewriteHost;
                            } else if (StringUtils.isNotEmpty(rewriteHost)) {
                                /* different rewriteHost, we need to call assignHost(rewriteHost) to check for further changes */
                                return assignHost(rewriteHost);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                }
                for (final LazyHostPlugin lazyHostPlugin : HostPluginController.getInstance().list()) {
                    if (lazyHostPlugin.isHasRewrite()) {
                        try {
                            PluginForHost plugin = pluginCaches.get(lazyHostPlugin.getHost());
                            if (plugin == null) {
                                plugin = lazyHostPlugin.getPrototype(null);
                                pluginCaches.put(lazyHostPlugin.getHost(), plugin);
                            }
                            final String rewriteHost = plugin.rewriteHost(host);
                            if (StringUtils.equalsIgnoreCase(rewriteHost, host)) {
                                /* host equals rewriteHost */
                                hostMappings.put(host, rewriteHost);
                                return rewriteHost;
                            } else if (StringUtils.isNotEmpty(rewriteHost)) {
                                /* different rewriteHost, we need to call assignHost(rewriteHost) to check for further changes */
                                return assignHost(rewriteHost);
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

    public synchronized PluginForHost assignPlugin(final DownloadLink link, final boolean assignPlugin) {
        final String host = assignHost(link.getHost());
        if (host != null) {
            if (pluginCaches.containsKey(host)) {
                final PluginForHost pluginForHost = pluginCaches.get(host);
                if (pluginForHost != null) {
                    try {
                        if (!assignPlugin || pluginForHost.assignPlugin(link)) {
                            return pluginForHost;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            }
            try {
                final LazyHostPlugin lazyHostPlugin = HostPluginController.getInstance().get(host);
                if (lazyHostPlugin != null) {
                    final PluginForHost pluginForHost = lazyHostPlugin.getPrototype(null);
                    pluginCaches.put(host, pluginForHost);
                    try {
                        if (!assignPlugin || pluginForHost.assignPlugin(link)) {
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
        try {
            final LazyHostPlugin fallBackPlugin = HostPluginController.getInstance().getFallBackPlugin();
            if (fallBackPlugin != null) {
                logger.severe("Assign fallBackPlugin for: " + link.getHost() + ">" + host + "=" + link.getName());
                final PluginForHost pluginForHost = fallBackPlugin.getPrototype(null);
                pluginCaches.put(host, pluginForHost);
                try {
                    if (!assignPlugin || pluginForHost.assignPlugin(link)) {
                        return pluginForHost;
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        logger.severe("Could not assign any plugin for link: " + link.getHost() + ">" + host + "=" + link.getName());
        return null;
    }

    public synchronized PluginForHost assignPlugin(final Account acc, final boolean assignPlugin) {
        final String host = assignHost(acc.getHoster());
        if (host != null) {
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
                final LazyHostPlugin lazyHostPlugin = HostPluginController.getInstance().get(host);
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
        logger.severe("Could not assign any plugin for account: " + acc.getHoster() + ">" + host + "=" + acc.getUser());
        return null;
    }
}
