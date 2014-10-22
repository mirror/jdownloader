package org.jdownloader.plugins.controller;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.Application;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class LazyPlugin<T extends Plugin> implements MinTimeWeakReferenceCleanup {

    private static final class SharedPluginObjects extends HashMap<String, Object> {
        protected final long version;

        private SharedPluginObjects(final long version) {
            this.version = version;
        }

        protected final ModifyLock modifyLock = new ModifyLock();

        protected final ModifyLock getModifyLock() {
            return modifyLock;
        }

        protected final long getVersion() {
            return version;
        }
    }

    private static final HashMap<String, SharedPluginObjects> sharedPluginObjectsPool = new HashMap<String, SharedPluginObjects>();
    private static final HashSet<String>                      immutableClasses        = new HashSet<String>() {
                                                                                          {
                                                                                              add("java.lang.Boolean");
                                                                                              add("java.lang.Byte");
                                                                                              add("java.lang.String");
                                                                                              add("java.lang.Double");
                                                                                              add("java.lang.Integer");
                                                                                              add("java.lang.Long");
                                                                                              add("java.lang.Float");
                                                                                              add("java.lang.Short");
                                                                                              add("java.math.BigInteger");
                                                                                              add("java.math.BigDecimal");
                                                                                          }
                                                                                      };

    private final byte[]                                      patternBytes;
    private volatile MinTimeWeakReference<Pattern>            compiledPattern         = null;
    private final String                                      displayName;
    protected volatile WeakReference<Class<T>>                pluginClass;

    private static final LogSource                            LOGGER                  = LogController.getInstance().getCurrentClassLogger();

    protected volatile WeakReference<T>                       prototypeInstance;
    /* PluginClassLoaderChild used to load this Class */
    private volatile WeakReference<PluginClassLoaderChild>    classLoader;

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
            this.displayName = displayName;
        }
        if (classLoader != null) {
            this.classLoader = new WeakReference<PluginClassLoaderChild>(classLoader);
        }
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
        try {
            final String cacheID = getClassName();
            SharedPluginObjects savedSharedObjects = null;
            synchronized (sharedPluginObjectsPool) {
                /* fetch SharedPluginObjects from cache */
                savedSharedObjects = sharedPluginObjectsPool.get(cacheID);
                if (savedSharedObjects != null && savedSharedObjects.getVersion() < getVersion()) {
                    /* TODO: do not trash old cache, instead update it to newer version! */
                    LOGGER.info("Remove outdated objectPool version " + savedSharedObjects.getVersion() + " for class " + cacheID + " because version is now " + getVersion());
                    sharedPluginObjectsPool.remove(cacheID);
                    savedSharedObjects = null;
                }
            }
            if (savedSharedObjects == null) {
                /* no cache available */
                HashMap<String, Object> currentSharedObjects = new HashMap<String, Object>();
                Field[] fields = ret.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isSynthetic()) {
                        /* we ignore synthetic fields */
                        continue;
                    }
                    final int modifiers = field.getModifiers();
                    final boolean isStatic = (modifiers & Modifier.STATIC) != 0;
                    if (isStatic) {
                        if ((modifiers & Modifier.FINAL) != 0) {
                            /* we ignore final objects */
                            continue;
                        }
                        if (field.getType().isEnum() || field.isEnumConstant()) {
                            LOGGER.info("Class " + cacheID + " has static enum: " + field.getName());
                            continue;
                        }
                        if (field.getType().isPrimitive()) {
                            LOGGER.info("Class " + cacheID + " has static primitive field: " + field.getName());
                            continue;
                        }
                        if (immutableClasses.contains(field.getType().getName())) {
                            LOGGER.info("Class " + cacheID + " has static immutable field: " + field.getName());
                            continue;
                        }
                        /* we only share static objects */
                        field.setAccessible(true);
                        Object fieldObject = field.get(null);
                        if (fieldObject != null) {
                            currentSharedObjects.put(field.getName(), fieldObject);
                        } else {
                            LOGGER.info("Class " + cacheID + " has static field: " + field.getName() + " with null content!");
                        }
                    }
                }
                final SharedPluginObjects newSavedSharedObjects = new SharedPluginObjects(getVersion());
                newSavedSharedObjects.putAll(currentSharedObjects);
                synchronized (sharedPluginObjectsPool) {
                    /* put SharedPluginObjects into cache if not set yet */
                    savedSharedObjects = sharedPluginObjectsPool.get(cacheID);
                    if (savedSharedObjects == null) {
                        savedSharedObjects = newSavedSharedObjects;
                        sharedPluginObjectsPool.put(cacheID, savedSharedObjects);
                        return ret;
                    }
                }
            }
            ArrayList<String> removeList = null;
            boolean readL = savedSharedObjects.modifyLock.readLock();
            try {
                Iterator<Entry<String, Object>> it = savedSharedObjects.entrySet().iterator();
                Class<?> c = ret.getClass();
                while (it.hasNext()) {
                    Entry<String, Object> next = it.next();
                    String fieldID = next.getKey();
                    Object fieldObject = next.getValue();
                    Field setField = null;
                    try {
                        /* restore shared Object */
                        setField = c.getDeclaredField(fieldID);
                        setField.setAccessible(true);
                        if ((setField.getModifiers() & Modifier.FINAL) != 0) {
                            /* field seems to be final now, dont change it */
                            if (getVersion() > savedSharedObjects.version) {
                                /* remove from pool */
                                LOGGER.severe("Class " + cacheID + " has final Field " + next.getKey() + " now->remove from pool");
                                if (removeList == null) {
                                    removeList = new ArrayList<String>();
                                }
                                removeList.add(fieldID);
                            }
                            continue;
                        }
                        setField.set(null, fieldObject);
                    } catch (final NoSuchFieldException e) {
                        LOGGER.log(e);
                        if (getVersion() > savedSharedObjects.version) {
                            /* field is gone, remove it from pool */
                            LOGGER.severe("Class " + cacheID + " no longer has Field " + next.getKey() + "->remove from pool");
                            if (removeList == null) {
                                removeList = new ArrayList<String>();
                            }
                            removeList.add(fieldID);
                        }
                    } catch (final Throwable e) {
                        LOGGER.log(e);
                        LOGGER.severe("Cant modify Field " + setField.getName() + " for " + cacheID);
                        e.printStackTrace();
                    }
                }
            } finally {
                savedSharedObjects.modifyLock.readUnlock(readL);
            }
            if (removeList != null) {
                try {
                    savedSharedObjects.modifyLock.writeLock();
                    for (String fieldID : removeList) {
                        savedSharedObjects.remove(fieldID);
                    }
                } finally {
                    savedSharedObjects.modifyLock.writeUnlock();
                }
            }
        } catch (final Throwable e) {
            LOGGER.log(e);
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

    public final Pattern getPattern() {
        Pattern ret = null;
        MinTimeWeakReference<Pattern> lCompiledPattern = compiledPattern;
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
