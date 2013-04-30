package org.jdownloader.plugins.controller;

import java.io.File;

import jd.plugins.Plugin;

public class PluginInfo<T extends Plugin> {

    private File       file;
    private Class<T>   clazz;
    private String     mainClassSHA256       = null;
    private long       mainClassLastModified = -1;
    private LazyPlugin lazyPlugin            = null;

    public long getMainClassLastModified() {
        return mainClassLastModified;
    }

    public void setMainClassLastModified(long mainClassLastModified) {
        this.mainClassLastModified = mainClassLastModified;
    }

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

    /**
     * @return the mainClassSHA256
     */
    public String getMainClassSHA256() {
        return mainClassSHA256;
    }

    /**
     * @param mainClassSHA256
     *            the mainClassSHA256 to set
     */
    public void setMainClassSHA256(String mainClassSHA256) {
        this.mainClassSHA256 = mainClassSHA256;
    }

    /**
     * @return the lazyPlugin
     */
    public LazyPlugin getLazyPlugin() {
        return lazyPlugin;
    }

    /**
     * @param lazyPlugin
     *            the lazyPlugin to set
     */
    public void setLazyPlugin(LazyPlugin lazyPlugin) {
        this.lazyPlugin = lazyPlugin;
    }

}
