package org.jdownloader.plugins.controller.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.nutils.Formatter;
import jd.plugins.DecrypterPlugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.Application;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class CrawlerPluginController extends PluginController<PluginForDecrypt> {

    private static final Object                                           INSTANCELOCK      = new Object();
    private static volatile MinTimeWeakReference<CrawlerPluginController> INSTANCE          = null;
    private static final AtomicBoolean                                    CACHE_INVALIDATED = new AtomicBoolean(false);
    private final ModifyLock                                              lock              = new ModifyLock();

    public static boolean isCacheInvalidated() {
        return CACHE_INVALIDATED.get();
    }

    public static void invalidateCache() {
        CACHE_INVALIDATED.set(true);
    }

    protected static void validateCache() {
        CACHE_INVALIDATED.set(false);
    }

    /**
     * get the only existing instance of HostPluginController. This is a singleton
     * 
     * @return
     */
    public static CrawlerPluginController getInstance() {
        CrawlerPluginController ret = null;
        MinTimeWeakReference<CrawlerPluginController> localInstance = INSTANCE;
        if (localInstance != null && (ret = localInstance.get()) != null) {
            return ret;
        }
        synchronized (INSTANCELOCK) {
            localInstance = INSTANCE;
            if (localInstance != null && (ret = localInstance.get()) != null) {
                return ret;
            }
            ret = new CrawlerPluginController();
            INSTANCE = new MinTimeWeakReference<CrawlerPluginController>(ret, 30 * 1000l, "CrawlerPlugin");
        }
        return ret;
    }

    private volatile List<LazyCrawlerPlugin> list;

    private String getCache() {
        return "crawler2.ejson";
    }

    /**
     * Create a new instance of HostPluginController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     * 
     */
    private CrawlerPluginController() {
        list = null;
    }

    public synchronized List<LazyCrawlerPlugin> init() {
        synchronized (INSTANCELOCK) {
            final LogSource logger = LogController.CL(false);
            logger.info("CrawlerPluginController: init");
            logger.setAllowTimeoutFlush(false);
            logger.setAutoFlushOnThrowable(true);
            LogController.setRebirthLogger(logger);
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            final long completeTimeStamp = System.currentTimeMillis();
            try {
                List<LazyCrawlerPlugin> updateCache = null;
                /* try to load from cache */
                long timeStamp = System.currentTimeMillis();
                try {
                    updateCache = loadFromCache();
                } catch (Throwable e) {
                    logger.log(e);
                    logger.severe("@CrawlerPluginController: cache failed!");
                } finally {
                    if (updateCache != null && updateCache.size() > 0) {
                        logger.info("@CrawlerPluginController: loadFromCache took " + (System.currentTimeMillis() - timeStamp) + "ms for " + updateCache.size());
                    }
                }
                List<LazyCrawlerPlugin> plugins = null;
                timeStamp = System.currentTimeMillis();
                try {
                    /* do a fresh scan */
                    plugins = update(logger, updateCache);
                } catch (Throwable e) {
                    logger.log(e);
                    logger.severe("@CrawlerPluginController: update failed!");
                } finally {
                    if (plugins != null && plugins.size() > 0) {
                        logger.info("@CrawlerPluginController: update took " + (System.currentTimeMillis() - timeStamp) + "ms for " + plugins.size());
                    }
                }
                if (plugins == null || plugins.size() == 0) {
                    if (plugins == null) {
                        plugins = new ArrayList<LazyCrawlerPlugin>();
                    }
                    logger.severe("@CrawlerPluginController: WTF, no plugins!");
                }
                timeStamp = System.currentTimeMillis();
                try {
                    final Comparator<LazyCrawlerPlugin> comp = new Comparator<LazyCrawlerPlugin>() {

                        @Override
                        public int compare(LazyCrawlerPlugin o1, LazyCrawlerPlugin o2) {
                            if (o1.getInterfaceVersion() == o2.getInterfaceVersion()) {
                                return 0;
                            }
                            if (o1.getInterfaceVersion() > o2.getInterfaceVersion()) {
                                return -1;
                            }
                            return 1;
                        }
                    };
                    Collections.sort(plugins, comp);
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.severe("@CrawlerPluginController: sort failed!");
                } finally {
                    logger.info("@CrawlerPluginController: sort took " + (System.currentTimeMillis() - timeStamp) + "ms for " + plugins.size());
                }
                final List<LazyCrawlerPlugin> finalPlugins = plugins;
                Thread saveThread = new Thread("@CrawlerPluginController:save") {
                    public void run() {
                        final ArrayList<AbstractCrawlerPlugin> saveList = new ArrayList<AbstractCrawlerPlugin>(finalPlugins.size());
                        for (LazyCrawlerPlugin plugin : finalPlugins) {
                            saveList.add(plugin.getAbstractCrawlerPlugin());
                        }
                        save(saveList);
                    };
                };
                saveThread.setDaemon(true);
                saveThread.start();
                for (LazyCrawlerPlugin plugin : plugins) {
                    plugin.setPluginClass(null);
                    plugin.setClassLoader(null);
                }
                list = Collections.unmodifiableList(plugins);
            } finally {
                validateCache();
                LogController.setRebirthLogger(null);
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
                final List<LazyCrawlerPlugin> llist = list;
                if (llist != null) {
                    logger.info("@CrawlerPluginController: init took " + (System.currentTimeMillis() - completeTimeStamp) + "ms for " + llist.size());
                } else {
                    logger.info("@CrawlerPluginController: init took " + (System.currentTimeMillis() - completeTimeStamp));
                }
                logger.close();
            }
            System.gc();
            return list;
        }
    }

    private List<LazyCrawlerPlugin> loadFromCache() {
        final List<AbstractCrawlerPlugin> list;
        boolean readL = lock.readLock();
        try {
            list = JSonStorage.restoreFrom(Application.getTempResource(getCache()), false, KEY, new TypeRef<ArrayList<AbstractCrawlerPlugin>>() {
            }, null);
        } finally {
            lock.readUnlock(readL);
        }
        if (list == null || list.size() == 0) {
            return null;
        }
        final List<LazyCrawlerPlugin> retList = new ArrayList<LazyCrawlerPlugin>(list.size());
        /* use this classLoader for all cached plugins to load */
        for (final AbstractCrawlerPlugin ap : list) {
            if (ap.getCacheVersion() != AbstractCrawlerPlugin.CACHEVERSION) {
                throw new WTFException("Invalid CacheVersion found");
            }
            retList.add(new LazyCrawlerPlugin(ap, null, null));
        }
        return retList;
    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private List<LazyCrawlerPlugin> update(final LogSource logger, final List<LazyCrawlerPlugin> updateCache) throws MalformedURLException {
        final Map<String, ArrayList<LazyPlugin>> updateCacheMap;
        if (updateCache != null && updateCache.size() > 0) {
            updateCacheMap = new HashMap<String, ArrayList<LazyPlugin>>();
            try {
                for (final LazyCrawlerPlugin cachedPlugin : updateCache) {
                    final String classFilename = cachedPlugin.getMainClassFilename();
                    if (classFilename != null && cachedPlugin.getMainClassLastModified() > 0 && cachedPlugin.getMainClassSHA256() != null) {
                        ArrayList<LazyPlugin> cachedPlugins = updateCacheMap.get(classFilename);
                        if (cachedPlugins == null) {
                            cachedPlugins = new ArrayList<LazyPlugin>();
                            updateCacheMap.put(classFilename, cachedPlugins);
                        }
                        cachedPlugins.add(cachedPlugin);
                    }
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        } else {
            updateCacheMap = null;
        }
        final ArrayList<LazyCrawlerPlugin> retList = new ArrayList<LazyCrawlerPlugin>();
        for (PluginInfo<PluginForDecrypt> c : scan("jd/plugins/decrypter", updateCacheMap)) {
            if (c.getLazyPlugin() != null) {
                final LazyCrawlerPlugin plugin = (LazyCrawlerPlugin) c.getLazyPlugin();
                retList.add(plugin);
                logger.finer("@CrawlerPlugin ok(cached):" + plugin.getClassname() + " " + plugin.getDisplayName() + " " + plugin.getVersion());
            } else {
                String simpleName = new String(c.getClazz().getSimpleName());
                DecrypterPlugin a = c.getClazz().getAnnotation(DecrypterPlugin.class);
                if (a != null) {
                    try {
                        long revision = Formatter.getRevision(a.revision());
                        String[] names = a.names();
                        String[] patterns = a.urls();
                        int[] flags = a.flags();
                        if (names.length == 0) {
                            /* create multiple crawler plugins from one source */
                            patterns = (String[]) c.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                            names = (String[]) c.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});
                            flags = (int[]) c.getClazz().getDeclaredMethod("getAnnotationFlags", new Class[] {}).invoke(null, new Object[] {});
                        }
                        if (patterns.length != names.length) {
                            //
                            throw new WTFException("names.length != patterns.length");
                        }
                        if (flags.length != names.length && a.interfaceVersion() == 2) {
                            /* interfaceVersion 2 is for Stable/Nightly */
                            logger.log(new WTFException("PLUGIN STABLE ISSUE!! names.length(" + names.length + ")!= flags.length(" + flags.length + ")->" + simpleName));
                        }
                        if (names.length == 0) {
                            //
                            throw new WTFException("names.length=0");
                        }
                        final PluginClassLoaderChild classLoader = (PluginClassLoaderChild) c.getClazz().getClassLoader();
                        /* during init we dont want dummy libs being created */
                        classLoader.setCreateDummyLibs(false);
                        Thread.currentThread().setContextClassLoader(classLoader);
                        for (int i = 0; i < names.length; i++) {
                            LazyCrawlerPlugin lazyCrawlerPlugin = null;
                            try {
                                String displayName = new String(names[i]);
                                /* we use new String() here to dereference the Annotation and it's loaded class */
                                AbstractCrawlerPlugin ap = new AbstractCrawlerPlugin(new String(c.getClazz().getSimpleName()));
                                ap.setCacheVersion(AbstractCrawlerPlugin.CACHEVERSION);
                                ap.setDisplayName(displayName);
                                ap.setPattern(new String(patterns[i]));
                                ap.setVersion(revision);
                                ap.setInterfaceVersion(a.interfaceVersion());
                                /* information to speed up rescan */
                                ap.setMainClassSHA256(c.getMainClassSHA256());
                                ap.setMainClassLastModified(c.getMainClassLastModified());
                                ap.setMainClassFilename(c.getFile().getName());
                                lazyCrawlerPlugin = new LazyCrawlerPlugin(ap, null, classLoader);
                                try {
                                    /* check for stable compatibility */
                                    classLoader.setPluginClass(new String(c.getClazz().getName()));
                                    classLoader.setCheckStableCompatibility(a.interfaceVersion() == 2);
                                    PluginForDecrypt plg = lazyCrawlerPlugin.newInstance(classLoader);
                                    Class<? extends ConfigInterface> configInterface = plg.getConfigInterface();
                                    if (configInterface != null) {
                                        String name = new String(configInterface.getName());
                                        ap.setConfigInterface(name);
                                        lazyCrawlerPlugin.setConfigInterface(name);
                                    }
                                    ap.setHasConfig(plg.hasConfig());
                                    ap.setMaxConcurrentInstances(plg.getMaxConcurrentProcessingInstances());
                                    lazyCrawlerPlugin.setMaxConcurrentInstances(ap.getMaxConcurrentInstances());
                                    lazyCrawlerPlugin.setHasConfig(plg.hasConfig());
                                    retList.add(lazyCrawlerPlugin);
                                } catch (UpdateRequiredClassNotFoundException e) {
                                    logger.finest("@CrawlerPlugin incomplete:" + simpleName + " " + new String(names[i]) + " " + e.getMessage() + " " + revision);
                                    throw e;
                                }
                                logger.finest("@CrawlerPlugin ok:" + simpleName + " " + new String(names[i]) + " " + revision);
                            } catch (Throwable e) {
                                logger.log(e);
                                logger.severe("@CrawlerPlugin failed:" + simpleName + " " + new String(names[i]) + " " + revision);
                            } finally {
                                if (lazyCrawlerPlugin != null) {
                                    lazyCrawlerPlugin.setPluginClass(null);
                                    lazyCrawlerPlugin.setClassLoader(null);
                                }
                            }
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.severe("@CrawlerPlugin failed:" + simpleName);
                    }
                } else {
                    logger.severe("@CrawlerPlugin missing:" + simpleName);
                }
            }
        }
        return retList;
    }

    private void save(List<AbstractCrawlerPlugin> save) {
        lock.writeLock();
        try {
            JSonStorage.saveTo(Application.getTempResource(getCache()), false, KEY, JSonStorage.serializeToJson(save));
        } finally {
            lock.writeUnlock();
            FileCreationManager.getInstance().delete(Application.getResource(HostPluginController.TMP_INVALIDPLUGINS), null);
        }
    }

    public List<LazyCrawlerPlugin> list() {
        return ensureLoaded();
    }

    /*
     * returns the list of available plugins
     * 
     * can return null if controller is not initiated yet and ensureLoaded is false
     */
    public static List<LazyCrawlerPlugin> list(boolean ensureLoaded) {
        CrawlerPluginController ret = null;
        MinTimeWeakReference<CrawlerPluginController> localInstance = INSTANCE;
        if (localInstance != null && (ret = localInstance.get()) != null) {
            /* Controller is initiated */
            if (ensureLoaded) {
                ret.list();
            }
            return ret.list;
        } else {
            /* Controller is not initiated */
            if (ensureLoaded) {
                /* init the controller */
                return getInstance().list;
            } else {
                /* return null */
                return null;
            }
        }
    }

    public List<LazyCrawlerPlugin> ensureLoaded() {
        List<LazyCrawlerPlugin> localList = list;
        if (localList != null && isCacheInvalidated() == false) {
            return localList;
        }
        synchronized (INSTANCELOCK) {
            localList = list;
            if (localList != null && isCacheInvalidated() == false) {
                return localList;
            }
            return init();
        }
    }

    public LazyCrawlerPlugin get(String displayName) {
        final List<LazyCrawlerPlugin> llist = ensureLoaded();
        for (LazyCrawlerPlugin plugin : llist) {
            if (plugin.getDisplayName().equalsIgnoreCase(displayName)) {
                return plugin;
            }
        }
        return null;
    }

    public List<LazyCrawlerPlugin> getAll(String displayName) {
        final ArrayList<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>();
        final List<LazyCrawlerPlugin> llist = ensureLoaded();
        for (LazyCrawlerPlugin plugin : llist) {
            if (plugin.getDisplayName().equalsIgnoreCase(displayName)) {
                ret.add(plugin);
            }
        }
        return ret;
    }

    public static void invalidateCacheIfRequired() {
        if (Application.getTempResource(HostPluginController.TMP_INVALIDPLUGINS).exists()) {
            invalidateCache();
        }
    }

}
