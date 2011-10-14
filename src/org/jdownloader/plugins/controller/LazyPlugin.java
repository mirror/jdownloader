package org.jdownloader.plugins.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    private Class<T>              pluginClass;
    private Constructor<T>        constructor;
    private Object[]              constructorParameters;

    public LazyPlugin(String patternString, String classname, String displayName) {
        pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        this.classname = classname;
        this.displayName = displayName;
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

    public T newInstance() {

        try {
            getConstructor();
            return constructor.newInstance(constructorParameters);
        } catch (InstantiationException e) {
            throw new WTFException(e);
        } catch (IllegalAccessException e) {
            throw new WTFException(e);
        } catch (IllegalArgumentException e) {
            throw new WTFException(e);
        } catch (InvocationTargetException e) {
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
            } catch (NoSuchMethodException e) {
                try {
                    constructor = getPluginClass().getConstructor(new Class[] { PluginWrapper.class });
                    constructorParameters = OLD;
                } catch (NoSuchMethodException e1) {
                    throw new WTFException(e1);
                } catch (SecurityException e1) {
                    throw new WTFException(e1);
                }
            } catch (SecurityException e) {
                throw new WTFException(e);
            }
            return constructor;
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getPluginClass() {
        if (pluginClass != null) return pluginClass;
        synchronized (this) {
            // try again synched
            if (pluginClass != null) return pluginClass;
            try {
                pluginClass = (Class<T>) Class.forName(classname);
            } catch (ClassNotFoundException e) {
                throw new WTFException(e);
            }
            return pluginClass;
        }

    }

    public Pattern getPattern() {
        return pattern;
    }
}
