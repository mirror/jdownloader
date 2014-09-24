package org.jdownloader.plugins.controller.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.nutils.Formatter;
import jd.plugins.DecrypterPlugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.Application;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPluginClass;
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
        return "crawlerCache";
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
            List<LazyCrawlerPlugin> finalPlugins = null;
            final long completeTimeStamp = System.currentTimeMillis();
            final AtomicLong lastModification = new AtomicLong(-1l);
            try {
                List<LazyCrawlerPlugin> updateCache = null;
                /* try to load from cache */
                long timeStamp = System.currentTimeMillis();
                try {
                    updateCache = loadFromCache(lastModification);
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
                    plugins = update(logger, updateCache, lastModification);
                } catch (Throwable e) {
                    if (lastModification != null) {
                        lastModification.set(-1l);
                    }
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
                            final LazyPluginClass l1 = o1.getLazyPluginClass();
                            final LazyPluginClass l2 = o2.getLazyPluginClass();
                            if (l1.getInterfaceVersion() == l2.getInterfaceVersion()) {
                                return 0;
                            }
                            if (l1.getInterfaceVersion() > l2.getInterfaceVersion()) {
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
                finalPlugins = plugins;
                for (LazyCrawlerPlugin plugin : plugins) {
                    plugin.setPluginClass(null);
                    plugin.setClassLoader(null);
                }
                list = plugins;
            } finally {
                validateCache();
                LogController.setRebirthLogger(null);
                final List<LazyCrawlerPlugin> llist = list;
                if (llist != null) {
                    logger.info("@CrawlerPluginController: init took " + (System.currentTimeMillis() - completeTimeStamp) + "ms for " + llist.size());
                } else {
                    logger.info("@CrawlerPluginController: init took " + (System.currentTimeMillis() - completeTimeStamp));
                }
                logger.close();
                if (finalPlugins != null) {
                    final List<LazyCrawlerPlugin> plugins = finalPlugins;
                    Thread saveThread = new Thread("@CrawlerPluginController:save") {
                        public void run() {
                            save(plugins, lastModification);
                        };
                    };
                    saveThread.setDaemon(true);
                    saveThread.start();
                }
            }
            System.gc();
            return list;
        }
    }

    private List<LazyCrawlerPlugin> loadFromCache(final AtomicLong lastFolderModification) throws IOException {
        final boolean readL = lock.readLock();
        try {
            return LazyCrawlerPluginCache.read(Application.getTempResource(getCache()), lastFolderModification);
        } finally {
            lock.readUnlock(readL);
        }
    }

    private List<LazyCrawlerPlugin> update(final LogSource logger, final List<LazyCrawlerPlugin> updateCache, final AtomicLong lastFolderModification) throws Exception {
        final ArrayList<LazyCrawlerPlugin> retList = new ArrayList<LazyCrawlerPlugin>();
        for (final PluginInfo<PluginForDecrypt> pluginInfo : scan(logger, "jd/plugins/decrypter", updateCache, lastFolderModification)) {
            if (pluginInfo.getLazyPlugin() != null) {
                final LazyCrawlerPlugin plugin = (LazyCrawlerPlugin) pluginInfo.getLazyPlugin();
                retList.add(plugin);
                // logger.finer("@CrawlerPlugin ok(cached):" + plugin.getClassName() + " " + plugin.getDisplayName() + " " +
                // plugin.getVersion());
            } else {
                String simpleName = new String(pluginInfo.getClazz().getSimpleName());
                DecrypterPlugin a = pluginInfo.getClazz().getAnnotation(DecrypterPlugin.class);
                if (a != null) {
                    try {
                        final long revision = pluginInfo.getLazyPluginClass().getRevision();
                        String[] names = a.names();
                        String[] patterns = a.urls();
                        int[] flags = a.flags();
                        if (names.length == 0) {
                            /* create multiple crawler plugins from one source */
                            patterns = (String[]) pluginInfo.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                            names = (String[]) pluginInfo.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});
                            flags = (int[]) pluginInfo.getClazz().getDeclaredMethod("getAnnotationFlags", new Class[] {}).invoke(null, new Object[] {});
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
                        final PluginClassLoaderChild classLoader = (PluginClassLoaderChild) pluginInfo.getClazz().getClassLoader();
                        /* during init we dont want dummy libs being created */
                        classLoader.setCreateDummyLibs(false);
                        for (int i = 0; i < names.length; i++) {
                            LazyCrawlerPlugin lazyCrawlerPlugin = null;
                            try {
                                lazyCrawlerPlugin = new LazyCrawlerPlugin(pluginInfo.getLazyPluginClass(), new String(patterns[i]), new String(names[i]), pluginInfo.getClazz(), classLoader);
                                try {
                                    /* check for stable compatibility */
                                    classLoader.setPluginClass(new String(pluginInfo.getClazz().getName()));
                                    classLoader.setCheckStableCompatibility(a.interfaceVersion() == 2);
                                    final PluginForDecrypt plg = lazyCrawlerPlugin.newInstance(classLoader);
                                    final Class<? extends ConfigInterface> configInterface = plg.getConfigInterface();
                                    if (configInterface != null) {
                                        lazyCrawlerPlugin.setConfigInterface(new String(configInterface.getName()));
                                    }
                                    lazyCrawlerPlugin.setMaxConcurrentInstances(plg.getMaxConcurrentProcessingInstances());
                                    lazyCrawlerPlugin.setHasConfig(plg.hasConfig());
                                    retList.add(lazyCrawlerPlugin);
                                } catch (UpdateRequiredClassNotFoundException e) {
                                    logger.finest("@CrawlerPlugin incomplete:" + simpleName + " " + new String(names[i]) + " " + e.getMessage() + " " + revision);
                                    throw e;
                                }
                                // logger.finest("@CrawlerPlugin ok:" + simpleName + " " + new String(names[i]) + " " + revision);
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

    private void save(List<LazyCrawlerPlugin> save, final AtomicLong lastFolderModification) {
        lock.writeLock();
        final File cache = Application.getTempResource(getCache());
        try {
            LazyCrawlerPluginCache.write(save, cache, lastFolderModification);
        } catch (final IOException e) {
            e.printStackTrace();
            cache.delete();
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

    @Override
    protected long[] getInfos(Class<PluginForDecrypt> clazz) {
        final DecrypterPlugin infos = clazz.getAnnotation(DecrypterPlugin.class);
        return new long[] { infos.interfaceVersion(), Formatter.getRevision(infos.revision()) };
    }

}
