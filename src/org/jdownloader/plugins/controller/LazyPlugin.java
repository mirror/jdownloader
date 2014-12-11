package org.jdownloader.plugins.controller;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
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
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;

public abstract class LazyPlugin<T extends Plugin> implements MinTimeWeakReferenceCleanup {

    private static final class SharedPluginObjects extends HashMap<String, Object> {

        private SharedPluginObjects() {
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

    public boolean equals(LazyPlugin<T> lazyPlugin) {
        if (lazyPlugin == null) {
            return false;
        }
        if (lazyPlugin == this) {
            return true;
        }
        if (!StringUtils.equals(getDisplayName(), lazyPlugin.getDisplayName())) {
            return false;
        }
        return true;
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
        if (pattern != null && StringUtils.isNotEmpty(pattern.pattern())) {
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
        Class<?> currentClass = ret.getClass();
        try {
            while (currentClass != null && currentClass.getClassLoader() instanceof PluginClassLoaderChild) {
                final String currentClassName = currentClass.getName();
                final SharedPluginObjects sharedPluginObjects;
                synchronized (sharedPluginObjectsPool) {
                    SharedPluginObjects contains = sharedPluginObjectsPool.get(currentClassName);
                    if (contains == null) {
                        contains = new SharedPluginObjects();
                        sharedPluginObjectsPool.put(currentClassName, contains);
                    }
                    sharedPluginObjects = contains;
                }
                final Field[] fields = currentClass.getDeclaredFields();
                synchronized (sharedPluginObjects) {
                    final HashSet<String> knownFields = new HashSet<String>(sharedPluginObjects.keySet());
                    for (Field field : fields) {
                        final String fieldName = field.getName();
                        if (!field.isSynthetic()) {
                            final int modifiers = field.getModifiers();
                            final boolean isStatic = (modifiers & Modifier.STATIC) != 0;
                            final boolean isFinal = (modifiers & Modifier.FINAL) != 0;
                            if (isStatic && !isFinal) {
                                if (field.getType().isEnum() || field.isEnumConstant()) {
                                    LOGGER.info("Class " + currentClassName + " has static enum: " + fieldName);
                                    continue;
                                }
                                if (field.getType().isPrimitive()) {
                                    LOGGER.info("Class " + currentClassName + " has static primitive field: " + fieldName);
                                    continue;
                                }
                                if (immutableClasses.contains(field.getType().getName())) {
                                    LOGGER.info("Class " + currentClassName + " has static immutable field: " + fieldName);
                                    continue;
                                }
                                /* we only share static objects */
                                field.setAccessible(true);
                                if (knownFields.contains(fieldName)) {
                                    final Object fieldObject = sharedPluginObjects.get(fieldName);
                                    try {
                                        field.set(null, fieldObject);
                                        knownFields.remove(fieldName);
                                        continue;
                                    } catch (final Throwable e) {
                                        LOGGER.severe("Cant modify Field " + fieldName + " for " + currentClassName);
                                    }
                                }
                                final Object fieldObject = field.get(null);
                                if (fieldObject != null) {
                                    sharedPluginObjects.put(fieldName, fieldObject);
                                } else {
                                    LOGGER.info("Class " + currentClassName + " has static field: " + fieldName + " with null content!");
                                }
                            }
                        }
                    }
                    for (final String missingField : knownFields) {
                        LOGGER.info("Class " + currentClassName + " no longer has static field: " + missingField);
                        sharedPluginObjects.remove(missingField);
                    }
                }
                currentClass = currentClass.getSuperclass();
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
