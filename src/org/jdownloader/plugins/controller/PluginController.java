package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.ArrayList;

import jd.plugins.Plugin;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;

public class PluginController<T extends Plugin> {

    @SuppressWarnings("unchecked")
    public ArrayList<PluginInfo<T>> scan(String hosterpath) throws MalformedURLException {
        final File[] files = Application.getResource(hosterpath).listFiles(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".class") && !name.contains("$");
            }
        });
        final URLClassLoader cl = new PluginClassLoader();
        final String pkg = hosterpath.replace("/", ".");
        final ArrayList<PluginInfo<T>> ret = new ArrayList<PluginInfo<T>>();

        for (final File f : files) {

            try {
                ret.add(new PluginInfo<T>(f, (Class<T>) cl.loadClass(pkg + "." + f.getName().substring(0, f.getName().length() - 6))));
                Log.L.finer("Loaded from: " + cl.getResource(hosterpath + "/" + f.getName()));
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }
        return ret;
    }
}
