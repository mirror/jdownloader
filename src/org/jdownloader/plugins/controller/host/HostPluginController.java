package org.jdownloader.plugins.controller.host;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.JDInitFlags;
import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForHost;

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

    private HashMap<String, LazyHostPlugin> classNameMap;
    private ArrayList<LazyHostPlugin>       list;

    /**
     * Create a new instance of HostPluginController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private HostPluginController() {
        this.classNameMap = new HashMap<String, LazyHostPlugin>();
        this.list = new ArrayList<LazyHostPlugin>();
    }

    public void init() {

        if (JDInitFlags.REFRESH_CACHE || JDInitFlags.SWITCH_RETURNED_FROM_UPDATE) {
            long t = System.currentTimeMillis();
            try {
                update();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            System.out.println("Plugin Scanner Loader: " + (System.currentTimeMillis() - t));
        } else {
            // from cache
            long t = System.currentTimeMillis();
            loadFromCache();
            System.out.println("Plugin Cached Loader: " + (System.currentTimeMillis() - t));
            if (list.size() == 0) {
                // cache empty or damaged?
                t = System.currentTimeMillis();
                try {
                    update();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                System.out.println("Plugin Scanner Loader: " + (System.currentTimeMillis() - t));
            }
        }

        for (LazyHostPlugin l : list) {
            classNameMap.put(l.getClassname(), l);
        }
        System.out.println(list);

    }

    private void loadFromCache() {

        for (AbstractHostPlugin ap : JSonStorage.restoreFrom(Application.getResource(TMP_HOSTS_JSON), true, null, new TypeRef<ArrayList<AbstractHostPlugin>>() {
        }, new ArrayList<AbstractHostPlugin>())) {
            LazyHostPlugin l = new LazyHostPlugin(ap);
            list.add(l);
        }

    }

    private void update() throws MalformedURLException {
        ArrayList<AbstractHostPlugin> save = new ArrayList<AbstractHostPlugin>();
        for (PluginInfo<PluginForHost> c : scan(HOSTERPATH)) {

            HostPlugin a = c.getClazz().getAnnotation(HostPlugin.class);
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

    private void save(ArrayList<AbstractHostPlugin> save) {
        JSonStorage.saveTo(Application.getResource(TMP_HOSTS_JSON), save);
    }

    public ArrayList<LazyHostPlugin> list() {
        return list;
    }

    public LazyHostPlugin get(Class<? extends PluginForHost> class1) {
        return classNameMap.get(class1.getName());
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginForHost> T newInstance(Class<T> class1) {

        return (T) get(class1).newInstance();
    }

    public boolean canHandle(String data) {
        ArrayList<LazyHostPlugin> list = this.list;
        for (LazyHostPlugin l : list) {
            if (l.canHandle(data)) return true;
        }
        return false;
    }

    private static final String HOSTERPATH = "jd/plugins/hoster";

}
