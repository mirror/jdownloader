package org.jdownloader.plugins.controller;

import java.io.File;

import jd.plugins.Plugin;

import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public class PluginInfo<T extends Plugin> {

    private File                   file;
    private Class<T>               clazz;
    private PluginClassLoaderChild cl;

    public PluginInfo(File file, Class<T> clazz, PluginClassLoaderChild cl) {
        this.file = file;
        this.clazz = clazz;
        this.cl = cl;
    }

    public File getFile() {
        return file;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    /**
     * @return the cl
     */
    public PluginClassLoaderChild getClassLoader() {
        return cl;
    }

}
