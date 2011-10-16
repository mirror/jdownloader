package org.jdownloader.plugins.controller.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jd.JDInitFlags;
import jd.plugins.DecrypterPlugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;

public class CrawlerPluginController extends PluginController<PluginForDecrypt> {

    private static final String                  CACHE_PATH = "tmp/crawler.ejs";
    private static final CrawlerPluginController INSTANCE   = new CrawlerPluginController();

    /**
     * get the only existing instance of HostPluginController. This is a
     * singleton
     * 
     * @return
     */
    public static CrawlerPluginController getInstance() {
        return CrawlerPluginController.INSTANCE;
    }

    private HashMap<String, LazyCrawlerPlugin> crawlerPluginMap;
    private List<LazyCrawlerPlugin>            list;

    /**
     * Create a new instance of HostPluginController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private CrawlerPluginController() {
        crawlerPluginMap = new HashMap<String, LazyCrawlerPlugin>();
        list = null;
    }

    public void init() {
        List<LazyCrawlerPlugin> plugins = new ArrayList<LazyCrawlerPlugin>();
        final long t = System.currentTimeMillis();
        try {
            if (JDInitFlags.REFRESH_CACHE || JDInitFlags.SWITCH_RETURNED_FROM_UPDATE) {
                try {
                    /* do a fresh scan */
                    plugins = update();
                } catch (Throwable e) {
                    Log.L.severe("@CrawlerPluginController: update failed!");
                    Log.exception(e);
                }
            } else {
                /* try to load from cache */
                try {
                    plugins = loadFromCache();
                } catch (Throwable e) {
                    Log.L.severe("@CrawlerPluginController: cache failed!");
                    Log.exception(e);
                }
                if (plugins.size() == 0) {
                    try {
                        /* do a fresh scan */
                        plugins = update();
                    } catch (Throwable e) {
                        Log.L.severe("@CrawlerPluginController: update failed!");
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            System.out.println("@CrawlerPluginController: init " + (System.currentTimeMillis() - t) + " :" + plugins.size());
        }
        if (plugins.size() == 0) {
            Log.L.severe("@CrawlerPluginController: WTF, no plugins!");
        }
        list = Collections.unmodifiableList(plugins);
        for (LazyCrawlerPlugin l : list) {
            crawlerPluginMap.put(l.getDisplayName(), l);
        }
    }

    private List<LazyCrawlerPlugin> loadFromCache() {
        List<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>();
        for (AbstractCrawlerPlugin ap : JSonStorage.restoreFrom(Application.getResource(CACHE_PATH), false, KEY, new TypeRef<ArrayList<AbstractCrawlerPlugin>>() {
        }, new ArrayList<AbstractCrawlerPlugin>())) {
            LazyCrawlerPlugin l = new LazyCrawlerPlugin(ap);
            ret.add(l);
        }
        return ret;
    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private List<LazyCrawlerPlugin> update() throws MalformedURLException {
        List<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>();
        ArrayList<AbstractCrawlerPlugin> save = new ArrayList<AbstractCrawlerPlugin>();
        for (PluginInfo<PluginForDecrypt> c : scan(PLUGIN_FOLDER_PATH)) {
            String simpleName = c.getClazz().getSimpleName();
            DecrypterPlugin a = c.getClazz().getAnnotation(DecrypterPlugin.class);
            if (a != null) {
                try {
                    String[] names = a.names();
                    String[] patterns = a.urls();
                    if (names.length == 0) {
                        /* create multiple crawler plugins from one source */
                        patterns = (String[]) c.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                        names = (String[]) c.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});
                    }
                    if (patterns.length != names.length) throw new WTFException("names.length != patterns.length");
                    if (names.length == 0) { throw new WTFException("names.length=0"); }
                    for (int i = 0; i < names.length; i++) {
                        try {
                            AbstractCrawlerPlugin ap = new AbstractCrawlerPlugin(c.getClazz().getSimpleName());
                            ap.setDisplayName(names[i]);
                            ap.setPattern(patterns[i]);
                            LazyCrawlerPlugin l = new LazyCrawlerPlugin(ap);
                            l.getPrototype();
                            ret.add(l);
                            save.add(ap);
                            Log.L.severe("@CrawlerPlugin ok:" + simpleName + " " + names[i]);
                        } catch (Throwable e) {
                            Log.L.severe("@CrawlerPlugin failed:" + simpleName + " " + names[i]);
                            Log.exception(e);
                        }
                    }
                } catch (final Throwable e) {
                    Log.L.severe("@CrawlerPlugin failed:" + simpleName);
                    Log.exception(e);
                }
            } else {
                Log.L.severe("@CrawlerPlugin missing:" + simpleName);
            }
        }
        save(save);
        return ret;
    }

    private void save(List<AbstractCrawlerPlugin> save) {
        JSonStorage.saveTo(Application.getResource(CACHE_PATH), false, KEY, JSonStorage.serializeToJson(save));
    }

    private static final String PLUGIN_FOLDER_PATH = "jd/plugins/decrypter";

    public List<LazyCrawlerPlugin> list() {
        lazyInit();
        return list;
    }

    private void lazyInit() {
        if (list != null) return;
        synchronized (this) {
            if (list != null) return;
            init();
        }
    }

    public LazyCrawlerPlugin get(String displayName) {
        lazyInit();
        return crawlerPluginMap.get(displayName);
    }

}
