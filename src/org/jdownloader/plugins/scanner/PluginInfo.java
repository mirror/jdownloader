package org.jdownloader.plugins.scanner;

import java.io.File;

public class PluginInfo {

    private File     file;
    private Class<?> clazz;

    public PluginInfo(File file, Class<?> clazz) {
        this.file = file;
        this.clazz = clazz;
    }

    public File getFile() {
        return file;
    }

    public Class<?> getClazz() {
        return clazz;
    }

}
