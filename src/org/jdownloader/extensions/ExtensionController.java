package org.jdownloader.extensions;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.JDInitFlags;
import jd.Main;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.translate._JDT;

public class ExtensionController {
    private static final ExtensionController INSTANCE = new ExtensionController();

    /**
     * get the only existing instance of ExtensionController. This is a
     * singleton
     * 
     * @return
     */
    public static ExtensionController getInstance() {
        return ExtensionController.INSTANCE;
    }

    private HashMap<Class<AbstractExtension<?>>, LazyExtension> map;
    private List<LazyExtension>                                 list;
    private HashMap<String, LazyExtension>                      cache;
    private File                                                cacheFile;
    private boolean                                             cacheChanged;
    private ExtensionControllerEventSender                      eventSender;

    /**
     * Create a new instance of ExtensionController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ExtensionController() {

        map = new HashMap<Class<AbstractExtension<?>>, LazyExtension>();
        cacheFile = Application.getResource("tmp/extensioncache/cache.json");
        eventSender = new ExtensionControllerEventSender();
        // Collections.sort(pluginsOptional, new
        // Comparator<AbstractExtensionWrapper>() {
        //
        // public int compare(AbstractExtensionWrapper o1,
        // AbstractExtensionWrapper o2) {
        // return o1.getName().compareTo(o2.getName());
        // }
        // });
    }

    public ExtensionControllerEventSender getEventSender() {
        return eventSender;
    }

    public void init() {

        final long t = System.currentTimeMillis();
        try {
            if (JDInitFlags.REFRESH_CACHE || JDInitFlags.SWITCH_RETURNED_FROM_UPDATE) {
                try {
                    /* do a fresh scan */
                    load();
                } catch (Throwable e) {
                    Log.L.severe("@ExtensionController: update failed!");
                    Log.exception(e);
                }
            } else {
                /* try to load from cache */
                try {
                    list = loadFromCache();
                } catch (Throwable e) {
                    Log.L.severe("@ExtensionController: cache failed!");
                    Log.exception(e);
                }
                if (list.size() == 0) {
                    try {
                        /* do a fresh scan */
                        load();
                    } catch (Throwable e) {
                        Log.L.severe("@ExtensionController: update failed!");
                        e.printStackTrace();
                    }
                }
            }
        } finally {

            System.out.println("@ExtensionController: init in" + (System.currentTimeMillis() - t) + "ms :" + list.size());

        }
        if (list.size() == 0) {
            Log.L.severe("@ExtensionController: WTF, no extensions!");
        }

        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                for (LazyExtension plg : list) {
                    if (plg._getExtension() != null && plg._getExtension().getGUI() != null) {
                        plg._getExtension().getGUI().restore();
                    }
                }
            }

        });
        getEventSender().fireEvent(new ExtensionControllerEvent(this, ExtensionControllerEvent.Type.UPDATED));
    }

    private List<LazyExtension> loadFromCache() {
        cache = JSonStorage.restoreFrom(cacheFile, true, null, new TypeRef<HashMap<String, LazyExtension>>() {
        }, new HashMap<String, LazyExtension>());

        return new ArrayList<LazyExtension>(cache.values());
    }

    private void load() {
        try {
            cache = new HashMap<String, LazyExtension>();
            list = new ArrayList<LazyExtension>();
            cacheChanged = false;
            if (Application.isJared(ExtensionController.class)) {
                loadJared();
            } else {
                loadUnpacked();
            }
            if (cacheChanged) {
                JSonStorage.saveTo(cacheFile, cache);
            }
        } catch (final Throwable e) {
            Log.exception(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadJared() {

        File[] addons = Application.getResource("extensions").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        if (addons == null) return;
        main: for (File jar : addons) {
            try {
                URLClassLoader cl = new URLClassLoader(new URL[] { jar.toURI().toURL() });

                final Enumeration<URL> urls = cl.getResources(AbstractExtension.class.getPackage().getName().replace('.', '/'));
                URL url;
                while (urls.hasMoreElements()) {
                    url = urls.nextElement();
                    if (url.getProtocol().equalsIgnoreCase("jar")) {
                        // jarred addon (JAR)
                        File jarFile = new File(new URL(url.getPath().substring(4, url.toString().lastIndexOf('!'))).toURI());
                        final JarInputStream jis = new JarInputStream(new FileInputStream(jarFile));
                        JarEntry e;

                        while ((e = jis.getNextJarEntry()) != null) {
                            try {
                                Matcher matcher = Pattern.compile(Pattern.quote(AbstractExtension.class.getPackage().getName().replace('.', '/')) + "/(\\w+)/(\\w+Extension)\\.class").matcher(e.getName());
                                if (matcher.find()) {
                                    String pkg = matcher.group(1);
                                    String clazzName = matcher.group(2);
                                    Class<?> clazz = cl.loadClass(AbstractExtension.class.getPackage().getName() + "." + pkg + "." + clazzName);

                                    if (AbstractExtension.class.isAssignableFrom(clazz)) {

                                        initModule((Class<AbstractExtension<?>>) clazz);
                                        continue main;
                                    }

                                }
                            } catch (Throwable e1) {
                                Log.exception(e1);
                            }

                        }
                    }
                }

            } catch (Throwable e) {
                Log.exception(e);
            }

        }

    }

    @SuppressWarnings("unchecked")
    private void loadUnpacked() {
        URL ret = getClass().getResource("/");
        File root;
        if (ret.getProtocol().equalsIgnoreCase("file")) {
            try {
                root = new File(ret.toURI());
            } catch (URISyntaxException e) {
                Log.exception(e);
                Log.L.finer("Did not load unpacked Extensions from " + ret);
                return;
            }
        } else {
            Log.L.finer("Did not load unpacked Extensions from " + ret);
            return;
        }
        root = new File(root, AbstractExtension.class.getPackage().getName().replace('.', '/'));
        Log.L.finer("Load Extensions from: " + root.getAbsolutePath());
        File[] folders = root.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        ClassLoader cl = getClass().getClassLoader();
        main: for (File f : folders) {
            File[] modules = f.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.endsWith("Extension.class");
                }
            });
            boolean loaded = false;
            for (File module : modules) {

                Class<?> cls;
                try {
                    cls = cl.loadClass(AbstractExtension.class.getPackage().getName() + "." + module.getParentFile().getName() + "." + module.getName().substring(0, module.getName().length() - 6));

                    if (AbstractExtension.class.isAssignableFrom(cls)) {
                        loaded = true;
                        initModule((Class<AbstractExtension<?>>) cls);
                        continue main;
                    }
                } catch (IllegalArgumentException e) {
                    Log.L.warning("Did not init Extension " + module + " : " + e.getMessage());
                } catch (Throwable e) {
                    Log.exception(e);
                    Dialog.getInstance().showExceptionDialog("Error", e.getMessage(), e);
                }
            }
            if (!loaded) {
                Log.L.warning("Could not load any Extension Module from " + f);
            }
        }

    }

    private void initModule(Class<AbstractExtension<?>> cls) throws InstantiationException, IllegalAccessException, StartException, IOException, ClassNotFoundException {

        LazyExtension cached = getCache(cls);

        if (cached._isEnabled()) {
            cached.init();
        }
        map.put(cls, cached);

        list.add(cached);

    }

    private LazyExtension getCache(Class<AbstractExtension<?>> cls) throws StartException, InstantiationException, IllegalAccessException, IOException {
        String id = cls.getName().substring(27);
        // File cache = Application.getResource("tmp/extensioncache/" + id +
        // ".json");
        // AbstractExtensionWrapper cached = JSonStorage.restoreFrom(cache,
        // true, (byte[]) null, new TypeRef<AbstractExtensionWrapper>() {

        LazyExtension cached = cache.get(id);

        int v = AbstractExtension.readVersion(cls);
        if (cached != null) {
            cached._setPluginClass(cls);
            if (cached.getVersion() != v || !cached.getLng().equals(_JDT.getLanguage()) || cached._getSettings() == null) {
                // update cache
                cached = null;
            }
        }
        if (cached == null) {
            Log.L.info("Update Cache " + cache);
            cached = LazyExtension.create(id, cls);
            cache.put(id, cached);
            cacheChanged = true;
        } else {
            cached._setPluginClass(cls);
        }
        return cached;
    }

    public boolean isExtensionActive(Class<? extends AbstractExtension<?>> class1) {
        LazyExtension plg = map.get(class1);
        if (plg != null && plg._getExtension() != null && plg._getExtension().isEnabled()) return true;
        return false;
    }

    public List<LazyExtension> getExtensions() {
        return list;
    }

    /**
     * Returns a list of all currently running extensions
     * 
     * @return
     */
    public ArrayList<AbstractExtension<?>> getEnabledExtensions() {
        ArrayList<AbstractExtension<?>> ret = new ArrayList<AbstractExtension<?>>();
        List<LazyExtension> llist = list;
        for (LazyExtension aew : llist) {
            if (aew._getExtension() != null && aew._getExtension().isEnabled()) ret.add(aew._getExtension());
        }
        return ret;
    }

    public <T extends AbstractExtension<?>> LazyExtension getExtension(Class<T> class1) {
        LazyExtension ret = map.get(class1);
        if (ret == null) {
            for (LazyExtension l : list) {
                if (class1.getName().equals(l.getClassname())) {
                    map.put(l._getPluginClass(), l);
                    ret = l;
                    break;
                }
            }
        }
        return ret;
    }

}
