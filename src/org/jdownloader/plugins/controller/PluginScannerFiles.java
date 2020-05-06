package org.jdownloader.plugins.controller;

import java.io.File;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.PluginController.CHECK_RESULT;
import org.jdownloader.plugins.controller.PluginController.PluginClassInfo;

public class PluginScannerFiles<T extends Plugin> {
    private final static boolean      isJava16orOlder = Application.getJavaVersion() <= Application.JAVA16;
    private final PluginController<T> pluginController;

    protected PluginController<T> getPluginController() {
        return pluginController;
    }

    public PluginScannerFiles(PluginController<T> pluginController) {
        this.pluginController = pluginController;
    }

    protected List<PluginInfo<T>> scan(LogSource logger, String hosterpath, final List<? extends LazyPlugin<T>> pluginCache, final AtomicLong lastFolderModification) throws Exception, OutOfMemoryError {
        final ArrayList<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        final long timeStamp = System.currentTimeMillis();
        final Map<Object, List<String>> dependenciesCache = new HashMap<Object, List<String>>();
        try {
            final long lastFolderModifiedCheck = lastFolderModification != null ? lastFolderModification.get() : -1;
            final File folder = Application.getRootByClass(jd.SecondLevelLaunch.class, hosterpath);
            final long lastFolderModifiedScanStart = folder.lastModified();
            if (lastFolderModifiedCheck > 0 && lastFolderModifiedScanStart == lastFolderModifiedCheck && pluginCache != null && pluginCache.size() > 0) {
                for (final LazyPlugin<T> lazyPlugin : pluginCache) {
                    final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPlugin.getLazyPluginClass(), lazyPlugin);
                    ret.add(pluginInfo);
                }
                return ret;
            }
            final String pkg = hosterpath.replace("/", ".");
            final HashMap<String, List<LazyPlugin<T>>> lazyPluginClassMap;
            if (pluginCache != null && pluginCache.size() > 0) {
                lazyPluginClassMap = new HashMap<String, List<LazyPlugin<T>>>();
                for (final LazyPlugin<T> lazyPlugin : pluginCache) {
                    List<LazyPlugin<T>> list = lazyPluginClassMap.get(lazyPlugin.getLazyPluginClass().getClassName());
                    if (list == null) {
                        list = new ArrayList<LazyPlugin<T>>();
                        lazyPluginClassMap.put(lazyPlugin.getLazyPluginClass().getClassName(), list);
                    }
                    list.add(lazyPlugin);
                }
            } else {
                lazyPluginClassMap = null;
            }
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] mdCache = new byte[32767];
            PluginClassLoaderChild cl = null;
            final File stream[] = folder.listFiles();
            for (final File path : stream) {
                try {
                    final String pathFileName = path.getName();
                    if (pathFileName.endsWith(".class")) {
                        final String className = pathFileName.substring(0, pathFileName.length() - 6);
                        if (className.indexOf("$") < 0 && !PluginController.IGNORELIST.contains(className)) {
                            if (cl == null || isJava16orOlder) {
                                cl = PluginClassLoader.getInstance().getChild();
                                cl.setMapStaticFields(false);
                            }
                            final long lastFileModification = path.lastModified();
                            if (lazyPluginClassMap != null) {
                                final List<LazyPlugin<T>> lazyPlugins = lazyPluginClassMap.get(className);
                                if (lazyPlugins != null && lazyPlugins.size() > 0) {
                                    final LazyPluginClass lazyPluginClass = lazyPlugins.get(0).getLazyPluginClass();
                                    try {
                                        if (lazyPluginClass != null && CHECK_RESULT.isSuccessFul(getPluginController().checkForChanges(dependenciesCache, cl, lazyPluginClass, lastFileModification))) {
                                            for (final LazyPlugin<T> lazyPlugin : lazyPlugins) {
                                                // logger.finer("Cached: " + className + "|" + lazyPlugin.getDisplayName() + "|" +
                                                // lazyPluginClass.getRevision());
                                                final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, lazyPlugin);
                                                ret.add(pluginInfo);
                                            }
                                            continue;
                                        }
                                    } catch (final Throwable e) {
                                        logger.finer("Failed: " + className);
                                        logger.log(e);
                                    }
                                }
                            }
                            PluginClassInfo<T> pluginClassInfo = null;
                            try {
                                final Class<T> pluginClass = (Class<T>) cl.loadClass(pkg + "." + className);
                                if (!Modifier.isAbstract(pluginClass.getModifiers()) && Plugin.class.isAssignableFrom(pluginClass)) {
                                    pluginClassInfo = getPluginController().getPluginClassInfo(dependenciesCache, pluginClass);
                                    if (pluginClassInfo == null) {
                                        continue;
                                    } else {
                                        pluginClassInfo.sha256 = PluginController.getFileHashBytes(path, md, mdCache);
                                    }
                                } else {
                                    continue;
                                }
                            } catch (final OutOfMemoryError e) {
                                logger.finer("Failed: " + className);
                                logger.log(e);
                                throw e;
                            } catch (final Throwable e) {
                                logger.finer("Failed: " + className);
                                logger.log(e);
                                continue;
                            }
                            final LazyPluginClass lazyPluginClass = new LazyPluginClass(className, pluginClassInfo.sha256, lastFileModification, pluginClassInfo.interfaceVersion, pluginClassInfo.revision, pluginClassInfo.dependencies);
                            final PluginInfo<T> pluginInfo = new PluginInfo<T>(lazyPluginClass, pluginClassInfo.clazz);
                            // logger.finer("Scaned: " + className + "|" + lazyPluginClass.getRevision());
                            ret.add(pluginInfo);
                        }
                    }
                } catch (final OutOfMemoryError e) {
                    logger.finer("Failed: " + path);
                    logger.log(e);
                    throw e;
                } catch (Throwable e) {
                    logger.finer("Failed: " + path);
                    logger.log(e);
                }
            }
            final long lastFolderModifiedScanStop = folder.lastModified();
            if (lastFolderModifiedScanStart != lastFolderModifiedScanStop) {
                logger.info("@PluginController(Files): folder modification during scan detected!");
                Thread.sleep(1000);
                return scan(logger, hosterpath, pluginCache, lastFolderModification);
            } else {
                if (lastFolderModification != null) {
                    lastFolderModification.set(lastFolderModifiedScanStop);
                }
                return ret;
            }
        } finally {
            logger.info("@PluginController(Files): scan took " + (System.currentTimeMillis() - timeStamp) + "ms for " + ret.size());
        }
    }
}
