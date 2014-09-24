package org.jdownloader.plugins.controller;

import jd.plugins.Plugin;

public class PluginInfo<T extends Plugin> {

    private final Class<T>        clazz;
    private LazyPlugin<T>         lazyPlugin = null;
    private final LazyPluginClass lazyPluginClass;

    public final LazyPluginClass getLazyPluginClass() {
        return lazyPluginClass;
    }

    public PluginInfo(LazyPluginClass lazyPluginClass, Class<T> clazz) {
        this.lazyPluginClass = lazyPluginClass;
        this.clazz = clazz;
    }

    public final Class<T> getClazz() {
        return clazz;
    }

    public LazyPlugin<T> getLazyPlugin() {
        return lazyPlugin;
    }

    public void setLazyPlugin(LazyPlugin<T> lazyPlugin) {
        this.lazyPlugin = lazyPlugin;
    }

}
