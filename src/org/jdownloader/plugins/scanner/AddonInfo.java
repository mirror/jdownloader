package org.jdownloader.plugins.scanner;

import java.io.File;

import jd.plugins.OptionalPlugin;

public class AddonInfo {

    private OptionalPlugin annotation;

    public OptionalPlugin getAnnotation() {
        return annotation;
    }

    public File getFile() {
        return file;
    }

    public String getHash() {
        return hash;
    }

    private File     file;
    private String   hash;
    private Class<?> clazz;

    public Class<?> getClazz() {
        return clazz;
    }

    public AddonInfo(OptionalPlugin an, Class<?> clazz, File jar, String md5) {
        this.annotation = an;
        this.clazz = clazz;
        this.file = jar;
        this.hash = md5;
    }

}
