package org.jdownloader.plugins.controller.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.JDInitFlags;
import jd.nutils.Formatter;
import jd.plugins.DecrypterPlugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;

public class CrawlerPluginController extends PluginController<PluginForDecrypt> {

    private static final CrawlerPluginController INSTANCE = new CrawlerPluginController();

    /**
     * get the only existing instance of HostPluginController. This is a singleton
     * 
     * @return
     */
    public static CrawlerPluginController getInstance() {
        return CrawlerPluginController.INSTANCE;
    }

    private List<LazyCrawlerPlugin> list;

    private String getCache() {
        return "tmp/crawler.ejs";
    }

    /**
     * Create a new instance of HostPluginController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private CrawlerPluginController() {
        list = null;
    }

    public void init(boolean noCache) {
        List<LazyCrawlerPlugin> plugins = new ArrayList<LazyCrawlerPlugin>();
        final long t = System.currentTimeMillis();
        try {
            if (noCache) {
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
                        Log.exception(e);
                    }
                }
            }
        } finally {
            Log.L.info("@CrawlerPluginController: init " + (System.currentTimeMillis() - t) + " :" + plugins.size());
        }
        if (plugins.size() == 0) {
            Log.L.severe("@CrawlerPluginController: WTF, no plugins!");
        }
        list = plugins;
        System.gc();
    }

    private List<LazyCrawlerPlugin> loadFromCache() {
        ArrayList<AbstractCrawlerPlugin> l = JSonStorage.restoreFrom(Application.getResource(getCache()), false, KEY, new TypeRef<ArrayList<AbstractCrawlerPlugin>>() {
        }, new ArrayList<AbstractCrawlerPlugin>());
        List<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>(l.size());
        PluginClassLoaderChild classLoader = PluginClassLoader.getInstance().getChild();
        /* use this classLoader for all cached plugins to load */
        for (AbstractCrawlerPlugin ap : l) {
            ret.add(new LazyCrawlerPlugin(ap, null, classLoader));
        }
        return ret;
    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private List<LazyCrawlerPlugin> update() throws MalformedURLException {
        HashMap<String, AbstractCrawlerPlugin> ret = new HashMap<String, AbstractCrawlerPlugin>();
        HashMap<String, LazyCrawlerPlugin> ret2 = new HashMap<String, LazyCrawlerPlugin>();
        PluginClassLoaderChild classLoader = PluginClassLoader.getInstance().getChild();
        for (PluginInfo<PluginForDecrypt> c : scan("jd/plugins/decrypter")) {
            String simpleName = c.getClazz().getSimpleName();
            DecrypterPlugin a = c.getClazz().getAnnotation(DecrypterPlugin.class);
            if (a != null) {
                try {
                    long revision = Formatter.getRevision(a.revision());
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
                            String displayName = new String(names[i]);
                            AbstractCrawlerPlugin existingPlugin = ret.get(displayName + patterns[i]);
                            if (existingPlugin != null && existingPlugin.getInterfaceVersion() > a.interfaceVersion()) {
                                /*
                                 * we already loaded a plugin with higher interfaceVersion, so skip older one
                                 */
                                continue;
                            }
                            AbstractCrawlerPlugin ap = new AbstractCrawlerPlugin(new String(c.getClazz().getSimpleName()));
                            ap.setDisplayName(displayName);

                            // why new String?
                            ap.setPattern(new String(patterns[i]));
                            ap.setVersion(revision);
                            ap.setInterfaceVersion(a.interfaceVersion());
                            LazyCrawlerPlugin l = new LazyCrawlerPlugin(ap, null, classLoader);
                            existingPlugin = ret.put(ap.getDisplayName() + ap.getPattern(), ap);
                            if (existingPlugin != null) {
                                Log.L.finest("@CrawlerPlugin replaced:" + simpleName + " " + names[i]);
                            }
                            ret2.put(ap.getDisplayName() + ap.getPattern(), l);
                            Log.L.finest("@CrawlerPlugin ok:" + simpleName + " " + names[i]);
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
        save(new ArrayList<AbstractCrawlerPlugin>(ret.values()));
        return new ArrayList<LazyCrawlerPlugin>(ret2.values());
    }

    private void save(List<AbstractCrawlerPlugin> save) {
        JSonStorage.saveTo(Application.getResource(getCache()), false, KEY, JSonStorage.serializeToJson(save));
    }

    public List<LazyCrawlerPlugin> list() {
        lazyInit();
        return list;
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
