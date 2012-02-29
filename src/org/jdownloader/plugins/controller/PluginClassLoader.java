package org.jdownloader.plugins.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.appwork.utils.Application;
import org.appwork.utils.IO;

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

    private Method findLoadedClass;

    private PluginClassLoader() {
        super(new URL[] { Application.getRootUrlByClass(jd.Main.class, null) }, PluginClassLoader.class.getClassLoader());
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            findLoadedClass.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public URLClassLoader getChild() {
        return new URLClassLoader(new URL[] { Application.getRootUrlByClass(jd.Main.class, null) }, this) {
            public Class loadClass(String name) throws ClassNotFoundException {
                try {
                    if (!name.startsWith("jd.plugins.hoster") && !name.startsWith("jd.plugins.decrypter")) { return super.loadClass(name); }

                    System.out.println(name);
                    URL myUrl = Application.getRessourceURL(name.replace(".", "/") + ".class");
                    byte[] data;
                    data = IO.readURL(myUrl);
                    return defineClass(name, data, 0, data.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                }

            }
        };
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
