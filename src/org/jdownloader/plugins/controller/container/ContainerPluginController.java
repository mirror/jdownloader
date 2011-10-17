package org.jdownloader.plugins.controller.container;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jd.JDInitFlags;
import jd.plugins.ContainerPlugin;
import jd.plugins.PluginsC;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.controller.PluginController;
import org.jdownloader.plugins.controller.PluginInfo;

public class ContainerPluginController extends PluginController<PluginsC> {
    private static final String                    CACHE_PATH = "tmp/container.ejs";
    private static final ContainerPluginController INSTANCE   = new ContainerPluginController();

    public static ContainerPluginController getInstance() {
        return ContainerPluginController.INSTANCE;
    }

    private HashMap<String, LazyContainerPlugin> containerPluginMap;
    private List<LazyContainerPlugin>            list;

    private ContainerPluginController() {
        containerPluginMap = new HashMap<String, LazyContainerPlugin>();
        list = null;
    }

    public void init() {
        List<LazyContainerPlugin> plugins = new ArrayList<LazyContainerPlugin>();
        final long t = System.currentTimeMillis();
        try {
            if (JDInitFlags.REFRESH_CACHE || JDInitFlags.SWITCH_RETURNED_FROM_UPDATE) {
                try {
                    /* do a fresh scan */
                    plugins = update();
                } catch (Throwable e) {
                    Log.L.severe("@ContainerPluginController: update failed!");
                    Log.exception(e);
                }
            } else {
                /* try to load from cache */
                try {
                    plugins = loadFromCache();
                } catch (Throwable e) {
                    Log.L.severe("@ContainerPluginController: cache failed!");
                    Log.exception(e);
                }
                if (plugins.size() == 0) {
                    try {
                        /* do a fresh scan */
                        plugins = update();
                    } catch (Throwable e) {
                        Log.L.severe("@ContainerPluginController: update failed!");
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            System.out.println("@ContainerPluginController: init " + (System.currentTimeMillis() - t) + " :" + plugins.size());
        }
        if (plugins.size() == 0) {
            Log.L.severe("@ContainerPluginController: WTF, no plugins!");
        }
        list = Collections.unmodifiableList(plugins);
        for (LazyContainerPlugin l : list) {
            containerPluginMap.put(l.getDisplayName(), l);
        }
    }

    private List<LazyContainerPlugin> loadFromCache() {
        List<LazyContainerPlugin> ret = new ArrayList<LazyContainerPlugin>();
        for (AbstractContainerPlugin ap : JSonStorage.restoreFrom(Application.getResource(CACHE_PATH), false, KEY, new TypeRef<ArrayList<AbstractContainerPlugin>>() {
        }, new ArrayList<AbstractContainerPlugin>())) {
            LazyContainerPlugin l = new LazyContainerPlugin(ap);
            ret.add(l);
        }
        return ret;
    }

    static public byte[] KEY = new byte[] { 0x01, 0x03, 0x11, 0x01, 0x01, 0x54, 0x01, 0x01, 0x01, 0x01, 0x12, 0x01, 0x01, 0x01, 0x22, 0x01 };

    private List<LazyContainerPlugin> update() throws MalformedURLException {
        List<LazyContainerPlugin> ret = new ArrayList<LazyContainerPlugin>();
        ArrayList<AbstractContainerPlugin> save = new ArrayList<AbstractContainerPlugin>();
        for (PluginInfo<PluginsC> c : scan(PLUGIN_FOLDER_PATH)) {
            String simpleName = c.getClazz().getSimpleName();
            ContainerPlugin a = c.getClazz().getAnnotation(ContainerPlugin.class);
            if (a != null) {
                try {
                    String[] names = a.names();
                    String[] patterns = a.urls();
                    if (names.length == 0) {
                        /* create multiple container plugins from one source */
                        patterns = (String[]) c.getClazz().getDeclaredMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                        names = (String[]) c.getClazz().getDeclaredMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});
                    }
                    if (patterns.length != names.length) throw new WTFException("names.length != patterns.length");
                    if (names.length == 0) { throw new WTFException("names.length=0"); }
                    for (int i = 0; i < names.length; i++) {
                        try {
                            AbstractContainerPlugin ap = new AbstractContainerPlugin(c.getClazz().getSimpleName());
                            ap.setDisplayName(names[i]);
                            ap.setPattern(patterns[i]);
                            LazyContainerPlugin l = new LazyContainerPlugin(ap);
                            l.getPrototype();
                            ret.add(l);
                            save.add(ap);
                            Log.L.severe("@ContainerPlugin ok:" + simpleName + " " + names[i]);
                        } catch (Throwable e) {
                            Log.L.severe("@ContainerPlugin failed:" + simpleName + " " + names[i]);
                            Log.exception(e);
                        }
                    }
                } catch (final Throwable e) {
                    Log.L.severe("@ContainerPlugin failed:" + simpleName);
                    Log.exception(e);
                }
            } else {
                Log.L.severe("@ContainerPlugin missing:" + simpleName);
            }
        }
        save(save);
        return ret;
    }

    private void save(List<AbstractContainerPlugin> save) {
        JSonStorage.saveTo(Application.getResource(CACHE_PATH), false, KEY, JSonStorage.serializeToJson(save));
    }

    private static final String PLUGIN_FOLDER_PATH = "jd/plugins/a";

    public List<LazyContainerPlugin> list() {
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

    public LazyContainerPlugin get(String displayName) {
        lazyInit();
        return containerPluginMap.get(displayName);
    }

    public String getContainerExtensions(final String filter) {
        lazyInit();
        StringBuilder sb = new StringBuilder("");
        for (final LazyContainerPlugin act : list) {
            if (filter != null && !new Regex(act.getDisplayName(), filter).matches()) continue;
            String exs[] = new Regex(act.getPattern().pattern(), "\\.([a-zA-Z0-9]+)").getColumn(0);
            for (String ex : exs) {
                if (sb.length() > 0) sb.append("|");
                sb.append(".").append(ex);
            }
        }
        return sb.toString();
    }
}