package org.jdownloader.plugins.controller.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

    public static boolean isCacheInvalidated() {
        return CACHE_INVALIDATED.get();
    }

    public static void invalidateCache() {
        CACHE_INVALIDATED.set(true);
    }

    protected static void validateCache() {
        CACHE_INVALIDATED.set(false);
        FileCreationManager.getInstance().delete(Application.getResource(HostPluginController.TMP_INVALIDPLUGINS), null);
    }

    /**
     * get the only existing instance of HostPluginController. This is a singleton
     * 
     * @return
     */
    public static CrawlerPluginController getInstance() {
        CrawlerPluginController ret = null;
        MinTimeWeakReference<CrawlerPluginController> localInstance = INSTANCE;
        if (localInstance != null && (ret = localInstance.get()) != null) return ret;
        synchronized (INSTANCELOCK) {
            localInstance = INSTANCE;
            if (localInstance != null && (ret = localInstance.get()) != null) return ret;
            ret = new CrawlerPluginController();
            INSTANCE = new MinTimeWeakReference<CrawlerPluginController>(ret, 30 * 1000l, "CrawlerPlugin");
        }
        return ret;
    }

    private volatile List<LazyCrawlerPlugin> list;

    private String getCache() {
        return "tmp/crawler.ejson";
    }

    /**
     * Create a new instance of HostPluginController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     * 
     */
    private CrawlerPluginController() {
        list = null;
    }

    public synchronized List<LazyCrawlerPlugin> init(boolean noCache) {
        synchronized (INSTANCELOCK) {
            List<LazyCrawlerPlugin> plugins = new ArrayList<LazyCrawlerPlugin>();
            final long t = System.currentTimeMillis();
            LogSource logger = LogController.CL(false);
            logger.info("CrawlerPluginController: init " + noCache);
            logger.setAllowTimeoutFlush(false);
            LogController.setRebirthLogger(logger);
            ClassLoader oldClassLoader = null;
            try {
                oldClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    if (noCache) {
                        /* try to load from cache, to speed up rescan */
                        HashMap<String, ArrayList<LazyPlugin>> rescanCache = new HashMap<String, ArrayList<LazyPlugin>>();
                        try {
                            List<LazyCrawlerPlugin> cachedPlugins = null;
                            if (this.list != null) {
                                cachedPlugins = this.list;
                            } else {
                                cachedPlugins = loadFromCache();
                            }
                            if (cachedPlugins != null) {
                                for (LazyCrawlerPlugin plugin : cachedPlugins) {
                                    if (plugin.getMainClassFilename() == null) continue;
                                    if (plugin.getMainClassLastModified() <= 0) continue;
                                    if (plugin.getMainClassSHA256() == null) continue;
                                    ArrayList<LazyPlugin> cache = rescanCache.get(plugin.getMainClassFilename());
                                    if (cache == null) {
                                        cache = new ArrayList<LazyPlugin>();
                                        rescanCache.put(plugin.getMainClassFilename(), cache);
                                    }
                                    cache.add(plugin);
                                }
                            }
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                        try {
                            /* do a fresh scan */
                            plugins = update(logger, rescanCache);
                        } catch (Throwable e) {
                            logger.severe("@CrawlerPluginController: update failed!");
                            logger.log(e);
                        }
                    } else {
                        /* try to load from cache */
                        try {
                            plugins = loadFromCache();
                        } catch (Throwable e) {
                            logger.severe("@CrawlerPluginController: cache failed!");
                            logger.log(e);
                        }
                        if (plugins.size() == 0) {
                            try {
                                /* do a fresh scan */
                                plugins = update(logger, null);
                            } catch (Throwable e) {
                                logger.severe("@CrawlerPluginController: update failed!");
                                logger.log(e);
                            }
                        }
                    }
                } finally {
                    logger.info("@CrawlerPluginController: init " + (System.currentTimeMillis() - t) + " :" + plugins.size());
                }
                if (plugins.size() == 0) {
                    logger.severe("@CrawlerPluginController: WTF, no plugins!");
                } else {
                    logger.clear();
                }
            } finally {
                logger.close();
                LogController.setRebirthLogger(null);
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            Comparator<LazyCrawlerPlugin> comp = new Comparator<LazyCrawlerPlugin>() {

                @Override
                public int compare(LazyCrawlerPlugin o1, LazyCrawlerPlugin o2) {
                    if (o1.getInterfaceVersion() == o2.getInterfaceVersion()) return 0;
                    if (o1.getInterfaceVersion() > o2.getInterfaceVersion()) return -1;
                    return 1;
                }
            };
            Collections.sort(plugins, comp);
            for (LazyCrawlerPlugin plugin : plugins) {
                plugin.setPluginClass(null);
                plugin.setClassLoader(null);
            }
            list = plugins;

            System.gc();
            return plugins;
        }
    }

    private List<LazyCrawlerPlugin> loadFromCache() {
        List<AbstractCrawlerPlugin> l = JSonStorage.restoreFrom(Application.getResource(getCache()), false, KEY, new TypeRef<ArrayList<AbstractCrawlerPlugin>>() {
        }, new ArrayList<AbstractCrawlerPlugin>());
        List<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>(l.size());
        /* use this classLoader for all cached plugins to load */
        for (AbstractCrawlerPlugin ap : l) {
            if (ap.getCacheVersion() != AbstractCrawlerPlugin.CACHEVERSION) throw new WTFException("Invalid CacheVersion found");
            ret.add(new LazyCrawlerPlugin(ap, null, null));
        }
        return ret;
    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private List<LazyCrawlerPlugin> update(LogSource logger, HashMap<String, ArrayList<LazyPlugin>> pluginCache) throws MalformedURLException {
        ArrayList<AbstractCrawlerPlugin> ret = new ArrayList<AbstractCrawlerPlugin>();
        HashMap<String, ArrayList<LazyCrawlerPlugin>> ret2 = new HashMap<String, ArrayList<LazyCrawlerPlugin>>();
        for (PluginInfo<PluginForDecrypt> c : scan("jd/plugins/decrypter", pluginCache)) {
            if (c.getLazyPlugin() != null) {
                LazyCrawlerPlugin plugin = (LazyCrawlerPlugin) c.getLazyPlugin();
                ret.add(plugin.getAbstractCrawlerPlugin());
                ArrayList<LazyCrawlerPlugin> existingLazyPlugin = ret2.get(plugin.getDisplayName());
                if (existingLazyPlugin == null) {
                    existingLazyPlugin = new ArrayList<LazyCrawlerPlugin>();
                    ret2.put(plugin.getDisplayName(), existingLazyPlugin);
                }
                existingLazyPlugin.add(plugin);
                logger.finer("@CrawlerPlugin ok(cached):" + plugin.getClassname() + " " + plugin.getDisplayName() + " " + plugin.getVersion());
                continue;
            }
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
                    ClassLoader oldCL = null;
                    try {
                        PluginClassLoaderChild classLoader = (PluginClassLoaderChild) c.getClazz().getClassLoader();
                        /* during init we dont want dummy libs being created */
                        classLoader.setCreateDummyLibs(false);
                        Thread.currentThread().setContextClassLoader(classLoader);
                        for (int i = 0; i < names.length; i++) {
                            LazyCrawlerPlugin l = null;
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
                                l = new LazyCrawlerPlugin(ap, null, classLoader);
                                ret.add(ap);
                                try {
                                    /* check for stable compatibility */
                                    classLoader.setPluginClass(new String(c.getClazz().getName()));
                                    classLoader.setCheckStableCompatibility(a.interfaceVersion() == 2);
                                    PluginForDecrypt plg = l.newInstance(classLoader);
                                    Class<? extends ConfigInterface> configInterface = plg.getConfigInterface();
                                    if (configInterface != null) {
                                        String name = new String(configInterface.getName());
                                        ap.setConfigInterface(name);
                                        l.setConfigInterface(name);
                                    }
                                    ap.setHasConfig(plg.hasConfig());
                                    ap.setMaxConcurrentInstances(plg.getMaxConcurrentProcessingInstances());
                                    l.setMaxConcurrentInstances(ap.getMaxConcurrentInstances());
                                    l.setHasConfig(plg.hasConfig());
                                } catch (UpdateRequiredClassNotFoundException e) {
                                    logger.finest("@CrawlerPlugin incomplete:" + simpleName + " " + new String(names[i]) + " " + e.getMessage() + " " + revision);
                                    throw e;
                                }
                                ArrayList<LazyCrawlerPlugin> existingLazyPlugin = ret2.get(displayName);
                                if (existingLazyPlugin == null) {
                                    existingLazyPlugin = new ArrayList<LazyCrawlerPlugin>();
                                    ret2.put(displayName, existingLazyPlugin);
                                }
                                existingLazyPlugin.add(l);
                                if (existingLazyPlugin.size() > 1) {
                                    logger.finest("@CrawlerPlugin multiple crawler:" + displayName + "->" + simpleName + " " + revision);
                                }
                                logger.finest("@CrawlerPlugin ok:" + simpleName + " " + new String(names[i]) + " " + revision);
                            } catch (Throwable e) {
                                logger.severe("@CrawlerPlugin failed:" + simpleName + " " + new String(names[i]) + " " + revision);
                                logger.log(e);
                            } finally {
                                if (l != null) {
                                    l.setPluginClass(null);
                                    l.setClassLoader(null);
                                }
                            }
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldCL);
                    }
                } catch (final Throwable e) {
                    logger.severe("@CrawlerPlugin failed:" + simpleName);
                    logger.log(e);
                }
            } else {
                logger.severe("@CrawlerPlugin missing:" + simpleName);
            }
        }
        save(ret);
        validateCache();
        ArrayList<LazyCrawlerPlugin> retList = new ArrayList<LazyCrawlerPlugin>();
        for (ArrayList<LazyCrawlerPlugin> plugins : ret2.values()) {
            retList.addAll(plugins);
        }
        return retList;
    }

    private void save(List<AbstractCrawlerPlugin> save) {
        JSonStorage.saveTo(Application.getResource(getCache()), false, KEY, JSonStorage.serializeToJson(save));
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
            if (ensureLoaded) ret.list();
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
        if (localList != null && isCacheInvalidated() == false) return localList;
        synchronized (INSTANCELOCK) {
            localList = list;
            if (localList != null && isCacheInvalidated() == false) return localList;
            return init(isCacheInvalidated());
        }
    }

    public LazyCrawlerPlugin get(String displayName) {
        List<LazyCrawlerPlugin> llist = ensureLoaded();
        for (LazyCrawlerPlugin plugin : llist) {
            if (plugin.getDisplayName().equalsIgnoreCase(displayName)) return plugin;
        }
        return null;
    }

    public static void invalidateCacheIfRequired() {
        if (Application.getTempResource(HostPluginController.TMP_INVALIDPLUGINS).exists()) {
            invalidateCache();
        }
    }

}
