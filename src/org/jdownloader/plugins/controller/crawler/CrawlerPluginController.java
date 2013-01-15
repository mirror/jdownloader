package org.jdownloader.plugins.controller.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jd.JDInitFlags;
import jd.nutils.Formatter;
import jd.plugins.DecrypterPlugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;

public class CrawlerPluginController extends PluginController<PluginForDecrypt> {

    private static final Object                                  INTSANCELOCK       = new Object();
    private static final Object                                  INITLOCK           = new Object();
    private static MinTimeWeakReference<CrawlerPluginController> INSTANCE           = null;
    private static Boolean                                       FIRSTINIT_UNCACHED = JDInitFlags.REFRESH_CACHE;

    /**
     * get the only existing instance of HostPluginController. This is a singleton
     * 
     * @return
     */
    public static CrawlerPluginController getInstance() {
        CrawlerPluginController ret = null;
        if (INSTANCE != null && (ret = INSTANCE.get()) != null) return ret;
        synchronized (INTSANCELOCK) {
            if (INSTANCE != null && (ret = INSTANCE.get()) != null) return ret;
            ret = new CrawlerPluginController();
            if (Boolean.TRUE.equals(FIRSTINIT_UNCACHED)) {
                ret.init(true);
            } else {
                ret.init(false);
            }
            FIRSTINIT_UNCACHED = null;
            INSTANCE = new MinTimeWeakReference<CrawlerPluginController>(ret, 30 * 1000l, "CrawlerPlugin");
        }
        return ret;
    }

    private List<LazyCrawlerPlugin> list;

    private String getCache() {
        return "tmp/crawler.ejs";
    }

    /**
     * Create a new instance of HostPluginController. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     * 
     */
    private CrawlerPluginController() {
        list = null;
    }

