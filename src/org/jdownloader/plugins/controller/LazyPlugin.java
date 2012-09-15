package org.jdownloader.plugins.controller;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
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

    private static class SharedPluginObjects {
        protected HashMap<String, Object> sharedObjects = null;
        protected long                    version       = -1;
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
    private Pattern                                           pattern;
    private String                                            classname;
    private String                                            displayName;
    protected WeakReference<Class<T>>                         pluginClass;

    protected WeakReference<T>                                prototypeInstance;
    /* PluginClassLoaderChild used to load this Class */
    private WeakReference<PluginClassLoaderChild>             classLoader;

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
        T ret = null;
        try {
            ConstructorInfo<T> cons = getConstructor(getPluginClass(classLoader));
            ret = cons.constructor.newInstance(cons.constructorParameters);
        } catch (final Throwable e) {
            handleUpdateRequiredClassNotFoundException(e, true);
        }
        try {
            synchronized (sharedPluginObjectsPool) {
                HashMap<String, Object> currentSharedObjects = new HashMap<String, Object>();
                Field[] fields = ret.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if ((field.getModifiers() & Modifier.STATIC) != 0) {
                        if ((field.getModifiers() & Modifier.FINAL) != 0) {
                            /* we ignore final objects */
                            continue;
                        }
                        if (field.getType().isPrimitive()) {
                            System.out.println("Class " + getClassname() + " has static primitive field: " + field.getName());
                            continue;
                        }
                        if (immutableClasses.contains(field.getType().getName())) {
                            System.out.println("Class " + getClassname() + " has static immutable field: " + field.getName());
                            continue;
                        }
                        /* we only share static objects */
                        field.setAccessible(true);
                        currentSharedObjects.put(field.getName(), field.get(null));
                    }
                }
                if (currentSharedObjects.size() > 0) {
                    /* this plugin has shared Objects */
                    SharedPluginObjects savedSharedObjects = sharedPluginObjectsPool.get(classname);
                    if (savedSharedObjects == null) {
                        /* we dont have shared Objects from this plugin in pool yet */
                        savedSharedObjects = new SharedPluginObjects();
                        savedSharedObjects.version = getVersion();
                        savedSharedObjects.sharedObjects = currentSharedObjects;
                        sharedPluginObjectsPool.put(classname, savedSharedObjects);
                    } else {
                        /* reuse/update objects from pool */
                        Iterator<Entry<String, Object>> it = currentSharedObjects.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, Object> next = it.next();
                            Object savedObject = savedSharedObjects.sharedObjects.get(next.getKey());
                            if (savedObject != null) {
                                Field setField = null;
                                try {
                                    /* restore shared Object */
                                    setField = ret.getClass().getDeclaredField(next.getKey());
                                    setField.setAccessible(true);
                                    if ((setField.getModifiers() & Modifier.FINAL) != 0) {
                                        /* field seems to be final now, dont change it */
                                        if (getVersion() > savedSharedObjects.version) {
                                            /* remove from pool */
                                            System.out.println("Class " + getClassname() + " has final Field " + next.getKey() + " now->remove from pool");
                                            savedSharedObjects.sharedObjects.remove(next.getKey());
                                        }
                                        continue;
                                    }
                                    setField.set(null, savedObject);
                                } catch (final NoSuchFieldException e) {
                                    if (getVersion() > savedSharedObjects.version) {
                                        /* field is gone, remove it from pool */
                                        System.out.println("Class " + getClassname() + " no longer has Field " + next.getKey() + "->remove from pool");
                                        savedSharedObjects.sharedObjects.remove(next.getKey());
                                    }
                                } catch (final Throwable e) {
                                    System.out.println("Cant modify Field " + setField.getName() + " for " + getClassname());
                                    e.printStackTrace();
                                }
                            } else {
                                /* save object to pool */
                                savedSharedObjects.sharedObjects.put(next.getKey(), savedObject);
                            }
                        }
                    }
                } else {
                    /* this plugin no longer has shared Objects, update pool */
                    SharedPluginObjects savedSharedObjects = sharedPluginObjectsPool.get(classname);
                    if (savedSharedObjects != null && savedSharedObjects.version < this.getVersion()) {
                        /* saved objects are from older plugin version, lets remove it from pool */
                        sharedPluginObjectsPool.remove(classname);
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return ret;
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
