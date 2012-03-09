package org.jdownloader.plugins.controller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging.Log;

public class PluginClassLoader extends URLClassLoader {

    public static class PluginClassLoaderChild extends URLClassLoader {

        public PluginClassLoaderChild(ClassLoader parent) {
            super(new URL[] { Application.getRootUrlByClass(jd.Launcher.class, null) }, parent);
        }

        @Override
        public Class loadClass(String name) throws ClassNotFoundException {
            try {
                if (!name.startsWith("jd.plugins.hoster") && !name.startsWith("jd.plugins.decrypter")) { return super.loadClass(name); }
                if (name.startsWith("jd.plugins.hoster.RTMPDownload")) { return super.loadClass(name); }
                Class<?> c = findLoadedClass(name);
                if (c != null) {
                    // System.out.println("Class has already been loaded by this PluginClassLoaderChild");
                    return c;
                }
                // Log.L.info(name.replace(".", "/") + ".class");
                URL myUrl = Application.getRessourceURL(name.replace(".", "/") + ".class");
                byte[] data;
                data = IO.readURL(myUrl);
                return defineClass(name, data, 0, data.length);
            } catch (Exception e) {
                Log.exception(e);
                throw new ClassNotFoundException(e.getMessage(), e);
            }

        }
    }

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

    private Method findLoadedClass;

    private PluginClassLoader() {
        super(new URL[] { Application.getRootUrlByClass(jd.Launcher.class, null) }, PluginClassLoader.class.getClassLoader());
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            findLoadedClass.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public PluginClassLoaderChild getChild() {
        return new PluginClassLoaderChild(this);
    }

    public boolean isClassLoaded(String string) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean ret = this.findLoadedClass(string) != null;
        ClassLoader p;
        ClassLoader lastP = this;
        while ((p = this.getParent()) != null) {
            if (p == lastP) break;

            ret |= findLoadedClass.invoke(p, string) != null;
            lastP = p;
        }
        return ret;
    }

}
