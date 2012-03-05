package org.jdownloader.plugins.controller;

import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class LazyPlugin<T extends Plugin> {
    private static final Object[]        EMPTY = new Object[] {};
    private long                         version;
    private Pattern                      pattern;
    private String                       classname;
    private String                       displayName;
    protected Class<T>                   pluginClass;
    private Constructor<T>               constructor;
    private Object[]                     constructorParameters;
    protected T                          prototypeInstance;
    /* PluginClassLoaderChild used to load this Class */
    final private PluginClassLoaderChild classLoader;

    public LazyPlugin(String patternString, String classname, String displayName, long version, Class<T> class1, PluginClassLoaderChild classLoader) {
        pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        pluginClass = class1;
        this.classname = classname;
        this.displayName = displayName;
        this.version = version;
        this.classLoader = classLoader;
    }

    public long getVersion() {
        return version;
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
        ClassLoader lcl = Thread.currentThread().getContextClassLoader();
        if (lcl != null && (lcl != classLoader) && (lcl instanceof PluginClassLoaderChild)) {
            /*
             * current Thread uses a different PluginClassLoaderChild, so lets
             * use it to generate a new instance
             */
            return newInstance();
        }
        if (prototypeInstance != null) return prototypeInstance;
        synchronized (this) {
            if (prototypeInstance != null) return prototypeInstance;
            prototypeInstance = newInstance();
        }
        return prototypeInstance;
    }

    public T newInstance() {
        try {
            Constructor<T> cons = getConstructor(getPluginClass());
            return cons.newInstance(constructorParameters);
        } catch (final Throwable e) {
            throw new WTFException(e);
        }

    }

    private Constructor<T> getConstructor(Class<T> clazz) {
        if (clazz != pluginClass) {
            /*
             * a different PluginClassLoaderChild might be in use, so lets use
             * the new clazz
             */
            try {
                Constructor<T> lconstructor = clazz.getConstructor(new Class[] {});
                constructorParameters = EMPTY;
                return lconstructor;
            } catch (Throwable e) {
                try {
                    Constructor<T> lconstructor = clazz.getConstructor(new Class[] { PluginWrapper.class });
                    constructorParameters = new Object[] { new PluginWrapper(this) {
                        /* workaround for old plugin system */
                    } };
                    return lconstructor;
                } catch (final Throwable e2) {
                    throw new WTFException(e2);
                }
            }
        }
        if (constructor != null) return constructor;
        synchronized (this) {
            if (constructor != null) return constructor;
            try {
                constructor = clazz.getConstructor(new Class[] {});
                constructorParameters = EMPTY;
            } catch (Throwable e) {
                try {
                    constructor = clazz.getConstructor(new Class[] { PluginWrapper.class });
                    constructorParameters = new Object[] { new PluginWrapper(this) {
                        /* workaround for old plugin system */
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
        ClassLoader lcl = Thread.currentThread().getContextClassLoader();
        if (lcl != null && (lcl != classLoader) && (lcl instanceof PluginClassLoaderChild)) {
            /*
             * current Thread uses a different PluginClassLoaderChild, so lets
             * use it to get PluginClass
             */
            try {
                return (Class<T>) lcl.loadClass(classname);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
        }
        if (pluginClass != null) return pluginClass;
        synchronized (this) {
            if (pluginClass != null) return pluginClass;
            try {
                pluginClass = (Class<T>) classLoader.loadClass(classname);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
            return pluginClass;
        }

    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @return the classLoader
     */
    public PluginClassLoaderChild getClassLoader() {
        return classLoader;
    }
}
