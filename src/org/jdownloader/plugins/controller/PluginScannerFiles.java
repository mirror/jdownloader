package org.jdownloader.plugins.controller;

import java.io.File;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class PluginScannerFiles<T extends Plugin> {
    private final PluginController<T> pluginController;

    protected PluginController<T> getPluginController() {
        return pluginController;
    }

    public PluginScannerFiles(PluginController<T> pluginController) {
        this.pluginController = pluginController;
    }

    protected List<PluginInfo<T>> scan(LogSource logger, String hosterpath, final List<? extends LazyPlugin> pluginCache, final AtomicLong lastFolderModification) throws Exception {
        final ArrayList<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        final long timeStamp = System.currentTimeMillis();
        try {
            long lastModified = lastFolderModification != null ? lastFolderModification.get() : -1;
            final File folder = Application.getRootByClass(jd.SecondLevelLaunch.class, hosterpath);
            final long lastFolderModified = folder.lastModified();
            if (lastModified > 0 && lastFolderModified == lastModified && pluginCache != null && pluginCache.size() > 0) {
                for (final LazyPlugin lazyPlugin : pluginCache) {
                    final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPlugin.getLazyPluginClass(), null);
                    pluginInfo.setLazyPlugin(lazyPlugin);
                    ret.add(pluginInfo);
                }
                return ret;
            }
            PluginClassLoaderChild cl = null;
            final String pkg = hosterpath.replace("/", ".");
            MessageDigest md = null;
            final byte[] mdCache = new byte[32767];
            final HashMap<String, List<LazyPlugin>> lazyPluginClassMap;
            if (pluginCache != null && pluginCache.size() > 0) {
                lazyPluginClassMap = new HashMap<String, List<LazyPlugin>>();
                for (final LazyPlugin lazyPlugin : pluginCache) {
                    List<LazyPlugin> list = lazyPluginClassMap.get(lazyPlugin.getLazyPluginClass().getClassName());
                    if (list == null) {
                        list = new ArrayList<LazyPlugin>();
                        lazyPluginClassMap.put(lazyPlugin.getLazyPluginClass().getClassName(), list);
                    }
                    list.add(lazyPlugin);
                }
            } else {
                lazyPluginClassMap = null;
            }
            if (lastFolderModification != null) {
                lastFolderModification.set(lastFolderModified);
            }
            final File stream[] = folder.listFiles();
            for (final File path : stream) {
                final String pathFileName = path.getName();
                if (pathFileName.endsWith(".class")) {
                    final String className = pathFileName.substring(0, pathFileName.length() - 6);
                    if (className.indexOf("$") < 0 && !PluginController.IGNORELIST.contains(className)) {
                        byte[] sha256 = null;
                        lastModified = path.lastModified();
                        if (lazyPluginClassMap != null) {
                            final List<LazyPlugin> lazyPlugins = lazyPluginClassMap.get(className);
                            final LazyPluginClass lazyPluginClass = lazyPlugins.get(0).getLazyPluginClass();
                            if (lazyPluginClass != null && (lazyPluginClass.getLastModified() == lastModified || ((md = MessageDigest.getInstance("SHA-256")) != null && (sha256 = PluginController.getFileHashBytes(path, md, mdCache)) != null && Arrays.equals(sha256, lazyPluginClass.getSha256())))) {
                                for (final LazyPlugin lazyPlugin : lazyPlugins) {
                                    // logger.finer("Cached: " + className + "|" + lazyPlugin.getDisplayName() + "|" +
                                    // lazyPluginClass.getRevision());
                                    final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, null);
                                    pluginInfo.setLazyPlugin(lazyPlugin);
                                    ret.add(pluginInfo);
                                }
                                continue;
                            }
                        }
                        Class<T> pluginClass = null;
                        long[] infos = null;
                        try {
                            if (cl == null) {
                                cl = PluginClassLoader.getInstance().getChild();
                            }
                            if (md == null) {
                                md = MessageDigest.getInstance("SHA-256");
                            }
                            pluginClass = (Class<T>) cl.loadClass(pkg + "." + className);
                            if (!Modifier.isAbstract(pluginClass.getModifiers()) && Plugin.class.isAssignableFrom(pluginClass)) {
                                infos = getPluginController().getInfos(pluginClass);
                                if (infos == null) {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        } catch (final Throwable e) {
                            logger.finer("Failed: " + className);
                            logger.log(e);
                            continue;
                        }
                        if (sha256 == null) {
                            sha256 = getPluginController().getFileHashBytes(path, md, mdCache);
                        }
                        final LazyPluginClass lazyPluginClass = new LazyPluginClass(className, sha256, lastModified, (int) infos[0], infos[1]);
                        final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, pluginClass);
                        // logger.finer("Scaned: " + className + "|" + lazyPluginClass.getRevision());
                        ret.add(pluginInfo);
                    }
                }
            }
            return ret;
        } finally {
            logger.info("@PluginController(Files): scan took " + (System.currentTimeMillis() - timeStamp) + "ms for " + ret.size());
        }
    }
}
