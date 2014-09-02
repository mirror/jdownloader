package org.jdownloader.plugins.controller;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class PluginClassLoader extends URLClassLoader {
    private static final WeakHashMap<Class<?>, String>                                                    sharedClasses                = new WeakHashMap<Class<?>, String>();
    private static final WeakHashMap<PluginClassLoaderChild, WeakReference<LazyPlugin<? extends Plugin>>> sharedLazyPluginClassLoader  = new WeakHashMap<PluginClassLoaderChild, WeakReference<LazyPlugin<? extends Plugin>>>();
    private static final WeakHashMap<PluginClassLoaderChild, String>                                      sharedPluginClassLoader      = new WeakHashMap<PluginClassLoaderChild, String>();

    private static final WeakHashMap<Thread, WeakReference<PluginClassLoaderChild>>                       threadPluginClassLoader      = new WeakHashMap<Thread, WeakReference<PluginClassLoaderChild>>();
    private static final WeakHashMap<ThreadGroup, WeakReference<PluginClassLoaderChild>>                  threadGroupPluginClassLoader = new WeakHashMap<ThreadGroup, WeakReference<PluginClassLoaderChild>>();

    public static Class<?> findSharedClass(String name) {
        synchronized (sharedClasses) {
            Iterator<Entry<Class<?>, String>> it = sharedClasses.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Class<?>, String> next = it.next();
                Class<?> ret = null;
                if (name.equals(next.getValue()) && (ret = next.getKey()) != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    public static class PluginClassLoaderChild extends URLClassLoader {

        private boolean                 createDummyLibs          = true;
        private boolean                 jared                    = Application.isJared(PluginClassLoader.class);
        private final PluginClassLoader parent;
        private boolean                 checkStableCompatibility = false;
        private String                  pluginClass              = null;

        public boolean isCheckStableCompatibility() {
            return checkStableCompatibility;
        }

        public void setCheckStableCompatibility(boolean checkStableCompatibility) {
            this.checkStableCompatibility = checkStableCompatibility;
        }

        public PluginClassLoaderChild(PluginClassLoader parent) {
            super(new URL[] { Application.getRootUrlByClass(jd.SecondLevelLaunch.class, null) }, parent);
            this.parent = parent;
        }

        public boolean isUpdateRequired(String name) {
            if (!jared) {
                return false;
            }
            name = name.replace("/", ".");
            synchronized (DYNAMIC_LOADABLE_LOBRARIES) {
                Iterator<Entry<String, String>> it = DYNAMIC_LOADABLE_LOBRARIES.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, String> next = it.next();
                    String dynamicPackage = next.getKey();
                    String libFile = next.getValue();
                    if (name.startsWith(dynamicPackage)) {
                        /* dynamic Library in use */
                        /* check if the library is already available on disk */
                        File lib = Application.getResource("libs/" + libFile);
                        if (lib.exists() && lib.isFile() && lib.length() != 0) {
                            /* file already exists on disk, so we can use it */
                            it.remove();
                            break;
                        } else if (lib.exists() && lib.isFile() && lib.length() == 0) {
                            /* dummy library, we have to wait for update */
                            return true;
                        } else if (!lib.exists()) {
                            /* library file not existing, create a new one if wished, so the update system replaces it with correct one */
                            return true;
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        private Class<?> loadAndDefineClass(final URL myUrl, final String name, final PluginClassLoader parent) throws Exception {
            int tryAgain = 5;
            byte data[] = null;
            while (true) {
                try {
                    data = IO.readURL(myUrl);
                    if (parent != null) {
                        return parent.defineClass(name, data, 0, data.length);
                    } else {
                        return defineClass(name, data, 0, data.length);
                    }
                } catch (ClassFormatError e) {
                    LogSource logger = LogController.getRebirthLogger(LogController.GL);
                    if (logger != null) {
                        if (data != null) {
                            logger.severe("ClassFormatError:class=" + name + "|file=" + myUrl + "|size=" + data.length);
                        } else {
                            logger.severe("ClassFormatError:class=" + name + "|file=" + myUrl);
                        }
                        logger.log(e);
                    }
                    if (--tryAgain == 0) {
                        throw e;
                    } else {
                        Thread.sleep(150);
                    }
                } catch (IOException e) {
                    LogSource logger = LogController.getRebirthLogger(LogController.GL);
                    if (logger != null) {
                        logger.severe("IOException:class=" + name + "|file=" + myUrl);
                        logger.log(e);
                    }
                    if (--tryAgain == 0) {
                        throw e;
                    } else {
                        Thread.sleep(150);
                    }
                }
            }
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            try {
                if (jared) {
                    synchronized (DYNAMIC_LOADABLE_LOBRARIES) {
                        Iterator<Entry<String, String>> it = DYNAMIC_LOADABLE_LOBRARIES.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, String> next = it.next();
                            String dynamicPackage = next.getKey();
                            String libFile = next.getValue();
                            if (name.startsWith(dynamicPackage)) {
                                /* dynamic Library in use */
                                /* check if the library is already available on disk */
                                File lib = Application.getResource("libs/" + libFile);
                                if (lib.exists() && lib.isFile() && lib.length() != 0) {
                                    /* file already exists on disk, so we can use it */
                                    it.remove();
                                    break;
                                } else if (lib.exists() && lib.isFile() && lib.length() == 0) {
                                    /* dummy library, we have to wait for update */
                                    throw new UpdateRequiredClassNotFoundException(libFile);
                                } else if (!lib.exists()) {
                                    /*
                                     * library file not existing, create a new one if wished, so the update system replaces it with correct
                                     * one
                                     */
                                    if (createDummyLibs) {
                                        lib.createNewFile();
                                    }
                                    throw new UpdateRequiredClassNotFoundException(libFile);
                                }
                                throw new ClassNotFoundException(name);
                            }
                        }
                    }
                }
                if (isCheckStableCompatibility() && name.equals(pluginClass) == false && !name.startsWith(pluginClass + "$")) {
                    boolean check = true;
                    if (check) {
                        check = !name.equals("jd.plugins.hoster.RTMPDownload");/* available in 09581 Stable */
                    }
                    if (check) {
                        check = !name.equals("org.appwork.utils.speedmeter.SpeedMeterInterface");/* available in 09581 Stable */
                    }
                    if (check) {
                        check = !name.equals("org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream");/*
                                                                                                                       * available in 09581
                                                                                                                       * Stable
                                                                                                                       */
                    }
                    if (check) {
                        check = !name.equals("org.appwork.utils.net.throttledconnection.ThrottledConnection");/*
                                                                                                               * available in 09581 Stable
                                                                                                               */
                    }
                    if (check) {
                        if (name.startsWith("org.appwork") || name.startsWith("jd.plugins.hoster") || name.startsWith("jd.plugins.decrypter")) {
                            System.out.println("Check for Stable Compatibility!!!: " + getPluginClass() + " wants to load " + name);
                        }
                    }
                }
                if (!name.startsWith("jd.plugins.hoster") && !name.startsWith("jd.plugins.decrypter")) {
                    return super.loadClass(name);
                }
                if (name.startsWith("jd.plugins.hoster.RTMPDownload")) {
                    return super.loadClass(name);
                }
                Class<?> c = null;
                boolean sharedClass = name.endsWith("StringContainer");
                if (sharedClass) {
                    c = findSharedClass(name);
                } else {
                    c = findLoadedClass(name);
                }
                if (c != null) {
                    // System.out.println("Class has already been loaded by this PluginClassLoaderChild");
                    return c;
                }
                synchronized (this) {
                    /*
                     * we have to synchronize this because concurrent defineClass for same class throws exception
                     */
                    if (sharedClass) {
                        c = findSharedClass(name);
                    } else {
                        c = findLoadedClass(name);
                    }
                    if (c != null) {
                        return c;
                    }
                    URL myUrl = Application.getRessourceURL(name.replace(".", "/") + ".class");
                    if (myUrl == null) {
                        throw new ClassNotFoundException("Class does not exist(anymore): " + name);
                    }
                    if (sharedClass) {
                        synchronized (sharedClasses) {
                            c = findSharedClass(name);
                            if (c == null) {
                                c = loadAndDefineClass(myUrl, name, parent);
                                sharedClasses.put(c, name);
                            }
                        }
                    } else {
                        c = loadAndDefineClass(myUrl, name, null);
                    }
                    return c;
                }
            } catch (Exception e) {
                LogSource logger = LogController.getRebirthLogger(LogController.GL);
                if (logger != null) {
                    logger.log(e);
                }
                if (e instanceof UpdateRequiredClassNotFoundException) {
                    throw (UpdateRequiredClassNotFoundException) e;
                }
                if (e instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) e;
                }
                throw new ClassNotFoundException(name, e);
            }

        }

        /**
         * @return the createDummyLibs
         */
        public boolean isCreateDummyLibs() {
            return createDummyLibs;
        }

        /**
         * @param createDummyLibs
         *            the createDummyLibs to set
         */
        public void setCreateDummyLibs(boolean createDummyLibs) {
            this.createDummyLibs = createDummyLibs;
        }

        /**
         * @return the pluginClass
         */
        public String getPluginClass() {
            return pluginClass;
        }

        /**
         * @param pluginClass
         *            the pluginClass to set
         */
        public void setPluginClass(String pluginClass) {
            this.pluginClass = pluginClass;
        }
    }

    private static final PluginClassLoader       INSTANCE                   = new PluginClassLoader();
    private static final HashMap<String, String> DYNAMIC_LOADABLE_LOBRARIES = new HashMap<String, String>();
    static {
        synchronized (DYNAMIC_LOADABLE_LOBRARIES) {
            DYNAMIC_LOADABLE_LOBRARIES.put("org.bouncycastle", "bcprov-jdk15on-147.jar");
        }
    }

    public static PluginClassLoader getInstance() {
        return INSTANCE;
    }

    private PluginClassLoader() {
        super(new URL[] { Application.getRootUrlByClass(jd.SecondLevelLaunch.class, null) }, PluginClassLoader.class.getClassLoader());

    }

    public PluginClassLoaderChild getChild() {
        return new PluginClassLoaderChild(this);
    }

    public synchronized static PluginClassLoaderChild getSharedChild(LazyPlugin<? extends Plugin> lazyPlugin) {
        if (lazyPlugin == null) {
            return PluginClassLoader.getInstance().getChild();
        }
        PluginClassLoaderChild ret = fetchSharedChild(lazyPlugin, null);
        if (ret == null) {
            ret = PluginClassLoader.getInstance().getChild();
            return fetchSharedChild(lazyPlugin, ret);
        }
        return ret;
    }

    public synchronized static PluginClassLoaderChild getSharedChild(Plugin plugin) {
        if (plugin != null) {
            PluginClassLoaderChild ret = null;
            if (plugin instanceof PluginForHost) {
                ret = fetchSharedChild(((PluginForHost) plugin).getLazyP(), null);
            } else if (plugin instanceof PluginForDecrypt) {
                ret = fetchSharedChild(((PluginForDecrypt) plugin).getLazyC(), null);
            }
            if (ret == null) {
                ret = fetchSharedChild(plugin, null);
                if (ret == null) {
                    ret = fetchSharedChild(plugin, PluginClassLoader.getInstance().getChild());
                }
            }
            return ret;
        }
        return null;
    }

    private static String getCacheID(final Plugin plugin) {
        if (plugin instanceof PluginForHost) {
            final LazyHostPlugin lazyP = ((PluginForHost) plugin).getLazyP();
            return lazyP.getClassName() + lazyP.getVersion() + HexFormatter.byteArrayToHex(lazyP.getLazyPluginClass().getSha256());
        } else if (plugin instanceof PluginForDecrypt) {
            final LazyCrawlerPlugin lazyC = ((PluginForDecrypt) plugin).getLazyC();
            return lazyC.getClassName() + lazyC.getVersion() + HexFormatter.byteArrayToHex(lazyC.getLazyPluginClass().getSha256());
        }
        return null;
    }

    private synchronized static PluginClassLoaderChild fetchSharedChild(final Plugin plugin, final PluginClassLoaderChild putIfAbsent) {
        final Iterator<Entry<PluginClassLoaderChild, String>> it = sharedPluginClassLoader.entrySet().iterator();
        final String cacheID = getCacheID(plugin);
        while (it.hasNext()) {
            final Entry<PluginClassLoaderChild, String> next = it.next();
            final String ID = next.getValue();
            if (ID != null && ID.equals(cacheID)) {
                final PluginClassLoaderChild ret = next.getKey();
                if (ret != null) {
                    return ret;
                }
                break;
            }
        }
        if (putIfAbsent != null) {
            sharedPluginClassLoader.put(putIfAbsent, cacheID);
            return putIfAbsent;
        }
        return null;
    }

    private synchronized static PluginClassLoaderChild fetchSharedChild(final LazyPlugin<? extends Plugin> lazyPlugin, final PluginClassLoaderChild putIfAbsent) {
        final Iterator<Entry<PluginClassLoaderChild, WeakReference<LazyPlugin<? extends Plugin>>>> it = sharedLazyPluginClassLoader.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<PluginClassLoaderChild, WeakReference<LazyPlugin<? extends Plugin>>> next = it.next();
            final WeakReference<LazyPlugin<? extends Plugin>> weakPlugin = next.getValue();
            if (weakPlugin != null) {
                final LazyPlugin<? extends Plugin> plugin = weakPlugin.get();
                if (plugin != null && (lazyPlugin == plugin || lazyPlugin.getClassName().equals(plugin.getClassName()) && lazyPlugin.getVersion() == plugin.getVersion() && Arrays.equals(lazyPlugin.getLazyPluginClass().getSha256(), plugin.getLazyPluginClass().getSha256()))) {
                    final PluginClassLoaderChild ret = next.getKey();
                    if (ret != null) {
                        return ret;
                    }
                    break;
                }
            }
        }
        if (putIfAbsent != null) {
            sharedLazyPluginClassLoader.put(putIfAbsent, new WeakReference<LazyPlugin<? extends Plugin>>(lazyPlugin));
            return putIfAbsent;
        }
        return null;
    }

    public synchronized static PluginClassLoaderChild getThreadPluginClassLoaderChild() {
        final Thread currentThread = Thread.currentThread();
        threadPluginClassLoader.isEmpty();
        threadGroupPluginClassLoader.isEmpty();
        WeakReference<PluginClassLoaderChild> wcl = threadPluginClassLoader.get(currentThread);
        PluginClassLoaderChild cl = null;
        if (wcl != null && (cl = wcl.get()) != null) {
            return cl;
        }
        ThreadGroup threadGroup = currentThread.getThreadGroup();
        while (threadGroup != null) {
            wcl = threadGroupPluginClassLoader.get(threadGroup);
            if (wcl != null && (cl = wcl.get()) != null) {
                return cl;
            }
            threadGroup = threadGroup.getParent();
        }
        return null;
    }

    public synchronized static void setThreadPluginClassLoaderChild(PluginClassLoaderChild threadChild, PluginClassLoaderChild groupChild) {
        final Thread currentThread = Thread.currentThread();
        final ThreadGroup threadGroup = currentThread.getThreadGroup();
        threadPluginClassLoader.isEmpty();
        threadGroupPluginClassLoader.isEmpty();
        if (threadChild == null) {
            threadPluginClassLoader.remove(currentThread);
        } else {
            threadPluginClassLoader.put(currentThread, new WeakReference<PluginClassLoaderChild>(threadChild));
        }
        if (threadGroup != null) {
            if (groupChild == null) {
                threadGroupPluginClassLoader.remove(threadGroup);
            } else {
                threadGroupPluginClassLoader.put(threadGroup, new WeakReference<PluginClassLoaderChild>(groupChild));
            }
        }

    }
}
