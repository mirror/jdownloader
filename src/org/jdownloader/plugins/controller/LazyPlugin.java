package org.jdownloader.plugins.controller;

import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;

public abstract class LazyPlugin<T extends Plugin> {
    private static final Object[] EMPTY = new Object[] {};
    private static final Object[] OLD   = new Object[] { null };
    private Pattern               pattern;
    private String                classname;
    private String                displayName;
    protected Class<T>            pluginClass;
    private Constructor<T>        constructor;
    private Object[]              constructorParameters;
    protected T                   prototypeInstance;

    public LazyPlugin(String patternString, String classname, String displayName) {
        pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        this.classname = classname;
        this.displayName = displayName;
    }

    public boolean isLoaded() {
        return pluginClass != null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getClassname() {
        return classname;
    }

    public boolean canHandle(String url) {

        final Pattern pattern = this.getPattern();
        if (pattern != null) {
            final Matcher matcher = pattern.matcher(url);
            return matcher.find();
        } else {
            return false;
        }
    }

    public T getPrototype() {
        if (prototypeInstance != null) return prototypeInstance;
        synchronized (this) {
            if (prototypeInstance != null) return prototypeInstance;
            prototypeInstance = newInstance();
        }
        return prototypeInstance;
    }

    public T newInstance() {
        try {
            getConstructor();
            return constructor.newInstance(constructorParameters);
        } catch (final Throwable e) {
            throw new WTFException(e);
        }

    }

    private Constructor<T> getConstructor() {
        if (constructor != null) return constructor;
        synchronized (this) {
            if (constructor != null) return constructor;
            try {
                constructor = getPluginClass().getConstructor(new Class[] {});
                constructorParameters = EMPTY;
            } catch (Throwable e) {
                try {
                    constructor = getPluginClass().getConstructor(new Class[] { PluginWrapper.class });
                    constructorParameters = new Object[] { new PluginWrapper(this) {

                    } };
                } catch (final Throwable e2) {
                    throw new WTFException(e2);
                }
            }
            return constructor;
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getPluginClass() {
        if (pluginClass != null) return pluginClass;
        synchronized (this) {
            if (pluginClass != null) return pluginClass;
            try {
                pluginClass = (Class<T>) PluginClassLoader.getInstance().loadClass(classname);
            } catch (Throwable e) {
                throw new WTFException(e);
            }
            return pluginClass;
        }

    }

    public Pattern getPattern() {
        return pattern;
    }
}
