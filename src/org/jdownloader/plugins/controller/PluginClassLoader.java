package org.jdownloader.plugins.controller;

import java.net.URL;
import java.net.URLClassLoader;

import org.appwork.utils.Application;

public class PluginClassLoader extends URLClassLoader {

    // private ClassLoader parentClassLoader;
    //
    // @Override
    // protected synchronized Class<?> loadClass(String name, boolean resolve)
    // throws ClassNotFoundException {
    // try {
    // return super.loadClass(name, resolve);
    // } catch (ClassNotFoundException e) {
    // return parentClassLoader.loadClass(name);
    // }
    //
    // }
    //
    // @Override
    // public URL getResource(String name) {
    //
    // URL ret = super.getResource(name);
    // if (ret == null) ret = parentClassLoader.getResource(name);
    // return ret;
    //
    // }
    //
    // @Override
    // public Enumeration<URL> getResources(String name) throws IOException {
    //
    // Enumeration<URL> ret = super.getResources(name);
    // if (ret == null || !ret.hasMoreElements()) ret =
    // parentClassLoader.getResources(name);
    // return ret;
    // }
    private static final PluginClassLoader INSTANCE = new PluginClassLoader();

    public static PluginClassLoader getInstance() {
        return INSTANCE;
    }

    private PluginClassLoader() {
        super(new URL[] { Application.getRootUrlByClass(jd.Main.class, null) }, PluginClassLoader.class.getClassLoader());
    }

}
