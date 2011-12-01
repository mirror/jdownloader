package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLClassLoader;
import java.util.ArrayList;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;

public class PluginController<T extends Plugin> {

    @SuppressWarnings("unchecked")
    public ArrayList<PluginInfo<T>> scan(String hosterpath) {
        File path = null;
        URLClassLoader cl = null;

        path = Application.getRootByClass(jd.Main.class, hosterpath);
        cl = PluginClassLoader.getInstance();

        final File[] files = path.listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".class") && !name.contains("$");
            }
        });

        final ArrayList<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();
        final String pkg = hosterpath.replace("/", ".");
        if (files != null) {
            for (final File f : files) {

                try {
                    String classFileName = f.getName().substring(0, f.getName().length() - 6);
                    ret.add(new PluginInfo<T>(f, (Class<T>) cl.loadClass(pkg + "." + classFileName)));
                    Log.L.finer("Loaded from: " + cl.getResource(hosterpath + "/" + f.getName()));
                } catch (Throwable e) {
                    Log.exception(e);
                }
            }
        }
        return ret;

    }
}
