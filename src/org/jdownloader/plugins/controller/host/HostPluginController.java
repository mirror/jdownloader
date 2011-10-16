package org.jdownloader.plugins.controller.host;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jd.JDInitFlags;
import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;

public class HostPluginController extends PluginController<PluginForHost> {
    private static final String               HTTP_JDOWNLOADER_ORG_R_PHP_U = "http://jdownloader.org/r.php?u=";
    private static final String               TMP_HOSTS_JSON               = "tmp/hosts.json";
    private static final HostPluginController INSTANCE                     = new HostPluginController();

    /**
     * get the only existing instance of HostPluginController. This is a
     * singleton
     * 
     * @return
     */
    public static HostPluginController getInstance() {
        return HostPluginController.INSTANCE;
    }

    private HashMap<String, LazyHostPlugin> hosterPluginMap;
    private List<LazyHostPlugin>            list;

    /**
     * Create a new instance of HostPluginController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private HostPluginController() {
        this.hosterPluginMap = new HashMap<String, LazyHostPlugin>();
        this.list = null;
    }

    public void init() {
        List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();
        final long t = System.currentTimeMillis();
        try {
            if (JDInitFlags.REFRESH_CACHE || JDInitFlags.SWITCH_RETURNED_FROM_UPDATE) {
                try {
                    /* do a fresh scan */
                    plugins = update();
                } catch (Throwable e) {
                    Log.L.severe("@HostPluginController: update failed!");
                    Log.exception(e);
                }
            } else {
                /* try to load from cache */
                try {
                    plugins = loadFromCache();
                } catch (Throwable e) {
                    Log.L.severe("@HostPluginController: cache failed!");
                    Log.exception(e);
                }
                if (plugins.size() == 0) {
                    try {
                        /* do a fresh scan */
                        plugins = update();
                    } catch (Throwable e) {
                        Log.L.severe("@HostPluginController: update failed!");
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            System.out.println("@HostPluginController: init " + (System.currentTimeMillis() - t));
        }
        if (plugins.size() == 0) {
            Log.L.severe("@HostPluginController: WTF, no plugins!");
        }
        list = Collections.unmodifiableList(plugins);
        for (LazyHostPlugin l : list) {
            hosterPluginMap.put(l.getDisplayName(), l);
        }
    }

    private List<LazyHostPlugin> loadFromCache() {
        List<LazyHostPlugin> ret = new ArrayList<LazyHostPlugin>();
        for (AbstractHostPlugin ap : JSonStorage.restoreFrom(Application.getResource(TMP_HOSTS_JSON), true, null, new TypeRef<ArrayList<AbstractHostPlugin>>() {
        }, new ArrayList<AbstractHostPlugin>())) {
            LazyHostPlugin l = new LazyHostPlugin(ap);
            ret.add(l);
        }
        return ret;
    }

    private List<LazyHostPlugin> update() throws MalformedURLException {
        List<LazyHostPlugin> ret = new ArrayList<LazyHostPlugin>();
        ArrayList<AbstractHostPlugin> save = new ArrayList<AbstractHostPlugin>();
        for (PluginInfo<PluginForHost> c : scan(HOSTERPATH)) {
            HostPlugin a = c.getClazz().getAnnotation(HostPlugin.class);
            if (a != null) {
                try {
                    String[] names = a.names();
                    String[] patterns = a.urls();
                    if (names.length == 0) {
                        /* create multiple hoster plugins from one source */
                        patterns = (String[]) c.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                        names = (String[]) c.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});
                    }
                    if (patterns.length != names.length) throw new WTFException("names.length != patterns.length");
                    if (names.length == 0) { throw new WTFException("names.length=0"); }
                    for (int i = 0; i < names.length; i++) {
                        try {
                            AbstractHostPlugin ap = new AbstractHostPlugin(c.getClazz().getSimpleName());
                            ap.setDisplayName(names[i]);
                            ap.setPattern(patterns[i]);
                            PluginForHost plg;
                            try {
                                plg = (PluginForHost) c.getClazz().newInstance();
                            } catch (java.lang.InstantiationException e) {
                                plg = (PluginForHost) c.getClazz().getConstructor(new Class[] { PluginWrapper.class }).newInstance(null);
                            }
                            ap.setPremium(plg.isPremiumEnabled());
                            String purl = plg.getBuyPremiumUrl();
                            if (purl != null && purl.startsWith(HTTP_JDOWNLOADER_ORG_R_PHP_U)) {
                                purl = Encoding.urlDecode(purl.substring(HTTP_JDOWNLOADER_ORG_R_PHP_U.length()), false);
                            }
                            ap.setPremiumUrl(purl);
                            LazyHostPlugin l = new LazyHostPlugin(ap, c.getClazz(), plg);
                            list.add(l);
                            save.add(ap);
                            Log.L.severe("@HostPlugin ok:" + c);
                        } catch (Throwable e) {
                            Log.L.severe("@HostPlugin failed:" + c + ":" + names[i]);
                            Log.exception(e);
                        }
                    }
                } catch (final Throwable e) {
                    Log.L.severe("@HostPlugin failed:" + c);
                    Log.exception(e);
                }
            } else {
                Log.L.severe("@HostPlugin missing:" + c);
            }
        }
        save(save);
        return ret;
    }

    private void save(List<AbstractHostPlugin> save) {
        JSonStorage.saveTo(Application.getResource(TMP_HOSTS_JSON), save);
    }

    public List<LazyHostPlugin> list() {
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

    public LazyHostPlugin get(String displayName) {
        lazyInit();
        return hosterPluginMap.get(displayName);
    }

    private static final String HOSTERPATH = "jd/plugins/hoster";

}
