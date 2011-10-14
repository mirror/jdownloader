package org.jdownloader.plugins.controller.crawler;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.HostPluginWrapper;
import jd.JDInitFlags;
import jd.PluginWrapper;
import jd.plugins.DecrypterPlugin;
import jd.plugins.PluginForDecrypt;

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

    private HashMap<String, LazyCrawlerPlugin> map;
    private ArrayList<LazyCrawlerPlugin>       list;

    /**
     * Create a new instance of HostPluginController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private CrawlerPluginController() {
        this.map = new HashMap<String, LazyCrawlerPlugin>();
        this.list = new ArrayList<LazyCrawlerPlugin>();
    }

    public void init() {

        if (JDInitFlags.REFRESH_CACHE || JDInitFlags.SWITCH_RETURNED_FROM_UPDATE) {
            long t = System.currentTimeMillis();
            try {
                update();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            System.out.println("Crawler Plugin Scanner Loader: " + (System.currentTimeMillis() - t));
        } else {
            // from cache
            long t = System.currentTimeMillis();
            loadFromCache();
            System.out.println("Crawler Plugin Cached Loader: " + (System.currentTimeMillis() - t));
            if (list.size() == 0) {
                // cache empty or damaged?
                t = System.currentTimeMillis();
                try {
                    update();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                System.out.println("Crawler Plugin Scanner Loader: " + (System.currentTimeMillis() - t));
            }
        }
        System.out.println(list);

    }

    private void loadFromCache() {

        for (AbstractCrawlerPlugin ap : JSonStorage.restoreFrom(Application.getResource(CACHE_PATH), false, KEY, new TypeRef<ArrayList<AbstractCrawlerPlugin>>() {
        }, new ArrayList<AbstractCrawlerPlugin>())) {
            LazyCrawlerPlugin l = new LazyCrawlerPlugin(ap);
            list.add(l);
        }

    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private void update() throws MalformedURLException {
        ArrayList<AbstractCrawlerPlugin> save = new ArrayList<AbstractCrawlerPlugin>();
        for (PluginInfo<PluginForDecrypt> c : scan(PLUGIN_FOLDER_PATH)) {

            DecrypterPlugin a = c.getClazz().getAnnotation(DecrypterPlugin.class);
            if (a != null) {
                String[] names = a.names();
                String[] patterns = a.urls();
                if (names.length == 0) {
                    try {
                        patterns = (String[]) c.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                        names = (String[]) c.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});

                    } catch (Throwable e) {
                        Log.exception(e);
                    }
                }

                for (int i = 0; i < names.length; i++) {
                    try {
                        AbstractCrawlerPlugin ap = new AbstractCrawlerPlugin(c.getClazz().getSimpleName());
                        ap.setDisplayName(names[i]);
                        ap.setPattern(patterns[i]);
                        PluginForDecrypt plg;
                        try {
                            plg = (PluginForDecrypt) c.getClazz().newInstance();
                        } catch (java.lang.InstantiationException e) {
                            plg = (PluginForDecrypt) c.getClazz().getConstructor(new Class[] { PluginWrapper.class }).newInstance(new HostPluginWrapper(names[i], c.getClazz().getSimpleName(), patterns[i], 0, a.revision()));
                        }

                        LazyCrawlerPlugin l = new LazyCrawlerPlugin(ap, c.getClazz(), plg);
                        list.add(l);
                        save.add(ap);

                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                }
                System.out.println(c);

            } else {
                Log.L.severe("@HostPlugin missing for " + c);
            }
        }
        save(save);

    }

    private void save(ArrayList<AbstractCrawlerPlugin> save) {
        JSonStorage.saveTo(Application.getResource(CACHE_PATH), false, KEY, JSonStorage.serializeToJson(save));
    }

    private static final String PLUGIN_FOLDER_PATH = "jd/plugins/decrypter";

}
