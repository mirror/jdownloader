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

    protected WeakReference<T>                    prototypeInstance;
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

    public synchronized void setPluginClass(Class<T> pluginClass) {
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

    public synchronized T getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        if (classLoader != null && classLoader != getClassLoader(false)) {
            /* create new Instance because we have different classLoader given than ProtoTypeClassLoader */
            return newInstance(classLoader);
        }
        T ret = null;
        if (prototypeInstance != null && (ret = prototypeInstance.get()) != null) return ret;
        prototypeInstance = null;
        ret = newInstance(null);
        if (ret != null) prototypeInstance = new WeakReference<T>(ret);
        return ret;
    }

    public T newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        try {
            ConstructorInfo<T> cons = getConstructor(getPluginClass(classLoader));
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
                if (lcl == null || !(lcl instanceof PluginClassLoaderChild)) lcl = getClassLoader(true);
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
    protected synchronized Class<T> getPluginClass(PluginClassLoaderChild classLoader) {
        if (classLoader != null && classLoader != getClassLoader(false)) {
            /* load class with custom classLoader because it's not default one */
            try {
                return (Class<T>) classLoader.loadClass(classname);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
        }
        Class<T> ret = null;
        if (pluginClass != null && (ret = pluginClass.get()) != null) return ret;
        pluginClass = null;
        try {
            ret = (Class<T>) getClassLoader(true).loadClass(classname);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new WTFException(e);
        }
        if (ret != null) pluginClass = new WeakReference<Class<T>>(ret);
        return ret;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @return the classLoader
     */
    public synchronized PluginClassLoaderChild getClassLoader(boolean createNew) {
        PluginClassLoaderChild ret = null;
        if (classLoader != null && (ret = classLoader.get()) != null) return ret;
        if (createNew == false) return null;
        ret = PluginClassLoader.getInstance().getChild();
        setClassLoader(ret);
        return ret;
    }

    public synchronized void setClassLoader(PluginClassLoaderChild cl) {
        if (cl == null) {
            classLoader = null;
        } else {
            classLoader = new WeakReference<PluginClassLoader.PluginClassLoaderChild>(cl);
        }
    }
}
