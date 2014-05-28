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
import java.util.concurrent.atomic.AtomicBoolean;

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

    private volatile LinkedHashMap<String, LazyHostPlugin> list;
    private volatile LazyHostPlugin                        fallBackPlugin = null;
    private final ModifyLock                               lock           = new ModifyLock();

    public LazyHostPlugin getFallBackPlugin() {
        ensureLoaded();
        return fallBackPlugin;
    }

    private String getCache() {
        return "hosts.json";
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

    public synchronized LinkedHashMap<String, LazyHostPlugin> init(boolean noCache) {
        List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();
        LogSource logger = LogController.CL(false);
        logger.info("HostPluginController: init " + noCache);
        logger.setAllowTimeoutFlush(false);
        LogController.setRebirthLogger(logger);
        ClassLoader oldClassLoader = null;
        final LinkedHashMap<String, LazyHostPlugin> newList = new LinkedHashMap<String, LazyHostPlugin>();
        try {
            oldClassLoader = Thread.currentThread().getContextClassLoader();
            final long t = System.currentTimeMillis();
            try {
                if (noCache) {
                    /* try to load from cache, to speed up rescan */
                    final HashMap<String, ArrayList<LazyPlugin>> rescanCache = new HashMap<String, ArrayList<LazyPlugin>>();
                    try {
                        List<LazyHostPlugin> cachedPlugins = null;
                        if (this.list != null) {
                            cachedPlugins = new ArrayList<LazyHostPlugin>(this.list.values());
                        } else {
                            cachedPlugins = loadFromCache(logger);
                        }
                        if (cachedPlugins != null) {
                            for (LazyHostPlugin plugin : cachedPlugins) {
                                if (plugin.getMainClassFilename() == null) {
                                    continue;
                                }
                                if (plugin.getMainClassLastModified() <= 0) {
                                    continue;
                                }
                                if (plugin.getMainClassSHA256() == null) {
                                    continue;
                                }
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
                        logger.severe("@HostPluginController: update failed!");
                        logger.log(e);
                    }
                } else {
                    /* try to load from cache */
                    try {
                        plugins = loadFromCache(logger);
                    } catch (Throwable e) {
                        logger.severe("@HostPluginController: cache failed!");
                        logger.log(e);
                    }
                    if (plugins == null || plugins.size() == 0) {
                        try {
                            /* do a fresh scan */
                            plugins = update(logger, null);
                        } catch (Throwable e) {
                            logger.severe("@HostPluginController: update failed!");
                            logger.log(e);
                        }
                    }
                }
            } finally {
                logger.info("@HostPluginController: init " + (System.currentTimeMillis() - t) + " :" + plugins.size());
            }
            if (plugins.size() == 0) {
                logger.severe("@HostPluginController: WTF, no plugins!");
            } else {
                /* everything was okay */
                logger.clear();
            }
            try {
                Collections.sort(plugins, new Comparator<LazyHostPlugin>() {

                    public int compare(LazyHostPlugin o1, LazyHostPlugin o2) {
                        return o1.getDisplayName().compareTo(o2.getDisplayName());
                    }
                });
            } catch (final Throwable e) {
                logger.log(e);
            }
            LazyHostPlugin fallBackPlugin = null;
            for (LazyHostPlugin plugin : plugins) {
                plugin.setPluginClass(null);
                plugin.setClassLoader(null);
                if (fallBackPlugin == null && "UpdateRequired".equalsIgnoreCase(plugin.getDisplayName())) {
                    fallBackPlugin = plugin;
                    this.fallBackPlugin = plugin;
                    continue;
                }
                final String id = plugin.getDisplayName().toLowerCase(Locale.ENGLISH);
                final LazyHostPlugin existingPlugin = newList.put(id, plugin);
                if (existingPlugin != null) {
                    if (existingPlugin.getInterfaceVersion() > plugin.getInterfaceVersion()) {
                        newList.put(id, existingPlugin);
                        logger.finest("@HostPlugin keep:" + existingPlugin.getClassname() + "|" + existingPlugin.getInterfaceVersion() + ":" + existingPlugin.getVersion() + " instead " + plugin.getClassname() + "|" + plugin.getInterfaceVersion() + ":" + plugin.getVersion());
                    } else {
                        logger.finest("@HostPlugin replaced:" + existingPlugin.getClassname() + "|" + existingPlugin.getInterfaceVersion() + ":" + existingPlugin.getVersion() + " with " + plugin.getClassname() + "|" + plugin.getInterfaceVersion() + ":" + plugin.getVersion());
                    }
                }
            }
            for (LazyHostPlugin plugin : newList.values()) {
                plugin.setFallBackPlugin(fallBackPlugin);
            }
        } finally {
            logger.close();
            LogController.setRebirthLogger(null);
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        list = newList;
        System.gc();
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
        return newList;
    }

    private List<LazyHostPlugin> loadFromCache(LogSource logger) {
        boolean readL = lock.readLock();
        final List<AbstractHostPlugin> list;
        try {
            list = JSonStorage.restoreFrom(Application.getTempResource(getCache()), true, null, new TypeRef<ArrayList<AbstractHostPlugin>>() {
            }, new ArrayList<AbstractHostPlugin>());
        } finally {
            lock.readUnlock(readL);
        }
        if (list == null || list.size() == 0) {
            return null;
        }
        List<LazyHostPlugin> completeList = new ArrayList<LazyHostPlugin>(list.size());
        for (AbstractHostPlugin ap : list) {
            if (ap.getCacheVersion() != AbstractHostPlugin.CACHEVERSION) {
                throw new WTFException("Invalid CacheVersion found");
            }
            completeList.add(new LazyHostPlugin(ap, null, null));
        }
        return completeList;
    }

    private List<LazyHostPlugin> update(LogSource logger, HashMap<String, ArrayList<LazyPlugin>> pluginCache) throws MalformedURLException {
        final List<LazyHostPlugin> completeList = new ArrayList<LazyHostPlugin>();
        PluginClassLoaderChild classLoader;
        for (PluginInfo<PluginForHost> c : scan("jd/plugins/hoster", pluginCache)) {
            if (c.getLazyPlugin() != null) {
                LazyHostPlugin plugin = (LazyHostPlugin) c.getLazyPlugin();
                completeList.add(plugin);
                logger.finer("@HostPlugin ok(cached):" + plugin.getClassname() + " " + plugin.getDisplayName() + " " + plugin.getVersion());
                continue;
            }
            String simpleName = new String(c.getClazz().getSimpleName());
            HostPlugin a = c.getClazz().getAnnotation(HostPlugin.class);
            if (a != null) {
                try {
                    long revision = Formatter.getRevision(a.revision());
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
                    ClassLoader oldCL = null;
                    try {
                        oldCL = Thread.currentThread().getContextClassLoader();
                        classLoader = (PluginClassLoaderChild) c.getClazz().getClassLoader();
                        /* during init we dont want dummy libs being created */
                        classLoader.setCreateDummyLibs(false);
                        Thread.currentThread().setContextClassLoader(classLoader);
                        for (int i = 0; i < names.length; i++) {
                            LazyHostPlugin l = null;
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
                                l = new LazyHostPlugin(ap, null, classLoader);
                                try {
                                    /* check for stable compatibility */
                                    classLoader.setPluginClass(new String(c.getClazz().getName()));
                                    classLoader.setCheckStableCompatibility(a.interfaceVersion() == 2);
                                    PluginForHost plg = l.newInstance(classLoader);
                                    /* set configinterface */
                                    Class<? extends ConfigInterface> configInterface = plg.getConfigInterface();
                                    if (configInterface != null) {
                                        String name = new String(configInterface.getName());
                                        ap.setConfigInterface(name);
                                        l.setConfigInterface(name);
                                    }
                                    /* set premium */
                                    ap.setPremium(plg.isPremiumEnabled());
                                    l.setPremium(plg.isPremiumEnabled());

                                    /* set premiumUrl */
                                    String purl = plg.getBuyPremiumUrl();
                                    if (purl != null) {
                                        purl = new String(purl);
                                    }
                                    l.setPremiumUrl(purl);
                                    ap.setPremiumUrl(purl);

                                    /* set hasConfig */
                                    ap.setHasConfig(plg.hasConfig());
                                    l.setHasConfig(plg.hasConfig());

                                    /* set hasAccountRewrite */
                                    boolean hasAccountRewrite = false;
                                    try {
                                        if (plg.rewriteHost((Account) null) != null) {
                                            hasAccountRewrite = true;
                                        }
                                    } catch (Throwable e) {
                                    }
                                    ap.setHasAccountRewrite(hasAccountRewrite);
                                    l.setHasAccountRewrite(hasAccountRewrite);

                                    /* set hasLinkRewrite */
                                    boolean hasLinkRewrite = false;
                                    try {
                                        if (plg.rewriteHost((DownloadLink) null) != null) {
                                            hasLinkRewrite = true;
                                        }
                                    } catch (Throwable e) {
                                    }
                                    ap.setHasLinkRewrite(hasLinkRewrite);
                                    l.setHasLinkRewrite(hasLinkRewrite);
                                } catch (Throwable e) {
                                    if (e instanceof UpdateRequiredClassNotFoundException) {
                                        logger.finest("@HostPlugin incomplete:" + simpleName + " " + new String(names[i]) + " " + e.getMessage() + " " + revision);
                                    } else {
                                        throw e;
                                    }
                                }
                                completeList.add(l);
                                logger.finer("@HostPlugin ok:" + simpleName + " " + new String(names[i]) + " " + revision);
                            } catch (Throwable e) {
                                logger.severe("@HostPlugin failed:" + simpleName + " " + new String(names[i]) + " " + revision);
                                logger.log(e);
                            } finally {
                                /* now the pluginClassLoad may create dummy libraries */
                                if (l != null) {
                                    l.setClassLoader(null);
                                    l.setPluginClass(null);
                                }
                            }
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldCL);
                    }
                } catch (final Throwable e) {
                    logger.severe("@HostPlugin failed:" + simpleName);
                    logger.log(e);
                }
            } else {
                logger.severe("@HostPlugin missing:" + simpleName);
            }
        }

        this.fallBackPlugin = fallBackPlugin;
        Thread saveThread = new Thread("@HostPluginController:save") {
            public void run() {
                final ArrayList<AbstractHostPlugin> saveList = new ArrayList<AbstractHostPlugin>(completeList.size());
                for (LazyHostPlugin plugin : completeList) {
                    saveList.add(plugin.getAbstractHostPlugin());
                }
                save(saveList);
                FileCreationManager.getInstance().delete(Application.getTempResource(TMP_INVALIDPLUGINS), null);
            };
        };
        saveThread.setDaemon(true);
        saveThread.start();
        validateCache();
        return completeList;
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
        }
    }

    public Collection<LazyHostPlugin> list() {
        return ensureLoaded().values();
    }

    public LinkedHashMap<String, LazyHostPlugin> ensureLoaded() {
        LinkedHashMap<String, LazyHostPlugin> localList = list;
        if (localList != null && isCacheInvalidated() == false) {
            return localList;
        }
        synchronized (this) {
            localList = list;
            if (localList != null && isCacheInvalidated() == false) {
                return localList;
            }
            return init(isCacheInvalidated());
        }
    }

    public LazyHostPlugin get(String displayName) {
        LazyHostPlugin ret = ensureLoaded().get(displayName.toLowerCase(Locale.ENGLISH));
        if (ret != null) {
            return ret;
        }
        if ("UpdateRequired".equalsIgnoreCase(displayName)) {
            return fallBackPlugin;
        }
        return null;
    }

    public void invalidateCacheIfRequired() {
        if (Application.getTempResource(TMP_INVALIDPLUGINS).exists()) {
            invalidateCache();
        }
    }

}