    public void init(boolean noCache) {
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
                    try {
                        /* do a fresh scan */
                        plugins = update(logger);
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
                            plugins = update(logger);
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
        for (LazyCrawlerPlugin plugin : plugins) {
            plugin.setPluginClass(null);
            plugin.setClassLoader(null);
        }
        list = plugins;
        System.gc();
    }

    private List<LazyCrawlerPlugin> loadFromCache() {
        java.util.List<AbstractCrawlerPlugin> l = null;
        synchronized (INITLOCK) {
            l = JSonStorage.restoreFrom(Application.getResource(getCache()), false, KEY, new TypeRef<ArrayList<AbstractCrawlerPlugin>>() {
            }, new ArrayList<AbstractCrawlerPlugin>());
        }
        List<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>(l.size());
        /* use this classLoader for all cached plugins to load */
        for (AbstractCrawlerPlugin ap : l) {
            ret.add(new LazyCrawlerPlugin(ap, null, null));
        }
        return ret;
    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private List<LazyCrawlerPlugin> update(LogSource logger) throws MalformedURLException {
        HashMap<String, LinkedList<AbstractCrawlerPlugin>> ret = new HashMap<String, LinkedList<AbstractCrawlerPlugin>>();
        HashMap<String, LazyCrawlerPlugin> ret2 = new HashMap<String, LazyCrawlerPlugin>();
        for (PluginInfo<PluginForDecrypt> c : scan("jd/plugins/decrypter")) {
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
                    if (names.length == 0) { throw new WTFException("names.length=0"); }
                    for (int i = 0; i < names.length; i++) {
                        PluginClassLoaderChild classLoader = null;
                        LazyCrawlerPlugin l = null;
                        try {
                            String displayName = new String(names[i]);
                            LinkedList<AbstractCrawlerPlugin> existingPlugin = ret.get(displayName);
                            /* we use new String() here to dereference the Annotation and it's loaded class */
                            AbstractCrawlerPlugin ap = new AbstractCrawlerPlugin(new String(c.getClazz().getSimpleName()));
                            ap.setDisplayName(displayName);
                            ap.setPattern(new String(patterns[i]));
                            ap.setVersion(revision);
                            ap.setInterfaceVersion(a.interfaceVersion());
                            classLoader = (PluginClassLoaderChild) c.getClazz().getClassLoader();
                            /* during init we dont want dummy libs being created */
                            classLoader.setCreateDummyLibs(false);
                            l = new LazyCrawlerPlugin(ap, null, classLoader);
                            if (existingPlugin == null) {
                                existingPlugin = new LinkedList<AbstractCrawlerPlugin>();
                                ret.put(displayName, existingPlugin);
                            }
                            boolean added = false;
                            ListIterator<AbstractCrawlerPlugin> it = existingPlugin.listIterator();
                            /* plugins with higher interfaceVersion will be sorted in list */
                            while (it.hasNext()) {
                                AbstractCrawlerPlugin next = it.next();
                                if (a.interfaceVersion() > next.getInterfaceVersion()) {
                                    it.add(ap);
                                    added = true;
                                    break;
                                }
                            }
                            if (added == false) {
                                /* add plugin at the end of list */
                                existingPlugin.add(ap);
                            }
                            try {
                                PluginForDecrypt plg = l.newInstance(classLoader);
                                ap.setHasConfig(plg.hasConfig());
                                ap.setMaxConcurrentInstances(plg.getMaxConcurrentProcessingInstances());
                                l.setMaxConcurrentInstances(ap.getMaxConcurrentInstances());
                                l.setHasConfig(plg.hasConfig());
                            } catch (UpdateRequiredClassNotFoundException e) {
                                logger.finest("@CrawlerPlugin incomplete:" + simpleName + " " + new String(names[i]) + " " + e.getMessage() + " " + revision);
                                throw e;
                            }
                            if (existingPlugin.size() > 1) {
                                logger.finest("@CrawlerPlugin multiple crawler:" + displayName + "->" + simpleName + " " + revision);
                            }
                            ret2.put(ap.getDisplayName() + ap.getPattern(), l);
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
                } catch (final Throwable e) {
                    logger.severe("@CrawlerPlugin failed:" + simpleName);
                    logger.log(e);
                }
            } else {
                logger.severe("@CrawlerPlugin missing:" + simpleName);
            }
        }

        java.util.List<AbstractCrawlerPlugin> saveList = new ArrayList<AbstractCrawlerPlugin>();
        for (LinkedList<AbstractCrawlerPlugin> crawler : ret.values()) {
            saveList.addAll(crawler);
        }
        save(saveList);
        return new ArrayList<LazyCrawlerPlugin>(ret2.values());
    }

    private void save(List<AbstractCrawlerPlugin> save) {
        synchronized (INITLOCK) {
            JSonStorage.saveTo(Application.getResource(getCache()), false, KEY, JSonStorage.serializeToJson(save));
        }
    }

    public List<LazyCrawlerPlugin> list() {
        lazyInit();
        return list;
    }

    /*
     * returns the list of available plugins
     * 
     * can return null if controller is not initiated yet and ensureLoaded is false
     */
    public static List<LazyCrawlerPlugin> list(boolean ensureLoaded) {
        CrawlerPluginController ret = null;
        if (INSTANCE != null && (ret = INSTANCE.get()) != null) {
            /* Controller is initiated */
            if (ensureLoaded) ret.lazyInit();
            return ret.list;
        } else {
            /* Controller is not initiated */
            if (ensureLoaded) {
                /* init the controller */
                ret = getInstance();
                return ret.list();
            } else {
                /* return null */
                return null;
            }
        }
    }

    public void setList(List<LazyCrawlerPlugin> list) {
        if (list == null) return;
        this.list = list;
    }

    private void lazyInit() {
        if (list != null) return;
        synchronized (this) {
            if (list != null) return;
            init(JDInitFlags.REFRESH_CACHE);
        }
    }

    public LazyCrawlerPlugin get(String displayName) {
        lazyInit();
        for (LazyCrawlerPlugin p : list) {
            if (p.getDisplayName().equalsIgnoreCase(displayName)) return p;
        }
        return null;
    }

}
