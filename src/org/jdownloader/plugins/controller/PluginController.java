package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Map;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class PluginController<T extends Plugin> {

    @SuppressWarnings("unchecked")
    public java.util.List<PluginInfo<T>> scan(String hosterpath, Map<String, ArrayList<LazyPlugin>> pluginCache) {
        LogSource logger = LogController.getRebirthLogger();
        final boolean ownLogger;
        if (logger == null) {
            ownLogger = true;
            logger = LogController.CL();
            logger.setAllowTimeoutFlush(false);
        } else {
            ownLogger = false;
        }
        final java.util.List<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        try {
            final File path = Application.getRootByClass(jd.SecondLevelLaunch.class, hosterpath);
            final PluginClassLoaderChild cl = PluginClassLoader.getInstance().getChild();
            final File[] pluginClassFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    if (!name.endsWith(".class") || name.contains("$")) {
                        return false;
                    }
                    if (name.startsWith("YoutubeDashConfigPanel")) {
                        return false;
                    }
                    if (name.startsWith("RTMPDownload")) {
                        return false;
                    }
                    if (name.startsWith("YoutubeHelper")) {
                        return false;
                    }
                    return true;
                }
            });
            final String pkg = hosterpath.replace("/", ".");
            boolean errorFree = true;
            final ArrayList<PluginInfo<T>> scannedPlugins = new ArrayList<PluginInfo<T>>();
            if (pluginClassFiles != null) {
                for (final File pluginClassFile : pluginClassFiles) {
                    try {
                        final long lastModified = pluginClassFile.lastModified();
                        final String name = pluginClassFile.getName();
                        final String classFileName = name.substring(0, name.length() - 6);
                        String sha256 = null;
                        scannedPlugins.clear();
                        if (pluginCache != null) {
                            final ArrayList<LazyPlugin> cachedPlugins = pluginCache.get(name);
                            if (cachedPlugins != null) {
                                for (final LazyPlugin plugin : cachedPlugins) {
                                    if (plugin != null && ((plugin.getMainClassLastModified() > 0 && plugin.getMainClassLastModified() == lastModified) || ((sha256 != null || (sha256 = Hash.getSHA1(pluginClassFile)) != null) && sha256.equals(plugin.getMainClassSHA256())))) {
                                        final PluginInfo<T> retPlugin = new PluginInfo<T>(pluginClassFile, null);
                                        retPlugin.setLazyPlugin(plugin);
                                        scannedPlugins.add(retPlugin);
                                    } else {
                                        scannedPlugins.clear();
                                        break;
                                    }
                                }
                            }
                        }
                        if (scannedPlugins.size() == 0) {
                            final PluginInfo<T> pluginInfo = new PluginInfo<T>(pluginClassFile, (Class<T>) cl.loadClass(pkg + "." + classFileName));
                            ret.add(pluginInfo);
                            pluginInfo.setMainClassLastModified(lastModified);
                            if (sha256 == null) {
                                sha256 = Hash.getSHA256(pluginClassFile);
                            }
                            pluginInfo.setMainClassSHA256(sha256);
                            logger.finer("Loaded: " + classFileName);
                        } else {
                            for (final PluginInfo<T> scannedPlugin : scannedPlugins) {
                                logger.finer("Cached: " + classFileName + "|" + scannedPlugin.getLazyPlugin().getDisplayName() + "|" + scannedPlugin.getLazyPlugin().getVersion());
                            }
                            ret.addAll(scannedPlugins);
                        }
                    } catch (Throwable e) {
                        errorFree = false;
                        logger.log(e);
                    }
                }
            }
            if (errorFree && ownLogger) {
                logger.clear();
            }
        } finally {
            if (ownLogger) {
                logger.close();
            }
        }
        return ret;

    }
}
