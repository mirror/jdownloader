package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class PluginController<T extends Plugin> {

    @SuppressWarnings("unchecked")
    public java.util.List<PluginInfo<T>> scan(String hosterpath) {
        boolean ownLogger = false;
        LogSource logger = LogController.getRebirthLogger();
        if (logger == null) {
            ownLogger = true;
            logger = LogController.CL();
            logger.setAllowTimeoutFlush(false);
        }
        final java.util.List<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        try {
            File path = null;
            PluginClassLoaderChild cl = null;

            path = Application.getRootByClass(jd.Launcher.class, hosterpath);

            cl = PluginClassLoader.getInstance().getChild();

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
                        String classFileName = f.getName().substring(0, f.getName().length() - 6);
                        ret.add(new PluginInfo<T>(f, (Class<T>) cl.loadClass(pkg + "." + classFileName)));
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
        }
        return ret;

    }
}
