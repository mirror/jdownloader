package org.jdownloader.plugins.controller;

import jd.plugins.DecrypterPlugin;
import jd.plugins.HostPlugin;
import jd.plugins.Plugin;

import org.appwork.utils.Application;

public class PluginInfo<T extends Plugin> {

    private final Class<T>        clazz;
    private LazyPlugin<T>         lazyPlugin      = null;
    private final LazyPluginClass lazyPluginClass;

    private final String          simpleName;
    private final String[]        names;
    private final boolean         valid;
    private final static boolean  isJava16orOlder = Application.getJavaVersion() <= Application.JAVA16;

    public boolean isValid() {
        return valid;
    }

    public String[] getNames() {
        return names;
    }

    public String[] getPatterns() {
        return patterns;
    }

    private String[]     patterns;
    private final String clazzName;

    public String getClazzName() {
        return clazzName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public final LazyPluginClass getLazyPluginClass() {
        return lazyPluginClass;
    }

    public PluginInfo(LazyPluginClass lazyPluginClass, Class<T> clazz) {
        this.lazyPluginClass = lazyPluginClass;
        this.simpleName = new String(clazz.getSimpleName());
        this.clazzName = clazz.getName();
        String[] names = null;
        String[] patterns = null;
        boolean valid = false;
        if (clazzName.startsWith("jd.plugins.hoster")) {
            final HostPlugin hoster = clazz.getAnnotation(HostPlugin.class);
            if (hoster != null) {
                valid = true;
                names = hoster.names();
                patterns = hoster.urls();
            }
        } else if (clazzName.startsWith("jd.plugins.decrypter")) {
            final DecrypterPlugin decrypter = clazz.getAnnotation(DecrypterPlugin.class);
            if (decrypter != null) {
                valid = true;
                names = decrypter.names();
                patterns = decrypter.urls();
            }
        }
        if (names == null) {
            names = new String[0];
        }
        if (patterns == null) {
            patterns = new String[0];
        }
        this.valid = valid;
        this.names = names;
        this.patterns = patterns;
        if (isJava16orOlder) {
            this.clazz = null;
        } else {
            this.clazz = clazz;
        }
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
