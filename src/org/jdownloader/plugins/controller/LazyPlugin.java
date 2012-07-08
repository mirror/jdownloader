package org.jdownloader.plugins.controller;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class LazyPlugin<T extends Plugin> {

    private class ConstructorInfo<T extends Plugin> {
        protected Constructor<T> constructor;
        protected Object[]       constructorParameters;
    }

    private static final Object[]                 EMPTY = new Object[] {};
    private long                                  version;
    private Pattern                               pattern;
    private String                                classname;
    private String                                displayName;
    protected WeakReference<Class<T>>             pluginClass;

    protected T                                   prototypeInstance;
    /* PluginClassLoaderChild used to load this Class */
    private WeakReference<PluginClassLoaderChild> classLoader;

    public LazyPlugin(String patternString, String classname, String displayName, long version, Class<T> class1, PluginClassLoaderChild classLoader) {
        pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        if (class1 != null) {
            pluginClass = new WeakReference<Class<T>>(class1);
        }
        this.classname = classname;
        this.displayName = displayName;
        this.version = version;
        if (classLoader != null) {
            this.classLoader = new WeakReference<PluginClassLoaderChild>(classLoader);
        }
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

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public void setPluginClass(Class<T> pluginClass) {
        if (pluginClass == null) {
            this.pluginClass = null;
        } else {
            this.pluginClass = new WeakReference<Class<T>>(pluginClass);
        }
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

    public T getPrototype() throws UpdateRequiredClassNotFoundException {
        ClassLoader lcl = Thread.currentThread().getContextClassLoader();
        if (lcl != null && (lcl != getClassLoader()) && (lcl instanceof PluginClassLoaderChild)) {
            /*
             * current Thread uses a different PluginClassLoaderChild, so lets use it to generate a new instance
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

    public T newInstance() throws UpdateRequiredClassNotFoundException {
        try {
            ConstructorInfo<T> cons = getConstructor(getPluginClass());
            return cons.constructor.newInstance(cons.constructorParameters);
        } catch (final Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, true);
        }
        return null;
    }

    private ConstructorInfo<T> getConstructor(Class<T> clazz) throws UpdateRequiredClassNotFoundException {
        ConstructorInfo<T> ret = new ConstructorInfo<T>();
        try {
            ret.constructor = clazz.getConstructor(new Class[] {});
            ret.constructorParameters = EMPTY;
            return ret;
        } catch (Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, false);
            try {
                ret.constructor = clazz.getConstructor(new Class[] { PluginWrapper.class });
                ret.constructorParameters = new Object[] { new PluginWrapper(this) {
                    /* workaround for old plugin system */
                } };
                return ret;
            } catch (final Throwable e2) {
                handleUpdateRequiredClassNotFoundException(e, true);
            }
        }
        return null;
    }

    public void handleUpdateRequiredClassNotFoundException(Throwable e, boolean ThrowWTF) throws UpdateRequiredClassNotFoundException {
        if (e != null) {
            if (e instanceof NoClassDefFoundError) {
                NoClassDefFoundError ncdf = (NoClassDefFoundError) e;
                String classNotFound = ncdf.getMessage();
                ClassLoader lcl = Thread.currentThread().getContextClassLoader();
                if (lcl == null || !(lcl instanceof PluginClassLoaderChild)) lcl = getClassLoader();
                if (lcl != null && lcl instanceof PluginClassLoaderChild) {
                    PluginClassLoaderChild pcl = (PluginClassLoaderChild) lcl;
                    if (pcl.isUpdateRequired(classNotFound)) throw new UpdateRequiredClassNotFoundException(classNotFound);
                }
            }
            if (e instanceof UpdateRequiredClassNotFoundException) throw (UpdateRequiredClassNotFoundException) e;
            if (ThrowWTF) throw new WTFException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getPluginClass() {
        ClassLoader lcl = Thread.currentThread().getContextClassLoader();
        if (lcl != null && (lcl != getClassLoader()) && (lcl instanceof PluginClassLoaderChild)) {
            /*
             * current Thread uses a different PluginClassLoaderChild, so lets use it to get PluginClass
             */
            try {
                return (Class<T>) lcl.loadClass(classname);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
        }
        Class<T> ret = null;
        if ((ret = getWeakPluginClass()) != null) return ret;
        synchronized (this) {
            if ((ret = getWeakPluginClass()) != null) return ret;
            try {
                ret = (Class<T>) getClassLoader().loadClass(classname);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
            setPluginClass(ret);
            return ret;
        }

    }

    private Class<T> getWeakPluginClass() {
        Class<T> ret = null;
        if (pluginClass != null && (ret = pluginClass.get()) != null) return ret;
        pluginClass = null;
        return null;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @return the classLoader
     */
    public PluginClassLoaderChild getClassLoader() {
        PluginClassLoaderChild ret = null;
        if (classLoader != null && (ret = classLoader.get()) != null) return ret;
        ret = PluginClassLoader.getInstance().getChild();
        classLoader = new WeakReference<PluginClassLoader.PluginClassLoaderChild>(ret);
        return ret;
    }

    public void setClassLoader(PluginClassLoaderChild cl) {
        if (cl == null) {
            classLoader = null;
        } else {
            classLoader = new WeakReference<PluginClassLoader.PluginClassLoaderChild>(cl);
        }
    }
}
