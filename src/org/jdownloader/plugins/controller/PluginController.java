package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class PluginController<T extends Plugin> {

    @SuppressWarnings("unchecked")
    public java.util.List<PluginInfo<T>> scan(String hosterpath, HashMap<String, ArrayList<LazyPlugin>> pluginCache) {
        boolean ownLogger = false;
        LogSource logger = LogController.getRebirthLogger();
        if (logger == null) {
            ownLogger = true;
            logger = LogController.CL();
            logger.setAllowTimeoutFlush(false);
        }
        final java.util.List<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        ClassLoader oldCL = null;
        try {
            File path = null;
            PluginClassLoaderChild cl = null;
            path = Application.getRootByClass(jd.SecondLevelLaunch.class, hosterpath);
            cl = PluginClassLoader.getInstance().getChild();
            oldCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            final File[] files = path.listFiles(new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(".class") && !name.contains("$");
                }
            });
            final String pkg = hosterpath.replace("/", ".");
            boolean errorFree = true;
            if (files != null) {
                for (final File f : files) {
                    try {
                        long lastModified = f.lastModified();
                        boolean validCachedPlugins = false;
                        String sha256 = null;
                        if (pluginCache != null) {
                            ArrayList<LazyPlugin> cachedPlugins = pluginCache.get(f.getName());
                            if (cachedPlugins != null) {
                                for (LazyPlugin plugin : cachedPlugins) {
                                    if ((plugin.getMainClassLastModified() > 0 && plugin.getMainClassLastModified() == lastModified) || ((sha256 != null || (sha256 = Hash.getSHA1(f)) != null) && sha256.equals(plugin.getMainClassSHA256()))) {
                                        PluginInfo<T> retPlugin = new PluginInfo<T>(f, null);
                                        retPlugin.setLazyPlugin(plugin);
                                        ret.add(retPlugin);
                                        validCachedPlugins = true;
                                    } else {
                                        int wtf = 1;
                                    }
                                }
                            }
                        }
                        if (validCachedPlugins) continue;
                        String classFileName = f.getName().substring(0, f.getName().length() - 6);
                        PluginInfo<T> pluginInfo;
                        ret.add(pluginInfo = new PluginInfo<T>(f, (Class<T>) cl.loadClass(pkg + "." + classFileName)));
                        pluginInfo.setMainClassLastModified(lastModified);
                        if (sha256 == null) sha256 = Hash.getSHA256(f);
                        pluginInfo.setMainClassSHA256(sha256);
                        logger.finer("Loaded from: " + new String("" + cl.getResource(hosterpath + "/" + f.getName())));
                    } catch (Throwable e) {
                        errorFree = false;
                        logger.log(e);
                    }
                }
            }
            if (errorFree && ownLogger) logger.clear();
        } finally {
            if (ownLogger) logger.close();
            Thread.currentThread().setContextClassLoader(oldCL);
        }
        return ret;

    }
}
