package org.jdownloader.plugins.controller.host;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.utils.Application;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class HostPluginController extends PluginController<PluginForHost> {

    public static final String                TMP_INVALIDPLUGINS = "invalidplugins";
    private static final HostPluginController INSTANCE           = new HostPluginController();

    /**
     * get the only existing instance of HostPluginController. This is a singleton
     * 
     * @return
     */
    public static HostPluginController getInstance() {
        return HostPluginController.INSTANCE;
    }

    private volatile Map<String, LazyHostPlugin> list;
    private volatile LazyHostPlugin              fallBackPlugin = null;
    private final ModifyLock                     lock           = new ModifyLock();

    public LazyHostPlugin getFallBackPlugin() {
        ensureLoaded();
        return fallBackPlugin;
    }

    private String getCache() {
        return "hosts2.json";
    }

    /**
     * Create a new instance of HostPluginController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private HostPluginController() {
        this.list = null;
        try {
            // load config
            Class.forName("org.jdownloader.container.Config");
        } catch (Throwable e) {
        }
    }

    public synchronized Map<String, LazyHostPlugin> init() {
        final LogSource logger = LogController.CL(false);
        logger.info("HostPluginController: init");
        logger.setAllowTimeoutFlush(false);
        logger.setAutoFlushOnThrowable(true);
        LogController.setRebirthLogger(logger);
        final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        final long completeTimeStamp = System.currentTimeMillis();
        try {
            List<LazyHostPlugin> updateCache = null;
            /* try to load from cache */
            long timeStamp = System.currentTimeMillis();
            try {
                updateCache = loadFromCache();
            } catch (Throwable e) {
                logger.log(e);
                logger.severe("@HostPluginController: cache failed!");
            } finally {
                if (updateCache != null && updateCache.size() > 0) {
                    logger.info("@HostPluginController: loadFromCache took " + (System.currentTimeMillis() - timeStamp) + "ms for " + updateCache.size());
                }
            }
            List<LazyHostPlugin> plugins = null;
            timeStamp = System.currentTimeMillis();
            try {
                /* do a fresh scan */
                plugins = update(logger, updateCache);
            } catch (Throwable e) {
                logger.log(e);
                logger.severe("@HostPluginController: update failed!");
            } finally {
                if (plugins != null && plugins.size() > 0) {
                    logger.info("@HostPluginController: update took " + (System.currentTimeMillis() - timeStamp) + "ms for " + plugins.size());
                }
            }
            if (plugins == null || plugins.size() == 0) {
                if (plugins == null) {
                    plugins = new ArrayList<LazyHostPlugin>();
                }
                logger.severe("@HostPluginController: WTF, no plugins!");
            }
            timeStamp = System.currentTimeMillis();
            try {
                Collections.sort(plugins, new Comparator<LazyHostPlugin>() {

                    public int compare(LazyHostPlugin o1, LazyHostPlugin o2) {
                        return o1.getDisplayName().compareTo(o2.getDisplayName());
                    }
                });
            } catch (final Throwable e) {
                logger.log(e);
                logger.severe("@HostPluginController: sort failed!");
            } finally {
                logger.info("@HostPluginController: sort took " + (System.currentTimeMillis() - timeStamp) + "ms for " + plugins.size());
            }
            timeStamp = System.currentTimeMillis();
            final LinkedHashMap<String, LazyHostPlugin> retMap = new LinkedHashMap<String, LazyHostPlugin>();
            LazyHostPlugin fallBackPlugin = null;
            for (LazyHostPlugin plugin : plugins) {
                plugin.setPluginClass(null);
                plugin.setClassLoader(null);
                if (fallBackPlugin == null && "UpdateRequired".equalsIgnoreCase(plugin.getDisplayName())) {
                    fallBackPlugin = plugin;
                    this.fallBackPlugin = plugin;
                    continue;
                }
                final String pluginID = plugin.getDisplayName().toLowerCase(Locale.ENGLISH);
                final LazyHostPlugin existingPlugin = retMap.put(pluginID, plugin);
                if (existingPlugin != null) {
                    if (existingPlugin.getInterfaceVersion() > plugin.getInterfaceVersion()) {
                        retMap.put(pluginID, existingPlugin);
                        logger.finest("@HostPlugin keep:" + existingPlugin.getClassname() + "|" + existingPlugin.getInterfaceVersion() + ":" + existingPlugin.getVersion() + " instead " + plugin.getClassname() + "|" + plugin.getInterfaceVersion() + ":" + plugin.getVersion());
                    } else {
                        logger.finest("@HostPlugin replaced:" + existingPlugin.getClassname() + "|" + existingPlugin.getInterfaceVersion() + ":" + existingPlugin.getVersion() + " with " + plugin.getClassname() + "|" + plugin.getInterfaceVersion() + ":" + plugin.getVersion());
                    }
                }
            }
            for (LazyHostPlugin plugin : retMap.values()) {
                plugin.setFallBackPlugin(fallBackPlugin);
            }
            logger.info("@HostPluginController: mapping took " + (System.currentTimeMillis() - timeStamp) + "ms for " + plugins.size());
            final List<LazyHostPlugin> finalPlugins = plugins;
            Thread saveThread = new Thread("@HostPluginController:save") {
                public void run() {
                    final ArrayList<AbstractHostPlugin> saveList = new ArrayList<AbstractHostPlugin>(finalPlugins.size());
                    for (LazyHostPlugin plugin : finalPlugins) {
                        saveList.add(plugin.getAbstractHostPlugin());
                    }
                    save(saveList);
                };
            };
            saveThread.setDaemon(true);
            saveThread.start();
            list = Collections.<String, LazyHostPlugin> unmodifiableMap(retMap);
        } finally {
            validateCache();
            LogController.setRebirthLogger(null);
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            final Map<String, LazyHostPlugin> llist = list;
            if (llist != null) {
                logger.info("@HostPluginController: init took " + (System.currentTimeMillis() - completeTimeStamp) + "ms for " + llist.size());
            } else {
                logger.info("@HostPluginController: init took " + (System.currentTimeMillis() - completeTimeStamp));
            }
            logger.close();
        }
        System.gc();
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {
                    private final PluginFinder finder = new PluginFinder();

                    private final LogSource    logger = LogController.CL(false);

                    @Override
                    public void execute(DownloadSession currentSession) {
                        DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                            @Override
                            public int returnMaxResults() {
                                return 0;
                            }

                            private final void updatePluginInstance(DownloadLink link) {
                                long beforeVersion = -1;
                                if (link.getDefaultPlugin() != null) {
                                    beforeVersion = link.getDefaultPlugin().getLazyP().getVersion();
                                }
                                PluginForHost afterPlugin = finder.assignPlugin(link, true, logger);
                                if (link.getFinalLinkState() == FinalLinkState.PLUGIN_DEFECT && afterPlugin != null && beforeVersion != afterPlugin.getLazyP().getVersion()) {
                                    link.setFinalLinkState(null);
                                }
                            }

                            @Override
                            public boolean acceptNode(final DownloadLink node) {
                                if (node.getDownloadLinkController() != null) {
                                    node.getDownloadLinkController().getJobsAfterDetach().add(new DownloadWatchDogJob() {

                                        @Override
                                        public void execute(DownloadSession currentSession) {
                                            updatePluginInstance(node);
                                        }

                                        @Override
                                        public void interrupt() {
                                        }
                                    });
                                } else {
                                    updatePluginInstance(node);
                                }
                                return false;
                            }
                        });
                    }

                    @Override
                    public void interrupt() {
                    }
                });
            }
        });
        return list;
    }

    private List<LazyHostPlugin> loadFromCache() {
        boolean readL = lock.readLock();
        final List<AbstractHostPlugin> list;
        try {
            list = JSonStorage.restoreFrom(Application.getTempResource(getCache()), true, null, new TypeRef<ArrayList<AbstractHostPlugin>>() {
            }, null);
        } finally {
            lock.readUnlock(readL);
        }
        if (list == null || list.size() == 0) {
            return null;
        }
        final List<LazyHostPlugin> cachedPlugins = new ArrayList<LazyHostPlugin>(list.size());
        for (AbstractHostPlugin ap : list) {
            if (ap.getCacheVersion() != AbstractHostPlugin.CACHEVERSION) {
                throw new WTFException("Invalid CacheVersion found");
            }
            cachedPlugins.add(new LazyHostPlugin(ap, null, null));
        }
        return cachedPlugins;
    }

    private List<LazyHostPlugin> update(LogSource logger, List<LazyHostPlugin> updateCache) throws MalformedURLException {
        final Map<String, ArrayList<LazyPlugin>> updateCacheMap;
        if (updateCache != null && updateCache.size() > 0) {
            updateCacheMap = new HashMap<String, ArrayList<LazyPlugin>>();
            try {
                for (final LazyHostPlugin cachedPlugin : updateCache) {
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
        final List<LazyHostPlugin> retList = new ArrayList<LazyHostPlugin>();
        for (PluginInfo<PluginForHost> c : scan("jd/plugins/hoster", updateCacheMap)) {
            if (c.getLazyPlugin() != null) {
                final LazyHostPlugin plugin = (LazyHostPlugin) c.getLazyPlugin();
                retList.add(plugin);
                logger.finer("@HostPlugin ok(cached):" + plugin.getClassname() + " " + plugin.getDisplayName() + " " + plugin.getVersion());
            } else {
                final String simpleName = new String(c.getClazz().getSimpleName());
                final HostPlugin a = c.getClazz().getAnnotation(HostPlugin.class);
                if (a != null) {
                    try {
                        final long revision = Formatter.getRevision(a.revision());
                        String[] names = a.names();
                        String[] patterns = a.urls();
                        int[] flags = a.flags();
                        if (names.length == 0) {
                            /* create multiple hoster plugins from one source */
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
                            logger.log((new WTFException("PLUGIN STABLE ISSUE!! names.length(" + names.length + ")!= flags.length(" + flags.length + ")->" + simpleName)));
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
                            LazyHostPlugin lazyHostPlugin = null;
                            try {
                                String displayName = new String(names[i]);
                                /* we use new String() here to dereference the Annotation and it's loaded class */
                                AbstractHostPlugin ap = new AbstractHostPlugin(new String(c.getClazz().getSimpleName()));
                                ap.setCacheVersion(AbstractHostPlugin.CACHEVERSION);
                                ap.setDisplayName(displayName);
                                ap.setPattern(new String(patterns[i]));
                                ap.setVersion(revision);
                                ap.setInterfaceVersion(a.interfaceVersion());

                                /* information to speed up rescan */
                                ap.setMainClassSHA256(c.getMainClassSHA256());
                                ap.setMainClassLastModified(c.getMainClassLastModified());
                                ap.setMainClassFilename(c.getFile().getName());
                                lazyHostPlugin = new LazyHostPlugin(ap, null, classLoader);
                                try {
                                    /* check for stable compatibility */
                                    classLoader.setPluginClass(new String(c.getClazz().getName()));
                                    classLoader.setCheckStableCompatibility(a.interfaceVersion() == 2);
                                    PluginForHost plg = lazyHostPlugin.newInstance(classLoader);
                                    /* set configinterface */
                                    Class<? extends ConfigInterface> configInterface = plg.getConfigInterface();
                                    if (configInterface != null) {
                                        String name = new String(configInterface.getName());
                                        ap.setConfigInterface(name);
                                        lazyHostPlugin.setConfigInterface(name);
                                    }
                                    /* set premium */
                                    ap.setPremium(plg.isPremiumEnabled());
                                    lazyHostPlugin.setPremium(plg.isPremiumEnabled());

                                    /* set premiumUrl */
                                    String purl = plg.getBuyPremiumUrl();
                                    if (purl != null) {
                                        purl = new String(purl);
                                    }
                                    lazyHostPlugin.setPremiumUrl(purl);
                                    ap.setPremiumUrl(purl);

                                    /* set hasConfig */
                                    ap.setHasConfig(plg.hasConfig());
                                    lazyHostPlugin.setHasConfig(plg.hasConfig());

                                    /* set hasAccountRewrite */
                                    boolean hasAccountRewrite = false;
                                    try {
                                        if (plg.rewriteHost((Account) null) != null) {
                                            hasAccountRewrite = true;
                                        }
                                    } catch (Throwable e) {
                                    }
                                    ap.setHasAccountRewrite(hasAccountRewrite);
                                    lazyHostPlugin.setHasAccountRewrite(hasAccountRewrite);
                                    /* set hasLinkRewrite */
                                    boolean hasLinkRewrite = false;
                                    try {
                                        if (plg.rewriteHost((DownloadLink) null) != null) {
                                            hasLinkRewrite = true;
                                        }
                                    } catch (Throwable e) {
                                    }
                                    ap.setHasLinkRewrite(hasLinkRewrite);
                                    lazyHostPlugin.setHasLinkRewrite(hasLinkRewrite);
                                } catch (Throwable e) {
                                    if (e instanceof UpdateRequiredClassNotFoundException) {
                                        logger.log(e);
                                        logger.finest("@HostPlugin incomplete:" + simpleName + " " + new String(names[i]) + " " + e.getMessage() + " " + revision);
                                    } else {
                                        throw e;
                                    }
                                }
                                if (lazyHostPlugin != null) {
                                    retList.add(lazyHostPlugin);
                                    logger.finer("@HostPlugin ok:" + simpleName + " " + new String(names[i]) + " " + revision);
                                }
                            } catch (Throwable e) {
                                logger.log(e);
                                logger.severe("@HostPlugin failed:" + simpleName + " " + new String(names[i]) + " " + revision);
                            } finally {
                                /* now the pluginClassLoad may create dummy libraries */
                                if (lazyHostPlugin != null) {
                                    lazyHostPlugin.setClassLoader(null);
                                    lazyHostPlugin.setPluginClass(null);
                                }
                            }
                        }
                    } catch (final Throwable e) {
                        logger.severe("@HostPlugin failed:" + simpleName);
                        logger.log(e);
                    }
                } else {
                    logger.severe("@HostPlugin missing:" + simpleName);
                }
            }
        }
        return retList;
    }

    private AtomicBoolean cacheInvalidated = new AtomicBoolean(false);

    public boolean isCacheInvalidated() {
        return cacheInvalidated.get();
    }

    public void invalidateCache() {
        cacheInvalidated.set(true);
    }

    protected void validateCache() {
        cacheInvalidated.set(false);
    }

    private void save(List<AbstractHostPlugin> save) {
        lock.writeLock();
        try {
            JSonStorage.saveTo(Application.getTempResource(getCache()), save);
        } finally {
            lock.writeUnlock();
            FileCreationManager.getInstance().delete(Application.getTempResource(TMP_INVALIDPLUGINS), null);
        }
    }

    public Collection<LazyHostPlugin> list() {
        return ensureLoaded().values();
    }

    public Map<String, LazyHostPlugin> ensureLoaded() {
        Map<String, LazyHostPlugin> localList = list;
        if (localList != null && isCacheInvalidated() == false) {
            return localList;
        }
        synchronized (this) {
            localList = list;
            if (localList != null && isCacheInvalidated() == false) {
                return localList;
            }
            return init();
        }
    }

    public LazyHostPlugin get(String displayName) {
        final LazyHostPlugin ret = ensureLoaded().get(displayName.toLowerCase(Locale.ENGLISH));
        if (ret != null) {
            return ret;
        } else {
            if ("UpdateRequired".equalsIgnoreCase(displayName)) {
                return fallBackPlugin;
            }
            return null;
        }
    }

    public void invalidateCacheIfRequired() {
        if (Application.getTempResource(TMP_INVALIDPLUGINS).exists()) {
            invalidateCache();
        }
    }

}
