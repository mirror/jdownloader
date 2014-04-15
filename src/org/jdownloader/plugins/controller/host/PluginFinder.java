package org.jdownloader.plugins.controller.host;

import java.util.ArrayList;
import java.util.HashMap;

import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class PluginFinder {

    volatile HashMap<String, PluginForHost> pluginCache           = new HashMap<String, PluginForHost>();

    volatile HashMap<String, PluginForHost> rewriteLinkCache      = new HashMap<String, PluginForHost>();
    volatile ArrayList<PluginForHost>       rewriteLinkPlugins    = null;

    volatile HashMap<String, PluginForHost> rewriteAccountCache   = new HashMap<String, PluginForHost>();
    volatile ArrayList<PluginForHost>       rewriteAccountPlugins = null;

    public PluginForHost assignPlugin(DownloadLink link, boolean allowRewrite, LogSource logger) {
        PluginForHost pluginForHost = null;
        if (logger == null) logger = LogController.CL(true);
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
                LazyHostPlugin hPlugin = HostPluginController.getInstance().get(link.getHost());
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
                for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
                    if (!p.isHasLinkRewrite()) continue;
                    PluginForHost protoType = null;
                    try {
                        protoType = p.getPrototype(null);
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    if (protoType == null) {
                        /* could not initialize a prototype plugin */
                        continue;
                    }
                    try {
                        if (protoType.rewriteHost((DownloadLink) null) != null) {
                            rewriteLinkPlugins.add(protoType);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            }
            String originalHost = link.getHost();
            if (rewriteLinkCache.containsKey(originalHost)) {
                pluginForHost = rewriteLinkCache.get(originalHost);
            } else {
                /* rewrite cache available, let's check for a valid rewriting plugin */
                for (PluginForHost p : rewriteLinkPlugins) {
                    try {
                        if (p.rewriteHost(link)) {
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
                    if (pluginForHost.rewriteHost(link)) {
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

    public PluginForHost assignPlugin(Account acc, boolean allowRewrite, LogSource logger) {
        if (acc.getHoster() == null) return null;
        PluginForHost pluginForHost = null;
        if (logger == null) logger = LogController.CL(true);
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
                if (hPlugin != null && hPlugin.isPremium()) {
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
                for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
                    if (!p.isHasAccountRewrite()) continue;
                    PluginForHost protoType = null;
                    try {
                        protoType = p.getPrototype(null);
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    if (protoType == null) {
                        /* could not initialize a prototype plugin */
                        continue;
                    }
                    try {
                        if (protoType.rewriteHost((Account) null) != null) {
                            rewriteAccountPlugins.add(protoType);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
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
                        if (p.rewriteHost(acc)) {
                            pluginForHost = p;
                            break;
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                rewriteAccountCache.put(originalHost, pluginForHost);
                if (pluginForHost != null) {
                    logger.info("Plugin " + pluginForHost.getHost() + " now handles " + acc.getHoster());
                    return pluginForHost;
                }
            }
            if (pluginForHost != null) {
                try {
                    if (pluginForHost.rewriteHost(acc)) {
                        logger.info("Plugin " + pluginForHost.getHost() + " now handles " + acc.getHoster());
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
