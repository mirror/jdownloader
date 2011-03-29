package org.jdownloader.plugins.scanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.appwork.utils.Application;

public class PluginClassLoader extends URLClassLoader {

    private ClassLoader parentClassLoader;

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

    public PluginClassLoader() throws MalformedURLException {
        super(new URL[] { new File(Application.getRoot()).toURI().toURL() }, PluginClassLoader.class.getClassLoader());
        parentClassLoader = PluginClassLoader.class.getClassLoader();
    }

}
