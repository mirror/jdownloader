package org.jdownloader.plugins.controller.host;

import java.util.ArrayList;
import java.util.HashMap;

import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class PluginFinder {

    private final HashMap<String, PluginForHost> pluginCache           = new HashMap<String, PluginForHost>();

    private final HashMap<String, PluginForHost> rewriteLinkCache      = new HashMap<String, PluginForHost>();
    private volatile ArrayList<PluginForHost>    rewriteLinkPlugins    = null;

    private final HashMap<String, PluginForHost> rewriteAccountCache   = new HashMap<String, PluginForHost>();
    private volatile ArrayList<PluginForHost>    rewriteAccountPlugins = null;

    private final HashMap<String, String>        rewriteHostCache      = new HashMap<String, String>();
    private volatile ArrayList<PluginForHost>    rewriteHostPlugins    = null;

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
        if (!rewriteHostCache.containsKey(host)) {
            if (rewriteHostPlugins == null) {
                /* rewrite cache not initialized yet, let's create it */
                rewriteHostPlugins = new ArrayList<PluginForHost>();
                for (LazyHostPlugin lazyPlugin : HostPluginController.getInstance().list()) {
                    if (lazyPlugin.isHasRewrite()) {
                        try {
                            final PluginForHost protoType = lazyPlugin.getPrototype(null);
                            if (!StringUtils.equals(protoType.rewriteHost((String) null), protoType.getHost())) {
                                rewriteHostPlugins.add(protoType);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                }
            }
            for (PluginForHost plugin : rewriteHostPlugins) {
                try {
                    final String assignHost = plugin.rewriteHost(host);
                    if (StringUtils.isNotEmpty(assignHost)) {
                        rewriteHostCache.put(host, assignHost);
                        return assignHost;
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
            final LazyHostPlugin lazyPlugin = HostPluginController.getInstance().get(host);
            if (lazyPlugin != null) {
                rewriteHostCache.put(host, lazyPlugin.getHost());
                return lazyPlugin.getHost();
            }
            rewriteHostCache.put(host, null);
            return null;
        }
        return rewriteHostCache.get(host);
    }

    public synchronized PluginForHost assignPlugin(DownloadLink link, boolean allowRewrite) {
        PluginForHost pluginForHost = null;
        /* check if we already have a cached plugin for given host */
        if (pluginCache.containsKey(link.getHost())) {
            pluginForHost = pluginCache.get(link.getHost());
            if (pluginForHost != null) {
                /* plugin in cache found */
                link.setDefaultPlugin(pluginForHost);
                return pluginForHost;
            }
        } else {
            /* no cached plugin found, first lets try to find a valid plugin for given host */
            try {
                final LazyHostPlugin hPlugin = HostPluginController.getInstance().get(link.getHost());
                if (hPlugin != null) {
                    pluginForHost = hPlugin.getPrototype(null);
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            pluginCache.put(link.getHost(), pluginForHost);
            if (pluginForHost != null) {
                /* plugin found, let's put it into cache */
                link.setDefaultPlugin(pluginForHost);
                return pluginForHost;
            }
        }
        if (allowRewrite && pluginForHost == null) {
            if (rewriteLinkPlugins == null) {
                /* rewrite cache not initialized yet, let's create it */
                rewriteLinkPlugins = new ArrayList<PluginForHost>();
                for (LazyHostPlugin lazyPlugin : HostPluginController.getInstance().list()) {
                    if (lazyPlugin.isHasLinkRewrite()) {
                        try {
                            final PluginForHost protoType = lazyPlugin.getPrototype(null);
                            if (protoType.rewriteHost((DownloadLink) null) != null) {
                                rewriteLinkPlugins.add(protoType);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                }
            }
            final String originalHost = link.getHost();
            if (rewriteLinkCache.containsKey(originalHost)) {
                pluginForHost = rewriteLinkCache.get(originalHost);
            } else {
                /* rewrite cache available, let's check for a valid rewriting plugin */
                for (PluginForHost p : rewriteLinkPlugins) {
                    try {
                        if (Boolean.TRUE.equals(p.rewriteHost(link))) {
                            pluginForHost = p;
                            break;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                rewriteLinkCache.put(originalHost, pluginForHost);
                if (pluginForHost != null) {
                    logger.info("Plugin " + pluginForHost.getHost() + " now handles " + link.getView().getDisplayName());
                    link.setDefaultPlugin(pluginForHost);
                    return pluginForHost;
                }
            }
            if (pluginForHost != null) {
                try {
                    if (Boolean.TRUE.equals(pluginForHost.rewriteHost(link))) {
                        logger.info("Plugin " + pluginForHost.getHost() + " now handles " + link.getView().getDisplayName());
                        link.setDefaultPlugin(pluginForHost);
                        return pluginForHost;
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }
        try {
            /* assign UpdateRequiredPlugin */
            LazyHostPlugin fallBackPlugin = HostPluginController.getInstance().getFallBackPlugin();
            if (fallBackPlugin != null) {
                pluginForHost = fallBackPlugin.getPrototype(null);
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        if (pluginForHost != null) {
            logger.severe("Use fallBackPlugin for: " + link.getHost() + "|" + link.getView().getDisplayName());
            pluginCache.put(link.getHost(), pluginForHost);
            if (pluginForHost != null) {
                /* plugin found, let's put it into cache */
                link.setDefaultPlugin(pluginForHost);
                return pluginForHost;
            }
        }
        logger.severe("Could not find plugin: " + link.getHost() + " for " + link.getView().getDisplayName());
        return null;
    }

    public synchronized PluginForHost assignPlugin(Account acc, boolean allowRewrite) {
        if (acc.getHoster() == null) {
            return null;
        }
        PluginForHost pluginForHost = null;
        /* check if we already have a cached plugin for given host */
        if (pluginCache.containsKey(acc.getHoster())) {
            pluginForHost = pluginCache.get(acc.getHoster());
            if (pluginForHost != null) {
                /* plugin in cache found */
                return pluginForHost;
            }
        } else {
            /* no cached plugin found, first lets try to find a valid plugin for given host */
            try {
                LazyHostPlugin hPlugin = HostPluginController.getInstance().get(acc.getHoster());
                if (hPlugin != null && (hPlugin.isPremium() || hPlugin.getClassName().endsWith("r.Offline"))) {
                    pluginForHost = hPlugin.getPrototype(null);
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            pluginCache.put(acc.getHoster(), pluginForHost);
            if (pluginForHost != null) {
                /* plugin found, let's put it into cache */
                return pluginForHost;
            }
        }
        if (allowRewrite && pluginForHost == null) {
            if (rewriteAccountPlugins == null) {
                /* rewrite cache not initialized yet, let's create it */
                rewriteAccountPlugins = new ArrayList<PluginForHost>();
                for (LazyHostPlugin lazyPlugin : HostPluginController.getInstance().list()) {
                    if (lazyPlugin.isHasAccountRewrite()) {
                        try {
                            final PluginForHost protoType = lazyPlugin.getPrototype(null);
                            if (protoType.rewriteHost((Account) null) != null) {
                                rewriteAccountPlugins.add(protoType);
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                }
            }
            String originalHost = acc.getHoster();
            if (rewriteAccountCache.containsKey(originalHost)) {
                pluginForHost = rewriteAccountCache.get(originalHost);
            } else {
                /* rewrite cache available, let's check for a valid rewriting plugin */
                for (PluginForHost p : rewriteAccountPlugins) {
                    try {
                        if (Boolean.TRUE.equals(p.rewriteHost(acc))) {
                            pluginForHost = p;
                            break;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                rewriteAccountCache.put(originalHost, pluginForHost);
                if (pluginForHost != null) {
                    logger.info("Plugin " + pluginForHost.getHost() + " has been renamed, now 'known as/handled by' " + acc.getHoster());
                    return pluginForHost;
                }
            }
            if (pluginForHost != null) {
                try {
                    if (Boolean.TRUE.equals(pluginForHost.rewriteHost(acc))) {
                        logger.info("Plugin " + pluginForHost.getHost() + " has been renamed, now 'known as/handled by' " + acc.getHoster());
                        return pluginForHost;
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }
        logger.severe("Could not find plugin: " + acc.getHoster());
        return null;
    }
}
