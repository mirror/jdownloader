package org.jdownloader.plugins.controller;

import java.io.File;

import jd.plugins.Plugin;

public class PluginInfo<T extends Plugin> {

    private File     file;
    private Class<T> clazz;

    public PluginInfo(File file, Class<T> clazz) {
        this.file = file;
        this.clazz = clazz;
    }

    public File getFile() {
        return file;
    }

    public Class<T> getClazz() {
        return clazz;
    }

}
