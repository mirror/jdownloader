package org.jdownloader.plugins.controller;

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
import jd.controlling.packagecontroller.ModifyLock;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class LazyPlugin<T extends Plugin> implements MinTimeWeakReferenceCleanup {

    private class ConstructorInfo<T> {
        protected Constructor<T> constructor;
        protected Object[]       constructorParameters;
    }

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
    private static final HashSet<String>                      immutableClasses        = new HashSet<String>();
    static {
        immutableClasses.add("java.lang.Boolean");
        immutableClasses.add("java.lang.Byte");
        immutableClasses.add("java.lang.String");
        immutableClasses.add("java.lang.Double");
        immutableClasses.add("java.lang.Integer");
        immutableClasses.add("java.lang.Long");
        immutableClasses.add("java.lang.Float");
        immutableClasses.add("java.lang.Short");
        immutableClasses.add("java.math.BigInteger");
        immutableClasses.add("java.math.BigDecimal");
    }

    private static final Object[]                             EMPTY                   = new Object[] {};
    private long                                              version;
    private String                                            pattern;
    private volatile MinTimeWeakReference<Pattern>            compiledPattern         = null;
    private String                                            className;
    private String                                            displayName;
    protected volatile WeakReference<Class<T>>                pluginClass;
    protected String                                          mainClassFilename       = null;
    private final Object[]                                    CONSTRUCTOR;
    private final PluginWrapper                               pluginWrapper;
    private static final LogSource                            LOGGER                  = LogController.getInstance().getCurrentClassLogger();

    public String getMainClassFilename() {
        return mainClassFilename;
    }

    public String getMainClassSHA256() {
        return mainClassSHA256;
    }

    public long getMainClassLastModified() {
        return mainClassLastModified;
    }

    protected String mainClassSHA256       = null;
    protected long   mainClassLastModified = -1;
    protected int    interfaceVersion      = -1;

    public int getInterfaceVersion() {
        return interfaceVersion;
    }

    public void setInterfaceVersion(int interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    protected volatile WeakReference<T>                    prototypeInstance;
    /* PluginClassLoaderChild used to load this Class */
    private volatile WeakReference<PluginClassLoaderChild> classLoader;

    public PluginWrapper getPluginWrapper() {
        return pluginWrapper;
    }

    private String configInterface = null;

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public LazyPlugin(String patternString, String className, String configInterface, String displayName, long version, Class<T> class1, PluginClassLoaderChild classLoader) {
        pattern = patternString;
        this.configInterface = configInterface;
        if (class1 != null) {
            pluginClass = new WeakReference<Class<T>>(class1);
        }
        this.className = className;
        if (Application.getJavaVersion() >= Application.JAVA17) {
            this.displayName = displayName.toLowerCase(Locale.ENGLISH).intern();
        } else {
            this.displayName = displayName;
        }
        this.version = version;
        if (classLoader != null) {
            this.classLoader = new WeakReference<PluginClassLoaderChild>(classLoader);
        }
        pluginWrapper = new PluginWrapper(this) {
            /* workaround for old plugin system */
        };
        CONSTRUCTOR = new Object[] { pluginWrapper };
    }

    public long getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getClassname() {
        return className;
    }

    protected void setClassname(String classname) {
        this.mainClassLastModified = -1;
        this.className = classname;
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
        T ret = null;
        try {
            ConstructorInfo<T> cons = getConstructor(getPluginClass(classLoader));
            ret = cons.constructor.newInstance(cons.constructorParameters);
        } catch (final Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, true);
        }
        try {
            final String cacheID = className;
            SharedPluginObjects savedSharedObjects = null;
            synchronized (sharedPluginObjectsPool) {
                /* fetch SharedPluginObjects from cache */
                savedSharedObjects = sharedPluginObjectsPool.get(cacheID);
                if (savedSharedObjects != null && savedSharedObjects.getVersion() < getVersion()) {
                    /* TODO: do not trash old cache, instead update it to newer version! */
                    LOGGER.info("Remove outdated objectPool version " + savedSharedObjects.getVersion() + " for class " + getClassname() + " because version is now " + getVersion());
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
                            LOGGER.info("Class " + getClassname() + " has static enum: " + field.getName());
                            continue;
                        }
                        if (field.getType().isPrimitive()) {
                            LOGGER.info("Class " + getClassname() + " has static primitive field: " + field.getName());
                            continue;
                        }
                        if (immutableClasses.contains(field.getType().getName())) {
                            LOGGER.info("Class " + getClassname() + " has static immutable field: " + field.getName());
                            continue;
                        }
                        /* we only share static objects */
                        field.setAccessible(true);
                        Object fieldObject = field.get(null);
                        if (fieldObject != null) {
                            currentSharedObjects.put(field.getName(), fieldObject);
                        } else {
                            LOGGER.info("Class " + getClassname() + " has static field: " + field.getName() + " with null content!");
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
                                LOGGER.severe("Class " + getClassname() + " has final Field " + next.getKey() + " now->remove from pool");
                                if (removeList == null) removeList = new ArrayList<String>();
                                removeList.add(fieldID);
                            }
                            continue;
                        }
                        setField.set(null, fieldObject);
                    } catch (final NoSuchFieldException e) {
                        LOGGER.log(e);
                        if (getVersion() > savedSharedObjects.version) {
                            /* field is gone, remove it from pool */
                            LOGGER.severe("Class " + getClassname() + " no longer has Field " + next.getKey() + "->remove from pool");
                            if (removeList == null) removeList = new ArrayList<String>();
                            removeList.add(fieldID);
                        }
                    } catch (final Throwable e) {
                        LOGGER.log(e);
                        LOGGER.severe("Cant modify Field " + setField.getName() + " for " + getClassname());
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

    private ConstructorInfo<T> getConstructor(Class<T> clazz) throws UpdateRequiredClassNotFoundException {
        ConstructorInfo<T> ret = new ConstructorInfo<T>();
        try {
            ret.constructor = clazz.getConstructor(new Class[] { PluginWrapper.class });
            ret.constructorParameters = CONSTRUCTOR;
            return ret;
        } catch (Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, false);
            try {
                ret.constructor = clazz.getConstructor(new Class[] {});
                ret.constructorParameters = EMPTY;
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
                return (Class<T>) classLoader.loadClass(className);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new WTFException(e);
            }
        }
        Class<T> ret = null;
        if (pluginClass != null && (ret = pluginClass.get()) != null) return ret;
        pluginClass = null;
        try {
            ret = (Class<T>) getClassLoader(true).loadClass(className);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new WTFException(e);
        }
        if (ret != null) pluginClass = new WeakReference<Class<T>>(ret);
        return ret;
    }

    public String getPatternSource() {
        return pattern;
    }

    public void setPatternSource(String pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        Pattern ret = null;
        MinTimeWeakReference<Pattern> lCompiledPattern = compiledPattern;
        if (lCompiledPattern != null && (ret = lCompiledPattern.get()) != null) return ret;
        compiledPattern = new MinTimeWeakReference<Pattern>(ret = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), 60 * 1000l, displayName, this);
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
        if (classLoader != null && (ret = classLoader.get()) != null) return ret;
        if (createNew == false) return null;
        ret = PluginClassLoader.getInstance().getSharedChild(this);
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
