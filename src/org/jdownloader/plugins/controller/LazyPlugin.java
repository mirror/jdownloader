package org.jdownloader.plugins.controller;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class LazyPlugin<T extends Plugin> implements MinTimeWeakReferenceCleanup {
    private final byte[]                                   patternBytes;
    private volatile MinTimeWeakReference<Pattern>         compiledPattern = null;
    private final String                                   displayName;
    protected volatile WeakReference<Class<T>>             pluginClass;
    protected volatile WeakReference<T>                    prototypeInstance;
    /* PluginClassLoaderChild used to load this Class */
    private volatile WeakReference<PluginClassLoaderChild> classLoader;
    private volatile MinTimeWeakReference<Matcher>         matcher         = null;

    public PluginWrapper getPluginWrapper() {
        return new PluginWrapper(this) {
            /* workaround for old plugin system */
        };
    }

    private final LazyPluginClass lazyPluginClass;

    public final LazyPluginClass getLazyPluginClass() {
        return lazyPluginClass;
    }

    public LazyPlugin(LazyPluginClass lazyPluginClass, String patternString, String displayName, Class<T> class1, PluginClassLoaderChild classLoader) {
        byte[] patternBytes = null;
        try {
            patternBytes = patternString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            patternBytes = patternString.getBytes();
        }
        this.patternBytes = patternBytes;
        this.lazyPluginClass = lazyPluginClass;
        if (class1 != null) {
            pluginClass = new WeakReference<Class<T>>(class1);
        }
        if (Application.getJavaVersion() >= Application.JAVA17) {
            this.displayName = displayName.toLowerCase(Locale.ENGLISH).intern();
        } else {
            this.displayName = displayName.toLowerCase(Locale.ENGLISH);
        }
        if (classLoader != null) {
            this.classLoader = new WeakReference<PluginClassLoaderChild>(classLoader);
        }
    }

    public String getID() {
        return getClassName() + "/" + getDisplayName();
    }

    public boolean equals(Object lazyPlugin) {
        if (lazyPlugin == this) {
            return true;
        }
        if (lazyPlugin != null && lazyPlugin instanceof LazyPlugin && getClass().isAssignableFrom(lazyPlugin.getClass())) {
            final LazyPlugin<?> other = (LazyPlugin<?>) lazyPlugin;
            if (!StringUtils.equals(getDisplayName(), other.getDisplayName())) {
                return false;
            }
            if (!StringUtils.equals(getClassName(), other.getClassName())) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return displayName.hashCode();
    }

    public final long getVersion() {
        return getLazyPluginClass().getRevision();
    }

    public abstract String getClassName();

    public final String getDisplayName() {
        return displayName;
    }

    public synchronized void setPluginClass(Class<T> pluginClass) {
        if (pluginClass == null) {
            this.pluginClass = null;
        } else {
            this.pluginClass = new WeakReference<Class<T>>(pluginClass);
        }
    }

    public boolean canHandle(String url) {
        if (patternBytes.length > 0) {
            final Matcher matcher = getMatcher();
            synchronized (matcher) {
                try {
                    return matcher.reset(url).find();
                } finally {
                    matcher.reset("");
                }
            }
        }
        return false;
    }

    public synchronized T getPrototype(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        if (classLoader != null && classLoader != getClassLoader(false)) {
            /* create new Instance because we have different classLoader given than ProtoTypeClassLoader */
            return newInstance(classLoader);
        }
        T ret = null;
        if (prototypeInstance != null && (ret = prototypeInstance.get()) != null) {
            return ret;
        }
        prototypeInstance = null;
        ret = newInstance(null);
        if (ret != null) {
            prototypeInstance = new WeakReference<T>(ret);
        }
        return ret;
    }

    public T newInstance(PluginClassLoaderChild classLoader) throws UpdateRequiredClassNotFoundException {
        T ret = null;
        try {
            final Class<T> clazz = getPluginClass(classLoader);
            final Constructor<T> cons = getConstructor(clazz);
            if (cons.getParameterTypes().length != 0) {
                ret = cons.newInstance(new Object[] { getPluginWrapper() });
            } else {
                ret = cons.newInstance(new Object[0]);
            }
        } catch (final Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, true);
        }
        return ret;
    }

    private Constructor<T> getConstructor(Class<T> clazz) throws UpdateRequiredClassNotFoundException {
        try {
            return clazz.getConstructor(new Class[] { PluginWrapper.class });
        } catch (Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, false);
            try {
                return clazz.getConstructor(new Class[] {});
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
                if (lcl == null || !(lcl instanceof PluginClassLoaderChild)) {
                    lcl = getClassLoader(true);
                }
                if (lcl != null && lcl instanceof PluginClassLoaderChild) {
                    PluginClassLoaderChild pcl = (PluginClassLoaderChild) lcl;
                    if (pcl.isUpdateRequired(classNotFound)) {
                        throw new UpdateRequiredClassNotFoundException(classNotFound);
                    }
                }
            }
            if (e instanceof UpdateRequiredClassNotFoundException) {
                throw (UpdateRequiredClassNotFoundException) e;
            }
            if (ThrowWTF) {
                throw new WTFException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected synchronized Class<T> getPluginClass(PluginClassLoaderChild classLoader) {
        if (classLoader != null && classLoader != getClassLoader(false)) {
            /* load class with custom classLoader because it's not default one */
            try {
                return (Class<T>) classLoader.loadClass(getClassName());
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
        }
        Class<T> ret = null;
        if (pluginClass != null && (ret = pluginClass.get()) != null) {
            return ret;
        }
        pluginClass = null;
        try {
            ret = (Class<T>) getClassLoader(true).loadClass(getClassName());
        } catch (Throwable e) {
            e.printStackTrace();
            throw new WTFException(e);
        }
        if (ret != null) {
            pluginClass = new WeakReference<Class<T>>(ret);
        }
        return ret;
    }

    public final String getPatternSource() {
        try {
            return new String(patternBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String(patternBytes);
        }
    }

    public final Matcher getMatcher() {
        Matcher ret = null;
        final MinTimeWeakReference<Matcher> lMatcher = matcher;
        if (lMatcher != null && (ret = lMatcher.get()) != null) {
            return ret;
        }
        matcher = new MinTimeWeakReference<Matcher>((ret = getPattern().matcher("")), 60 * 1000l, displayName, this);
        return ret;
    }

    public final Pattern getPattern() {
        Pattern ret = null;
        final MinTimeWeakReference<Pattern> lCompiledPattern = compiledPattern;
        if (lCompiledPattern != null && (ret = lCompiledPattern.get()) != null) {
            return ret;
        }
        compiledPattern = new MinTimeWeakReference<Pattern>(ret = Pattern.compile(getPatternSource(), Pattern.CASE_INSENSITIVE), 60 * 1000l, displayName, this);
        return ret;
    }

    @Override
    public synchronized void onMinTimeWeakReferenceCleanup(MinTimeWeakReference<?> minTimeWeakReference) {
        if (minTimeWeakReference == compiledPattern) {
            compiledPattern = null;
        } else if (minTimeWeakReference == classLoader) {
            classLoader = null;
        } else if (minTimeWeakReference == matcher) {
            matcher = null;
        }
    }

    /**
     * @return the classLoader
     */
    public synchronized PluginClassLoaderChild getClassLoader(boolean createNew) {
        PluginClassLoaderChild ret = null;
        if (classLoader != null && (ret = classLoader.get()) != null) {
            return ret;
        }
        if (createNew == false) {
            return null;
        }
        ret = PluginClassLoader.getSharedChild(this);
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

    @Override
    public String toString() {
        return getDisplayName() + "@" + getLazyPluginClass();
    }
}
